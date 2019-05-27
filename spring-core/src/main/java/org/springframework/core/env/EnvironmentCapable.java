package org.springframework.core.env;

/**
 * 指示包含和公开{@link Environment}引用的组件的接口.
 *
 * <p>所有Spring应用程序上下文都是 EnvironmentCapable, 并且该接口主要用于在接受BeanFactory实例的框架方法中执行{@code instanceof}检查,
 * 这些实例实际上可能是也可能不是ApplicationContext实例, 以便在可行的情况下与环境进行交互.
 *
 * <p>如上所述, {@link org.springframework.context.ApplicationContext ApplicationContext}扩展了 EnvironmentCapable,
 * 从而公开了一个{@link #getEnvironment()}方法;
 * 但是,
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * 重新定义了{@link org.springframework.context.ConfigurableApplicationContext#getEnvironment getEnvironment()},
 * 并缩小签名以返回{@link ConfigurableEnvironment}.
 * 结果是一个Environment对象是'只读', 直到从ConfigurableApplicationContext访问它, 此时它才可以被配置.
 */
public interface EnvironmentCapable {

	/**
	 * 返回与此组件关联的{@link Environment} (可能是{@code null}或默认环境).
	 */
	Environment getEnvironment();

}
