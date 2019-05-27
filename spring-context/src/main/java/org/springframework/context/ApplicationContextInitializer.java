package org.springframework.context;

/**
 * 用于在 {@linkplain ConfigurableApplicationContext#refresh() 刷新}之前,
 * 初始化Spring {@link ConfigurableApplicationContext}的回调接口.
 *
 * <p>通常在需要对应用程序上下文进行某些编程初始化的Web应用程序中使用.
 * 例如, 根据{@linkplain ConfigurableApplicationContext#getEnvironment() 上下文的环境}注册属性源或激活配置文件.
 * 请参阅{@code ContextLoader}和{@code FrameworkServlet}支持, 分别声明 "contextInitializerClasses" context-param 和 init-param.
 *
 * <p>鼓励{@code ApplicationContextInitializer}处理器检测是否已实现Spring的 {@link org.springframework.core.Ordered Ordered}接口,
 *  或者是否存在 @{@link org.springframework.core.annotation.Order Order}注解, 并在调用之前相应地对实例进行排序.
 */
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * 初始化给定的应用程序上下文.
	 * 
	 * @param applicationContext 要配置的应用程序
	 */
	void initialize(C applicationContext);

}
