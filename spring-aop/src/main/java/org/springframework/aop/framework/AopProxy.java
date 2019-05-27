package org.springframework.aop.framework;

/**
 * 已配置的AOP代理的委派接口, 允许创建实际的代理对象.
 *
 * <p>开箱即用的实现可用于JDK动态代理和CGLIB代理, 由{@link DefaultAopProxyFactory}应用.
 */
public interface AopProxy {

	/**
	 * 创建一个新的代理对象.
	 * <p>使用AopProxy的默认类加载器 (如果有必要):
	 * 通常, 线程上下文类加载器.
	 * 
	 * @return 新的代理对象 (never {@code null})
	 */
	Object getProxy();

	/**
	 * 创建一个新的代理对象.
	 * <p>使用给定的类加载器 (如果有必要).
	 * {@code null} 将简单地传递下来, 从而导致低级代理工具的默认值, 通常不同于AopProxy实现的{@link #getProxy()} 方法所选择的默认值.
	 * 
	 * @param classLoader 用于创建代理的类加载器 (或{@code null}表示低级代理工具的默认值)
	 * 
	 * @return 新的代理对象 (never {@code null})
	 */
	Object getProxy(ClassLoader classLoader);

}
