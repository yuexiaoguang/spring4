package org.springframework.jndi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.ClassUtils;

/**
 * 查找JNDI对象的{@link org.springframework.beans.factory.FactoryBean}.
 * 公开JNDI中找到的对象以获取bean引用, e.g. {@link javax.sql.DataSource}的数据访问对象的"dataSource"属性.
 *
 * <p>典型的用法是在应用程序上下文中将其注册为单例工厂 (e.g. 对于某个JNDI绑定的DataSource),
 * 并为需要它的应用程序服务提供bean引用.
 *
 * <p>默认行为是在启动时查找JNDI对象并对其进行缓存.
 * 这可以通过"lookupOnStartup" 和 "cache"属性进行自定义, 使用下面的{@link JndiObjectTargetSource}.
 * 请注意, 您需要在这种情况下指定"proxyInterface", 因为事先不知道实际的JNDI对象类型.
 *
 * <p>当然, Spring环境中的bean类可能会查找 e.g. 来自JNDI的DataSource.
 * 该类只需启用JNDI名称的中央配置, 并轻松切换到非JNDI备选方案.
 * 后者对于测试设置, 在独立客户端中重用等特别方便.
 *
 * <p>请注意, 切换到 e.g. DriverManagerDataSource 只是配置问题:
 * 只需使用{@link org.springframework.jdbc.datasource.DriverManagerDataSource}定义替换此FactoryBean的定义!
 */
public class JndiObjectFactoryBean extends JndiObjectLocator
		implements FactoryBean<Object>, BeanFactoryAware, BeanClassLoaderAware {

	private Class<?>[] proxyInterfaces;

	private boolean lookupOnStartup = true;

	private boolean cache = true;

	private boolean exposeAccessContext = false;

	private Object defaultObject;

	private ConfigurableBeanFactory beanFactory;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Object jndiObject;


	/**
	 * 指定用于JNDI对象的代理接口.
	 * <p>通常与"lookupOnStartup"=false 和/或 "cache"=false一起使用.
	 * 需要指定, 因为在延迟查找的情况下, 事先不知道实际的JNDI对象类型.
	 */
	public void setProxyInterface(Class<?> proxyInterface) {
		this.proxyInterfaces = new Class<?>[] {proxyInterface};
	}

	/**
	 * 指定用于JNDI对象的多个代理接口.
	 * <p>通常与"lookupOnStartup"=false 和/或 "cache"=false一起使用.
	 * 请注意, 如有必要, 将从指定的"expectedType", 自动检测代理接口.
	 */
	public void setProxyInterfaces(Class<?>... proxyInterfaces) {
		this.proxyInterfaces = proxyInterfaces;
	}

	/**
	 * 设置是否在启动时查找JNDI对象. 默认"true".
	 * <p>可以关闭以允许JNDI对象的延迟可用性.
	 * 在这种情况下, 将在首次访问时获取JNDI对象.
	 * <p>对于延迟查找, 需要指定代理接口.
	 */
	public void setLookupOnStartup(boolean lookupOnStartup) {
		this.lookupOnStartup = lookupOnStartup;
	}

	/**
	 * 设置是否在找到JNDI对象后对其进行缓存.
	 * 默认"true".
	 * <p>可以关闭以允许热重新部署JNDI对象.
	 * 在这种情况下, 将为每次调用获取JNDI对象.
	 * <p>对于热重新部署, 需要指定代理接口.
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * 设置是否为所有对目标对象的访问公开JNDI环境上下文, i.e. 对于公开的对象引用上的所有方法调用.
	 * <p>默认"false", i.e. 仅公开用于对象查找的JNDI上下文.
	 * 将此标志切换为"true", 为了公开每个方法调用的JNDI环境 (包括授权上下文),
	 * 根据WebLogic对JNDI获得的具有授权要求的工厂 (e.g. JDBC DataSource, JMS ConnectionFactory)的需要.
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}

	/**
	 * 如果JNDI查找失败, 请指定要回退到的默认对象.
	 * 默认无.
	 * <p>这可以是任意bean引用或文字值.
	 * 它通常用于, JNDI环境可能定义特定配置, 但这些设置不需要存在, 的场景中的文字值.
	 * <p>Note: 仅支持在启动时查找.
	 * 如果与{@link #setExpectedType}一起指定, 则指定的值必须是该类型的任何一种, 或者可以转换为该类型.
	 */
	public void setDefaultObject(Object defaultObject) {
		this.defaultObject = defaultObject;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			// 只是可选  - 如果需要, 可以获得专门配置的TypeConverter.
			// 如果没有特定的, 我们将简单地回到SimpleTypeConverter.
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 查找JNDI对象并存储它.
	 */
	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		super.afterPropertiesSet();

		if (this.proxyInterfaces != null || !this.lookupOnStartup || !this.cache || this.exposeAccessContext) {
			// We need to create a proxy for this...
			if (this.defaultObject != null) {
				throw new IllegalArgumentException(
						"'defaultObject' is not supported in combination with 'proxyInterface'");
			}
			// We need a proxy and a JndiObjectTargetSource.
			this.jndiObject = JndiObjectProxyFactory.createJndiObjectProxy(this);
		}
		else {
			if (this.defaultObject != null && getExpectedType() != null &&
					!getExpectedType().isInstance(this.defaultObject)) {
				TypeConverter converter = (this.beanFactory != null ?
						this.beanFactory.getTypeConverter() : new SimpleTypeConverter());
				try {
					this.defaultObject = converter.convertIfNecessary(this.defaultObject, getExpectedType());
				}
				catch (TypeMismatchException ex) {
					throw new IllegalArgumentException("Default object [" + this.defaultObject + "] of type [" +
							this.defaultObject.getClass().getName() + "] is not of expected type [" +
							getExpectedType().getName() + "] and cannot be converted either", ex);
				}
			}
			// Locate specified JNDI object.
			this.jndiObject = lookupWithFallback();
		}
	}

	/**
	 * 在查找失败的情况下, 返回指定的"defaultObject".
	 * 
	 * @return 找到的对象, 或"defaultObject"作为回退
	 * @throws NamingException 如果查找失败而没有回退
	 */
	protected Object lookupWithFallback() throws NamingException {
		ClassLoader originalClassLoader = ClassUtils.overrideThreadContextClassLoader(this.beanClassLoader);
		try {
			return lookup();
		}
		catch (TypeMismatchNamingException ex) {
			// 始终让TypeMismatchNamingException通过 - 不希望在这种情况下回退到defaultObject.
			throw ex;
		}
		catch (NamingException ex) {
			if (this.defaultObject != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("JNDI lookup failed - returning specified default object instead", ex);
				}
				else if (logger.isInfoEnabled()) {
					logger.info("JNDI lookup failed - returning specified default object instead: " + ex);
				}
				return this.defaultObject;
			}
			throw ex;
		}
		finally {
			if (originalClassLoader != null) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
	}


	/**
	 * 返回单例JNDI对象.
	 */
	@Override
	public Object getObject() {
		return this.jndiObject;
	}

	@Override
	public Class<?> getObjectType() {
		if (this.proxyInterfaces != null) {
			if (this.proxyInterfaces.length == 1) {
				return this.proxyInterfaces[0];
			}
			else if (this.proxyInterfaces.length > 1) {
				return createCompositeInterface(this.proxyInterfaces);
			}
		}
		if (this.jndiObject != null) {
			return this.jndiObject.getClass();
		}
		else {
			return getExpectedType();
		}
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 为给定接口创建复合接口Class, 在单个Class中实现给定接口.
	 * <p>默认实现为给定接口构建JDK代理类.
	 * 
	 * @param interfaces 要合并的接口
	 * 
	 * @return 合并后的接口
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
	}


	/**
	 * 内部类, 只是在实际创建代理时引入AOP依赖项.
	 */
	private static class JndiObjectProxyFactory {

		private static Object createJndiObjectProxy(JndiObjectFactoryBean jof) throws NamingException {
			// 创建一个镜像JndiObjectFactoryBean配置的JndiObjectTargetSource.
			JndiObjectTargetSource targetSource = new JndiObjectTargetSource();
			targetSource.setJndiTemplate(jof.getJndiTemplate());
			targetSource.setJndiName(jof.getJndiName());
			targetSource.setExpectedType(jof.getExpectedType());
			targetSource.setResourceRef(jof.isResourceRef());
			targetSource.setLookupOnStartup(jof.lookupOnStartup);
			targetSource.setCache(jof.cache);
			targetSource.afterPropertiesSet();

			// 使用JndiObjectFactoryBean的代理接口和JndiObjectTargetSource创建代理.
			ProxyFactory proxyFactory = new ProxyFactory();
			if (jof.proxyInterfaces != null) {
				proxyFactory.setInterfaces(jof.proxyInterfaces);
			}
			else {
				Class<?> targetClass = targetSource.getTargetClass();
				if (targetClass == null) {
					throw new IllegalStateException(
							"Cannot deactivate 'lookupOnStartup' without specifying a 'proxyInterface' or 'expectedType'");
				}
				Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(targetClass, jof.beanClassLoader);
				for (Class<?> ifc : ifcs) {
					if (Modifier.isPublic(ifc.getModifiers())) {
						proxyFactory.addInterface(ifc);
					}
				}
			}
			if (jof.exposeAccessContext) {
				proxyFactory.addAdvice(new JndiContextExposingInterceptor(jof.getJndiTemplate()));
			}
			proxyFactory.setTargetSource(targetSource);
			return proxyFactory.getProxy(jof.beanClassLoader);
		}
	}


	/**
	 * 根据JndiObjectFactoryBean的"exposeAccessContext"标志, 为所有方法调用公开JNDI上下文的拦截器.
	 */
	private static class JndiContextExposingInterceptor implements MethodInterceptor {

		private final JndiTemplate jndiTemplate;

		public JndiContextExposingInterceptor(JndiTemplate jndiTemplate) {
			this.jndiTemplate = jndiTemplate;
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Context ctx = (isEligible(invocation.getMethod()) ? this.jndiTemplate.getContext() : null);
			try {
				return invocation.proceed();
			}
			finally {
				this.jndiTemplate.releaseContext(ctx);
			}
		}

		protected boolean isEligible(Method method) {
			return (Object.class != method.getDeclaringClass());
		}
	}

}
