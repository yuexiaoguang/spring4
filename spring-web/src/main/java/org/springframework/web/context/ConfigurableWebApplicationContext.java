package org.springframework.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * 由可配置的Web应用程序上下文实现的接口.
 * 由{@link ContextLoader}和{@link org.springframework.web.servlet.FrameworkServlet}提供支持.
 *
 * <p>Note: 在调用从{@link org.springframework.context.ConfigurableApplicationContext}
 * 继承的{@link #refresh}方法之前, 需要调用此接口的setter.
 * 它们不会导致自己初始化上下文.
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {

	/**
	 * ApplicationContext id的前缀, 用于引用上下文路径和/或servlet名称.
	 */
	String APPLICATION_CONTEXT_ID_PREFIX = WebApplicationContext.class.getName() + ":";

	/**
	 * 工厂中ServletConfig环境bean的名称.
	 */
	String SERVLET_CONFIG_BEAN_NAME = "servletConfig";


	/**
	 * 为此Web应用程序上下文设置ServletContext.
	 * <p>不会导致上下文初始化: 需要在设置所有配置属性后调用refresh.
	 */
	void setServletContext(ServletContext servletContext);

	/**
	 * 为此Web应用程序上下文设置ServletConfig.
	 * 仅调用属于特定Servlet的WebApplicationContext.
	 */
	void setServletConfig(ServletConfig servletConfig);

	/**
	 * 返回此Web应用程序上下文的ServletConfig.
	 */
	ServletConfig getServletConfig();

	/**
	 * 设置此Web应用程序上下文的命名空间, 以用于构建默认上下文配置位置.
	 * 根Web应用程序上下文没有命名空间.
	 */
	void setNamespace(String namespace);

	/**
	 * 返回此Web应用程序上下文的命名空间.
	 */
	String getNamespace();

	/**
	 * 以init-param样式设置此Web应用程序上下文的配置位置, i.e. 使用逗号, 分号, 或空格分隔的不同位置.
	 * <p>如果未设置, 则实现应该根据需要使用给定命名空间或根Web应用程序上下文的默认值.
	 */
	void setConfigLocation(String configLocation);

	/**
	 * 设置此Web应用程序上下文的配置位置.
	 * <p>如果未设置, 则实现应该根据需要使用给定命名空间或根Web应用程序上下文的默认值.
	 */
	void setConfigLocations(String... configLocations);

	/**
	 * 返回此Web应用程序上下文的配置位置, 或{@code null}.
	 */
	String[] getConfigLocations();

}
