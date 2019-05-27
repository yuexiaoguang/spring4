package org.springframework.orm.jpa;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 抽象{@link org.springframework.beans.factory.FactoryBean},
 * 在Spring应用程序上下文中创建本地JPA {@link javax.persistence.EntityManagerFactory}实例.
 *
 * <p>封装不同JPA引导程序约定 (独立和容器)之间的通用功能.
 *
 * <p>实现支持标准JPA配置约定, 以及Spring的可定制{@link JpaVendorAdapter}机制, 并控制EntityManagerFactory的生命周期.
 *
 * <p>此类也实现了
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator}接口, 由Spring的
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}自动检测,
 * 用于基于AOP的Spring DataAccessException的本机异常转换.
 * 因此, LocalEntityManagerFactoryBean自动启用 PersistenceExceptionTranslationPostProcessor来转换JPA异常.
 */
@SuppressWarnings("serial")
public abstract class AbstractEntityManagerFactoryBean implements
		FactoryBean<EntityManagerFactory>, BeanClassLoaderAware, BeanFactoryAware, BeanNameAware,
		InitializingBean, DisposableBean, EntityManagerFactoryInfo, PersistenceExceptionTranslator, Serializable {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private PersistenceProvider persistenceProvider;

	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();

	private Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private Class<? extends EntityManager> entityManagerInterface;

	private JpaDialect jpaDialect;

	private JpaVendorAdapter jpaVendorAdapter;

	private AsyncTaskExecutor bootstrapExecutor;

	private ClassLoader beanClassLoader = getClass().getClassLoader();

	private BeanFactory beanFactory;

	private String beanName;

	/** PersistenceProvider返回的原始 EntityManagerFactory */
	private EntityManagerFactory nativeEntityManagerFactory;

	/** 延迟初始化原始目标EntityManagerFactory的Future */
	private Future<EntityManagerFactory> nativeEntityManagerFactoryFuture;

	/** 暴露的客户端级EntityManagerFactory代理 */
	private EntityManagerFactory entityManagerFactory;


	/**
	 * 设置用于创建EntityManagerFactory的PersistenceProvider实现类.
	 * 如果未指定, 持久化提供者将从JpaVendorAdapter获取或通过扫描检索 (尽可能).
	 */
	public void setPersistenceProviderClass(Class<? extends PersistenceProvider> persistenceProviderClass) {
		this.persistenceProvider = BeanUtils.instantiateClass(persistenceProviderClass);
	}

	/**
	 * 设置用于创建EntityManagerFactory的PersistenceProvider实例.
	 * 如果未指定, 持久化提供者将从JpaVendorAdapter获取, 或由持久化单元部署描述符确定 (尽可能).
	 */
	public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	/**
	 * 指定EntityManagerFactory配置的名称.
	 * <p>默认无, 表示默认的EntityManagerFactory配置.
	 * 如果找到不明确的EntityManager配置, 持久化提供者将抛出异​​常.
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定要传递到{@code Persistence.createEntityManagerFactory}的JPA属性.
	 * <p>可以使用String "value" (通过PropertiesEditor解析) 或XML bean定义中的"props"元素填充.
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 指定要传递到{@code Persistence.createEntityManagerFactory}的JPA属性.
	 * <p>可以在XML bean定义中使用"map" 或 "props"元素填充.
	 */
	public void setJpaPropertyMap(Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许将对JPA属性的Map访问权限传递给持久化提供者, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"jpaPropertyMap[myKey]".
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * Specify the (potentially vendor-specific) EntityManagerFactory interface that this EntityManagerFactory proxy is supposed to implement.
	 * <p>The default will be taken from the specific JpaVendorAdapter, if any, or set to the standard {@code javax.persistence.EntityManagerFactory} interface else.
	 */
	public void setEntityManagerFactoryInterface(Class<? extends EntityManagerFactory> emfInterface) {
		this.entityManagerFactoryInterface = emfInterface;
	}

	/**
	 * Specify the (potentially vendor-specific) EntityManager interface that this factory's EntityManagers are supposed to implement.
	 * <p>The default will be taken from the specific JpaVendorAdapter, if any, or set to the standard {@code javax.persistence.EntityManager} interface else.
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> emInterface) {
		this.entityManagerInterface = emInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

	/**
	 * Specify the vendor-specific JpaDialect implementation to associate with this EntityManagerFactory.
	 * This will be exposed through the EntityManagerFactoryInfo interface, to be picked up as default dialect by accessors that intend to use JpaDialect functionality.
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = jpaDialect;
	}

	@Override
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * Specify the JpaVendorAdapter implementation for the desired JPA provider, if any.
	 * This will initialize appropriate defaults for the given provider, such as persistence provider class and JpaDialect, unless locally overridden in this FactoryBean.
	 */
	public void setJpaVendorAdapter(JpaVendorAdapter jpaVendorAdapter) {
		this.jpaVendorAdapter = jpaVendorAdapter;
	}

	/**
	 * Return the JpaVendorAdapter implementation for this EntityManagerFactory, or {@code null} if not known.
	 */
	public JpaVendorAdapter getJpaVendorAdapter() {
		return this.jpaVendorAdapter;
	}

	/**
	 * Specify an asynchronous executor for background bootstrapping, e.g. a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * <p>{@code EntityManagerFactory} initialization will then switch into background bootstrap mode, with a {@code EntityManagerFactory} proxy immediately returned for injection purposes instead of waiting for the JPA provider's bootstrapping to complete.
	 * However, note that the first actual call to a {@code EntityManagerFactory} method will then block until the JPA provider's bootstrapping completed, if not ready by then.
	 * For maximum benefit, make sure to avoid early {@code EntityManagerFactory} calls in init methods of related beans, even for metadata introspection purposes.
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * Return the asynchronous executor for background bootstrapping, if any.
	 */
	public AsyncTaskExecutor getBootstrapExecutor() {
		return this.bootstrapExecutor;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws PersistenceException {
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null) {
			if (this.persistenceProvider == null) {
				this.persistenceProvider = jpaVendorAdapter.getPersistenceProvider();
			}
			PersistenceUnitInfo pui = getPersistenceUnitInfo();
			Map<String, ?> vendorPropertyMap = null;
			if (pui != null) {
				try {
					vendorPropertyMap = jpaVendorAdapter.getJpaPropertyMap(pui);
				}
				catch (AbstractMethodError err) {
					// Spring 4.3.13 getJpaPropertyMap(PersistenceUnitInfo) not implemented
				}
			}
			if (vendorPropertyMap == null) {
				vendorPropertyMap = jpaVendorAdapter.getJpaPropertyMap();
			}
			if (!CollectionUtils.isEmpty(vendorPropertyMap)) {
				for (Map.Entry<String, ?> entry : vendorPropertyMap.entrySet()) {
					if (!this.jpaPropertyMap.containsKey(entry.getKey())) {
						this.jpaPropertyMap.put(entry.getKey(), entry.getValue());
					}
				}
			}
			if (this.entityManagerFactoryInterface == null) {
				this.entityManagerFactoryInterface = jpaVendorAdapter.getEntityManagerFactoryInterface();
				if (!ClassUtils.isVisible(this.entityManagerFactoryInterface, this.beanClassLoader)) {
					this.entityManagerFactoryInterface = EntityManagerFactory.class;
				}
			}
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = jpaVendorAdapter.getEntityManagerInterface();
				if (!ClassUtils.isVisible(this.entityManagerInterface, this.beanClassLoader)) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
			if (this.jpaDialect == null) {
				this.jpaDialect = jpaVendorAdapter.getJpaDialect();
			}
		}

		if (this.bootstrapExecutor != null) {
			this.nativeEntityManagerFactoryFuture = this.bootstrapExecutor.submit(new Callable<EntityManagerFactory>() {
				@Override
				public EntityManagerFactory call() {
					return buildNativeEntityManagerFactory();
				}
			});
		}
		else {
			this.nativeEntityManagerFactory = buildNativeEntityManagerFactory();
		}

		// Wrap the EntityManagerFactory in a factory implementing all its interfaces.
		// This allows interception of createEntityManager methods to return an application-managed EntityManager proxy that automatically joins existing transactions.
		this.entityManagerFactory = createEntityManagerFactoryProxy(this.nativeEntityManagerFactory);
	}

	private EntityManagerFactory buildNativeEntityManagerFactory() {
		EntityManagerFactory emf;
		try {
			emf = createNativeEntityManagerFactory();
		}
		catch (PersistenceException ex) {
			if (ex.getClass() == PersistenceException.class) {
				// Plain PersistenceException wrapper for underlying exception?
				// Make sure the nested exception message is properly exposed, along the lines of Spring's NestedRuntimeException.getMessage()
				Throwable cause = ex.getCause();
				if (cause != null) {
					String message = ex.getMessage();
					String causeString = cause.toString();
					if (!message.endsWith(causeString)) {
						throw new PersistenceException(message + "; nested exception is " + causeString, cause);
					}
				}
			}
			throw ex;
		}

		if (emf == null) {
			throw new IllegalStateException(
					"JPA PersistenceProvider returned null EntityManagerFactory - check your JPA provider setup!");
		}

		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null) {
			jpaVendorAdapter.postProcessEntityManagerFactory(emf);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Initialized JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		return emf;
	}

	/**
	 * Create a proxy for the given {@link EntityManagerFactory}.
	 * We do this to be able to return a transaction-aware proxy for an application-managed {@link EntityManager}.
	 * 
	 * @param emf the EntityManagerFactory as returned by the persistence provider, if initialized already
	 * 
	 * @return the EntityManagerFactory proxy
	 */
	protected EntityManagerFactory createEntityManagerFactoryProxy(EntityManagerFactory emf) {
		Set<Class<?>> ifcs = new LinkedHashSet<Class<?>>();
		Class<?> entityManagerFactoryInterface = this.entityManagerFactoryInterface;
		if (entityManagerFactoryInterface != null) {
			ifcs.add(entityManagerFactoryInterface);
		}
		else if (emf != null) {
			ifcs.addAll(ClassUtils.getAllInterfacesForClassAsSet(emf.getClass(), this.beanClassLoader));
		}
		else {
			ifcs.add(EntityManagerFactory.class);
		}
		ifcs.add(EntityManagerFactoryInfo.class);

		try {
			return (EntityManagerFactory) Proxy.newProxyInstance(this.beanClassLoader,
					ClassUtils.toClassArray(ifcs), new ManagedEntityManagerFactoryInvocationHandler(this));
		}
		catch (IllegalArgumentException ex) {
			if (entityManagerFactoryInterface != null) {
				throw new IllegalStateException("EntityManagerFactory interface [" + entityManagerFactoryInterface +
						"] seems to conflict with Spring's EntityManagerFactoryInfo mixin - consider resetting the "+
						"'entityManagerFactoryInterface' property to plain [javax.persistence.EntityManagerFactory]", ex);
			}
			else {
				throw new IllegalStateException("Conflicting EntityManagerFactory interfaces - " +
						"consider specifying the 'jpaVendorAdapter' or 'entityManagerFactoryInterface' property " +
						"to select a specific EntityManagerFactory interface to proceed with", ex);
			}
		}
	}

	/**
	 * Delegate an incoming invocation from the proxy, dispatching to EntityManagerFactoryInfo or the native EntityManagerFactory accordingly.
	 */
	Object invokeProxyMethod(Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(EntityManagerFactoryInfo.class)) {
			return method.invoke(this, args);
		}
		else if (method.getName().equals("createEntityManager") && args != null && args.length > 0 &&
				args[0] != null && args[0].getClass().isEnum() && "SYNCHRONIZED".equals(args[0].toString())) {
			// JPA 2.1's createEntityManager(SynchronizationType, Map)
			// Redirect to plain createEntityManager and add synchronization semantics through Spring proxy
			EntityManager rawEntityManager = (args.length > 1 ?
					getNativeEntityManagerFactory().createEntityManager((Map<?, ?>) args[1]) :
					getNativeEntityManagerFactory().createEntityManager());
			return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
		}

		// Look for Query arguments, primarily JPA 2.1's addNamedQuery(String, Query)
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof Query && Proxy.isProxyClass(arg.getClass())) {
					// Assumably a Spring-generated proxy from SharedEntityManagerCreator:
					// since we're passing it back to the native EntityManagerFactory, let's unwrap it to the original Query object from the provider.
					try {
						args[i] = ((Query) arg).unwrap(null);
					}
					catch (RuntimeException ex) {
						// Ignore - simply proceed with given Query object then
					}
				}
			}
		}

		// Standard delegation to the native factory, just post-processing EntityManager return values
		Object retVal = method.invoke(getNativeEntityManagerFactory(), args);
		if (retVal instanceof EntityManager) {
			// Any other createEntityManager variant - expecting non-synchronized semantics
			EntityManager rawEntityManager = (EntityManager) retVal;
			retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
		}
		return retVal;
	}

	/**
	 * Subclasses must implement this method to create the EntityManagerFactory that will be returned by the {@code getObject()} method.
	 * 
	 * @return EntityManagerFactory instance returned by this FactoryBean
	 * @throws PersistenceException if the EntityManager cannot be created
	 */
	protected abstract EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException;


	/**
	 * Implementation of the PersistenceExceptionTranslator interface, as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Uses the dialect's conversion if possible; otherwise falls back to standard JPA exception conversion.
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		JpaDialect jpaDialect = getJpaDialect();
		return (jpaDialect != null ? jpaDialect.translateExceptionIfPossible(ex) :
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex));
	}

	@Override
	public EntityManagerFactory getNativeEntityManagerFactory() {
		if (this.nativeEntityManagerFactory != null) {
			return this.nativeEntityManagerFactory;
		}
		else {
			try {
				return this.nativeEntityManagerFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted during initialization of native EntityManagerFactory", ex);
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof PersistenceException) {
					// Rethrow a provider configuration exception (possibly with a nested cause) directly
					throw (PersistenceException) cause;
				}
				throw new IllegalStateException("Failed to asynchronously initialize native EntityManagerFactory: " +
						ex.getMessage(), cause);
			}
		}
	}

	@Override
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return null;
	}

	@Override
	public DataSource getDataSource() {
		return null;
	}


	/**
	 * Return the singleton EntityManagerFactory.
	 */
	@Override
	public EntityManagerFactory getObject() {
		return this.entityManagerFactory;
	}

	@Override
	public Class<? extends EntityManagerFactory> getObjectType() {
		return (this.entityManagerFactory != null ? this.entityManagerFactory.getClass() : EntityManagerFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Close the EntityManagerFactory on bean factory shutdown.
	 */
	@Override
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Closing JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		this.entityManagerFactory.close();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("An EntityManagerFactoryBean itself is not deserializable - " +
				"just a SerializedEntityManagerFactoryBeanReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.beanFactory != null && this.beanName != null) {
			return new SerializedEntityManagerFactoryBeanReference(this.beanFactory, this.beanName);
		}
		else {
			throw new NotSerializableException("EntityManagerFactoryBean does not run within a BeanFactory");
		}
	}


	/**
	 * Minimal bean reference to the surrounding AbstractEntityManagerFactoryBean.
	 * Resolved to the actual AbstractEntityManagerFactoryBean instance on deserialization.
	 */
	@SuppressWarnings("serial")
	private static class SerializedEntityManagerFactoryBeanReference implements Serializable {

		private final BeanFactory beanFactory;

		private final String lookupName;

		public SerializedEntityManagerFactoryBeanReference(BeanFactory beanFactory, String beanName) {
			this.beanFactory = beanFactory;
			this.lookupName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		}

		private Object readResolve() {
			return this.beanFactory.getBean(this.lookupName, AbstractEntityManagerFactoryBean.class);
		}
	}


	/**
	 * Dynamic proxy invocation handler for an {@link EntityManagerFactory}, returning a  proxy {@link EntityManager} (if necessary) from {@code createEntityManager} methods.
	 */
	@SuppressWarnings("serial")
	private static class ManagedEntityManagerFactoryInvocationHandler implements InvocationHandler, Serializable {

		private final AbstractEntityManagerFactoryBean entityManagerFactoryBean;

		public ManagedEntityManagerFactoryInvocationHandler(AbstractEntityManagerFactoryBean emfb) {
			this.entityManagerFactoryBean = emfb;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				else if (method.getName().equals("hashCode")) {
					// Use hashCode of EntityManagerFactory proxy.
					return System.identityHashCode(proxy);
				}
				else if (method.getName().equals("unwrap")) {
					// Handle JPA 2.1 unwrap method - could be a proxy match.
					Class<?> targetClass = (Class<?>) args[0];
					if (targetClass == null) {
						return this.entityManagerFactoryBean.getNativeEntityManagerFactory();
					}
					else if (targetClass.isInstance(proxy)) {
						return proxy;
					}
				}
				return this.entityManagerFactoryBean.invokeProxyMethod(method, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
