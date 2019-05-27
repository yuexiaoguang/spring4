package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;

import org.springframework.aop.TargetSource;
import org.springframework.util.ClassUtils;

/**
 * 用于程序化使用的AOP代理工厂, 而不是通过bean工厂中的声明性设置.
 * 此类提供了一种在自定义用户代码中获取和配置AOP代理实例的简单方法.
 */
@SuppressWarnings("serial")
public class ProxyFactory extends ProxyCreatorSupport {

	public ProxyFactory() {
	}

	/**
	 * <p>将代理给定目标实现的所有接口.
	 * 
	 * @param target 要代理的目标对象
	 */
	public ProxyFactory(Object target) {
		setTarget(target);
		setInterfaces(ClassUtils.getAllInterfaces(target));
	}

	/**
	 * <p>没有目标, 只有接口. 必须添加拦截器.
	 * 
	 * @param proxyInterfaces 代理应该实现的接口
	 */
	public ProxyFactory(Class<?>... proxyInterfaces) {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * <p>为单个拦截器创建代理的便捷方法, 假设拦截器处理所有调用本身, 而不是委托给目标, 就像远程代理的情况一样.
	 * 
	 * @param proxyInterface 代理应该实现的接口
	 * @param interceptor 代理应该调用的拦截器
	 */
	public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
		addInterface(proxyInterface);
		addAdvice(interceptor);
	}

	/**
	 * 为指定的{@code TargetSource}创建ProxyFactory, 使代理实现指定的接口.
	 * 
	 * @param proxyInterface 代理应该实现的接口
	 * @param targetSource 代理应该调用的TargetSource
	 */
	public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
		addInterface(proxyInterface);
		setTargetSource(targetSource);
	}


	/**
	 * 根据此工厂中的设置创建新代理.
	 * <p>可以反复调用. 如果我们添加或删除了接口，效果会有所不同. 可以添加和删除拦截器.
	 * <p>使用默认的类加载器: 通常, 线程上下文类加载器 (如果需要代理创建).
	 * 
	 * @return 代理对象
	 */
	public Object getProxy() {
		return createAopProxy().getProxy();
	}

	/**
	 * 根据此工厂中的设置创建新代理.
	 * <p>可以反复调用. 如果我们添加或删除了接口，效果会有所不同. 可以添加和删除拦截器.
	 * <p>使用给定的类加载器 (如果需要代理创建).
	 * 
	 * @param classLoader 用于创建代理的类加载器(或 {@code null} 对于低级代理工具的默认值)
	 * 
	 * @return 代理对象
	 */
	public Object getProxy(ClassLoader classLoader) {
		return createAopProxy().getProxy(classLoader);
	}


	/**
	 * 为给定的接口和拦截器创建一个新的代理.
	 * <p>为单个拦截器创建代理的便捷方法, 假设拦截器处理所有调用本身而不是委托给目标, 就像远程代理的情况一样.
	 * 
	 * @param proxyInterface 代理应该实现的接口
	 * @param interceptor 代理应该调用的拦截器
	 * 
	 * @return 代理对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
		return (T) new ProxyFactory(proxyInterface, interceptor).getProxy();
	}

	/**
	 * 为指定的{@code TargetSource}创建代理, 实现指定的接口.
	 * 
	 * @param proxyInterface 代理应该实现的接口
	 * @param targetSource 代理应该调用的TargetSource
	 * 
	 * @return 代理对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
		return (T) new ProxyFactory(proxyInterface, targetSource).getProxy();
	}

	/**
	 * 为指定的{@code TargetSource}创建一个代理，用于扩展{@code TargetSource}的目标类.
	 * 
	 * @param targetSource 代理应该调用的TargetSource
	 * 
	 * @return 代理对象
	 */
	public static Object getProxy(TargetSource targetSource) {
		if (targetSource.getTargetClass() == null) {
			throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
		}
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setProxyTargetClass(true);
		return proxyFactory.getProxy();
	}
}
