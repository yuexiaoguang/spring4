package org.springframework.aop.framework;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

/**
 * 生成单例范围代理对象的{@link FactoryBean}类型的超类.
 *
 * <p>管理拦截器前后 (引用, 而不是拦截器名称, 在 {@link ProxyFactoryBean}中), 并提供一致的接口管理.
 */
@SuppressWarnings("serial")
public abstract class AbstractSingletonProxyFactoryBean extends ProxyConfig
		implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

	private Object target;

	private Class<?>[] proxyInterfaces;

	private Object[] preInterceptors;

	private Object[] postInterceptors;

	/** 默认是全局的AdvisorAdapterRegistry */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private transient ClassLoader proxyClassLoader;

	private Object proxy;


	/**
	 * 设置目标对象, 也就是说, 要用事务代理包装的bean.
	 * <p>目标可以是任何对象, 在这种情况下, 将创建SingletonTargetSource. 如果是TargetSource, 不会创建包装器TargetSource:
	 * 这使得可以使用池或原型TargetSource等.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * 指定要代理的接口集.
	 * <p>如果没有指定 (默认), AOP通过分析目标来确定哪些接口需要代理, 代理目标对象实现的所有接口.
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
		this.proxyInterfaces = proxyInterfaces;
	}

	/**
	 * 在隐式事务拦截器之前设置要应用的其他拦截器（或切面）, e.g. 一个PerformanceMonitorInterceptor.
	 * <p>可以指定任何AOP Alliance MethodInterceptors或其他Spring AOP增强, 以及Spring AOP Advisor.
	 */
	public void setPreInterceptors(Object[] preInterceptors) {
		this.preInterceptors = preInterceptors;
	}

	/**
	 * 设置在隐式事务拦截器之后应用的其他拦截器（或切面）.
	 * <p>可以指定任何AOP Alliance MethodInterceptors或其他Spring AOP增强, 以及Spring AOP Advisor.
	 */
	public void setPostInterceptors(Object[] postInterceptors) {
		this.postInterceptors = postInterceptors;
	}

	/**
	 * 指定要使用的AdvisorAdapterRegistry.
	 * 默认是全局AdvisorAdapterRegistry.
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置ClassLoader以生成代理类.
	 * <p>默认是bean ClassLoader, i.e. 包含BeanFactory用于加载所有bean类的ClassLoader.
	 * 对于特定代理, 可以在此处重写此内容.
	 */
	public void setProxyClassLoader(ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (this.proxyClassLoader == null) {
			this.proxyClassLoader = classLoader;
		}
	}


	@Override
	public void afterPropertiesSet() {
		if (this.target == null) {
			throw new IllegalArgumentException("Property 'target' is required");
		}
		if (this.target instanceof String) {
			throw new IllegalArgumentException("'target' needs to be a bean reference, not a bean name as value");
		}
		if (this.proxyClassLoader == null) {
			this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
		}

		ProxyFactory proxyFactory = new ProxyFactory();

		if (this.preInterceptors != null) {
			for (Object interceptor : this.preInterceptors) {
				proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
			}
		}

		// 添加主拦截器 (通常是 Advisor).
		proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(createMainInterceptor()));

		if (this.postInterceptors != null) {
			for (Object interceptor : this.postInterceptors) {
				proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
			}
		}

		proxyFactory.copyFrom(this);

		TargetSource targetSource = createTargetSource(this.target);
		proxyFactory.setTargetSource(targetSource);

		if (this.proxyInterfaces != null) {
			proxyFactory.setInterfaces(this.proxyInterfaces);
		}
		else if (!isProxyTargetClass()) {
			// 依靠AOP来告诉我们要代理的接口.
			proxyFactory.setInterfaces(
					ClassUtils.getAllInterfacesForClass(targetSource.getTargetClass(), this.proxyClassLoader));
		}

		postProcessProxyFactory(proxyFactory);

		this.proxy = proxyFactory.getProxy(this.proxyClassLoader);
	}

	/**
	 * 确定指定目标的TargetSource (或 TargetSource).
	 * 
	 * @param target 目标. 如果这是TargetSource的实现，则将其用作TargetSource; 否则它被包装在SingletonTargetSource中.
	 * 
	 * @return a TargetSource for this object
	 */
	protected TargetSource createTargetSource(Object target) {
		if (target instanceof TargetSource) {
			return (TargetSource) target;
		}
		else {
			return new SingletonTargetSource(target);
		}
	}

	/**
	 * 子类的钩子, 用于在使用它创建代理实例之前, 对{@link ProxyFactory}进行后处理.
	 * 
	 * @param proxyFactory 即将使用的AOP ProxyFactory
	 * 
	 * @since 4.2
	 */
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
	}


	@Override
	public Object getObject() {
		if (this.proxy == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		if (this.proxy != null) {
			return this.proxy.getClass();
		}
		if (this.proxyInterfaces != null && this.proxyInterfaces.length == 1) {
			return this.proxyInterfaces[0];
		}
		if (this.target instanceof TargetSource) {
			return ((TargetSource) this.target).getTargetClass();
		}
		if (this.target != null) {
			return this.target.getClass();
		}
		return null;
	}

	@Override
	public final boolean isSingleton() {
		return true;
	}


	/**
	 * 为此代理工厂bean创建“main”拦截器.
	 * 通常是 Advisor, 但也可以是任何类型的 Advice.
	 * <p>预拦截器将在此之前应用，拦截器将在此拦截器之后应用.
	 */
	protected abstract Object createMainInterceptor();

}
