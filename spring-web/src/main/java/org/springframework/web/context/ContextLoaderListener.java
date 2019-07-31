package org.springframework.web.context;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Bootstrap监听器启动并关闭Spring的根{@link WebApplicationContext}.
 * 委托给{@link ContextLoader}以及{@link ContextCleanupListener}.
 *
 * <p>如果使用后者, 则应在{@code web.xml}中{@link org.springframework.web.util.Log4jConfigListener}后注册此监听器.
 *
 * <p>从Spring 3.1开始, {@code ContextLoaderListener}支持通过
 * {@link #ContextLoaderListener(WebApplicationContext)}构造函数注入根Web应用程序上下文,
 * 允许在Servlet 3.0+环境中进行编程配置.
 * 有关用法示例, 请参阅{@link org.springframework.web.WebApplicationInitializer}.
 */
public class ContextLoaderListener extends ContextLoader implements ServletContextListener {

	/**
	 * 基于"contextClass"和"contextConfigLocation" servlet context-params创建Web应用程序上下文.
	 * 有关每个默认值的详细信息, 请参阅{@link ContextLoader}超类文档.
	 * <p>在{@code web.xml}中将{@code ContextLoaderListener}声明为{@code <listener>}时, 通常使用此构造函数.
	 * <p>创建的应用程序上下文将在属性名称
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}下注册到ServletContext中,
	 * 当在此监听器上调用{@link #contextDestroyed}生命周期方法时, 将关闭Spring应用程序上下文.
	 */
	public ContextLoaderListener() {
	}

	/**
	 * 此构造函数在Servlet 3.0+环境中非常有用, 在这些环境中,
	 * 可以通过{@link javax.servlet.ServletContext#addListener} API进行基于实例的监听器注册.
	 * <p>上下文不确定是否{@linkplain org.springframework.context.ConfigurableApplicationContext#refresh() 刷新}.
	 * 如果
	 * (a) 它是{@link ConfigurableWebApplicationContext}的实现, 而且
	 * (b) <strong>没有</strong>刷新 (推荐的方法),
	 * 然后会发生以下情况:
	 * <ul>
	 * <li>如果尚未为给定的上下文分配
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#setId id}, 则会为其分配一个</li>
	 * <li>{@code ServletContext}和{@code ServletConfig}对象将被委托给应用程序上下文</li>
	 * <li>将调用{@link #customizeContext}</li>
	 * <li>将应用通过"contextInitializerClasses" init-param指定的任何
	 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}.</li>
	 * <li>将调用{@link org.springframework.context.ConfigurableApplicationContext#refresh refresh()}</li>
	 * </ul>
	 * 如果上下文已经刷新或未实现{@code ConfigurableWebApplicationContext},
	 * 则假设用户已根据其特定需求执行(或不执行)这些操作, 则不会发生上述任何情况.
	 * <p>有关用法示例, 请参阅{@link org.springframework.web.WebApplicationInitializer}.
	 * <p>在任何情况下, 给定的应用程序上下文将在属性名称
	 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}下注册到ServletContext中,
	 * 当在此侦听器上调用{@link #contextDestroyed}生命周期方法时, 将关闭Spring应用程序上下文.
	 * 
	 * @param context 要管理的应用程序上下文
	 */
	public ContextLoaderListener(WebApplicationContext context) {
		super(context);
	}


	/**
	 * 初始化根Web应用程序上下文.
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		initWebApplicationContext(event.getServletContext());
	}


	/**
	 * 关闭根Web应用程序上下文.
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		closeWebApplicationContext(event.getServletContext());
		ContextCleanupListener.cleanupAttributes(event.getServletContext());
	}
}
