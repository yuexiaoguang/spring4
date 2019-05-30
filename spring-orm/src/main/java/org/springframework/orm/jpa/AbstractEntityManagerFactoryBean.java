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
	 * 指定此EntityManagerFactory代理应实现的 (可能特定于供应商的) EntityManagerFactory接口.
	 * <p>默认值取自特定的JpaVendorAdapter, 或者设置为标准{@code javax.persistence.EntityManagerFactory}接口.
	 */
	public void setEntityManagerFactoryInterface(Class<? extends EntityManagerFactory> emfInterface) {
		this.entityManagerFactoryInterface = emfInterface;
	}

	/**
	 * 指定此工厂的EntityManager应实现的(可能特定于供应商的) EntityManager接口.
	 * <p>默认值取自特定的JpaVendorAdapter, 或者设置为标准{@code javax.persistence.EntityManager}接口.
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> emInterface) {
		this.entityManagerInterface = emInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

	/**
	 * 指定特定于供应商的JpaDialect实现以与此EntityManagerFactory关联.
	 * 这将通过EntityManagerFactoryInfo接口公开, 由打算使用JpaDialect功能的访问者选择为默认方言.
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = jpaDialect;
	}

	@Override
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * 指定所需的JPA提供者的JpaVendorAdapter实现.
	 * 这将初始化给定提供者的适当默认值, 例如持久化提供者类和JpaDialect, 除非在此FactoryBean中进行本地重写.
	 */
	public void setJpaVendorAdapter(JpaVendorAdapter jpaVendorAdapter) {
		this.jpaVendorAdapter = jpaVendorAdapter;
	}

	/**
	 * 返回此EntityManagerFactory的JpaVendorAdapter实现, 或{@code null}.
	 */
	public JpaVendorAdapter getJpaVendorAdapter() {
		return this.jpaVendorAdapter;
	}

	/**
	 * 指定后台引导的异步执行器, e.g. {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * <p>{@code EntityManagerFactory}初始化将切换到后台引导模式, 并立即返回{@code EntityManagerFactory}代理以进行注入,
	 * 而不是等待JPA提供者的引导完成.
	 * 但请注意, 对{@code EntityManagerFactory}方法的第一次实际调用将阻塞,
	 * 直到JPA提供者的引导完成, 如果其尚未准备就绪.
	 * 为了获得最大收益, 请确保避免在相关bean的init方法中进行实时{@code EntityManagerFactory}调用, 即使是为了元数据内省目的.
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * 返回用于后台引导的异步执行器.
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

		// 将EntityManagerFactory包装在实现其所有接口的工厂中.
		// 这允许拦截createEntityManager方法以返回应用程序管理的自动加入现有事务的EntityManager代理.
		this.entityManagerFactory = createEntityManagerFactoryProxy(this.nativeEntityManagerFactory);
	}

	private EntityManagerFactory buildNativeEntityManagerFactory() {
		EntityManagerFactory emf;
		try {
			emf = createNativeEntityManagerFactory();
		}
		catch (PersistenceException ex) {
			if (ex.getClass() == PersistenceException.class) {
				// 底层异常的普通 PersistenceException包装器?
				// 确保嵌套异常消息正确公开, 沿着Spring的 NestedRuntimeException.getMessage()行
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
	 * 为给定的{@link EntityManagerFactory}创建代理.
	 * 为了能够为应用程序管理的{@link EntityManager}返回事务感知代理.
	 * 
	 * @param emf 持久化提供者返回的EntityManagerFactory, 如果已初始化的话
	 * 
	 * @return EntityManagerFactory代理
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
	 * 委托来自代理的传入调用, 相应地调度到EntityManagerFactoryInfo或本机EntityManagerFactory.
	 */
	Object invokeProxyMethod(Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(EntityManagerFactoryInfo.class)) {
			return method.invoke(this, args);
		}
		else if (method.getName().equals("createEntityManager") && args != null && args.length > 0 &&
				args[0] != null && args[0].getClass().isEnum() && "SYNCHRONIZED".equals(args[0].toString())) {
			// JPA 2.1的 createEntityManager(SynchronizationType, Map)
			// 重定向到普通的createEntityManager, 并通过Spring代理添加同步语义
			EntityManager rawEntityManager = (args.length > 1 ?
					getNativeEntityManagerFactory().createEntityManager((Map<?, ?>) args[1]) :
					getNativeEntityManagerFactory().createEntityManager());
			return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
		}

		// 查找Query参数, 主要是JPA 2.1的addNamedQuery(String, Query)
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof Query && Proxy.isProxyClass(arg.getClass())) {
					// 可能是来自SharedEntityManagerCreator的Spring生成的代理:
					// 因为将它传递回本机EntityManagerFactory, 将它从提供者解包到原始的Query对象.
					try {
						args[i] = ((Query) arg).unwrap(null);
					}
					catch (RuntimeException ex) {
						// Ignore - 然后简单地继续使用给定的Query对象
					}
				}
			}
		}

		// 标准委托到本机工厂, 只是后处理EntityManager返回值
		Object retVal = method.invoke(getNativeEntityManagerFactory(), args);
		if (retVal instanceof EntityManager) {
			// 任何其他createEntityManager变体 - 期望非同步语义
			EntityManager rawEntityManager = (EntityManager) retVal;
			retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
		}
		return retVal;
	}

	/**
	 * 子类必须实现此方法来创建将由{@code getObject()}方法返回的EntityManagerFactory.
	 * 
	 * @return 此FactoryBean返回的EntityManagerFactory实例
	 * @throws PersistenceException 如果无法创建EntityManager
	 */
	protected abstract EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException;


	/**
	 * PersistenceExceptionTranslator接口的实现, 由Spring的PersistenceExceptionTranslationPostProcessor自动检测.
	 * <p>如果可能, 使用方言的转换; 否则回退到标准的JPA异常转换.
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
					// 直接重写提供者配置异常 (可能具有嵌套原因)
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
	 * 返回单个EntityManagerFactory.
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
	 * 在bean工厂关闭时关闭EntityManagerFactory.
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
	 * 对周围的AbstractEntityManagerFactoryBean的最小bean引用.
	 * 解析反序列化时的实际AbstractEntityManagerFactoryBean实例.
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
	 * {@link EntityManagerFactory}的动态代理调用处理器, 从{@code createEntityManager}方法返回代理{@link EntityManager}.
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
					// 只有当代理相同时才考虑相等.
					return (proxy == args[0]);
				}
				else if (method.getName().equals("hashCode")) {
					// 使用EntityManagerFactory代理的hashCode.
					return System.identityHashCode(proxy);
				}
				else if (method.getName().equals("unwrap")) {
					// 处理JPA 2.1 unwrap方法 - 可以是代理匹配.
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
