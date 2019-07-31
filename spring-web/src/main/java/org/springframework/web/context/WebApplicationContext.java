package org.springframework.web.context;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;

/**
 * 用于为Web应用程序提供配置的接口.
 * 这在应用程序运行时是只读的, 但如果实现支持, 则可以重新加载.
 *
 * <p>此接口将{@code getServletContext()}方法添加到通用ApplicationContext接口,
 * 并定义一个众所周知的应用程序属性名称, 根上下文必须在引导过程中绑定到该名称.
 *
 * <p>与通用应用程序上下文一样, Web应用程序上下文是分层的.
 * 每个应用程序都有一个根上下文, 而应用程序中的每个servlet (包括MVC框架中的调度程序servlet)都有自己的子上下文.
 *
 * <p>除了标准的应用程序上下文生命周期功能外, WebApplicationContext实现还需要检测{@link ServletContextAware} bean,
 * 并相应地调用{@code setServletContext}方法.
 */
public interface WebApplicationContext extends ApplicationContext {

	/**
	 * 用于在成功启动时将根WebApplicationContext绑定到的Context属性.
	 * <p>Note: 如果根上下文启动失败, 则此属性可以包含异常或错误作为值.
	 * 使用WebApplicationContextUtils可以方便地查找根WebApplicationContext.
	 */
	String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

	/**
	 * 请求范围的范围标识符: "request".
	 * 除了标准范围"singleton" 和 "prototype"之外的支持.
	 */
	String SCOPE_REQUEST = "request";

	/**
	 * 会话范围的范围标识符: "session".
	 * 除了标准范围"singleton" 和 "prototype"之外的支持.
	 */
	String SCOPE_SESSION = "session";

	/**
	 * 全局会话范围的范围标识符: "globalSession".
	 * 除了标准范围"singleton" 和 "prototype"之外的支持.
	 */
	String SCOPE_GLOBAL_SESSION = "globalSession";

	/**
	 * 全局Web应用程序范围的范围标识符: "application".
	 * 除了标准范围"singleton" 和 "prototype"之外的支持.
	 */
	String SCOPE_APPLICATION = "application";

	/**
	 * 工厂中ServletContext环境bean的名称.
	 */
	String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

	/**
	 * 工厂中ServletContext / PortletContext init-params环境bean的名称.
	 * <p>Note: 可能与ServletConfig/PortletConfig参数合并.
	 * ServletConfig参数覆盖同名的ServletContext参数.
	 */
	String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

	/**
	 * 工厂中ServletContext/PortletContext属性环境bean的名称.
	 */
	String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";


	/**
	 * 返回此应用程序的标准Servlet API ServletContext.
	 * <p>除了PortletContext之外, 还可用于Portlet应用程序.
	 */
	ServletContext getServletContext();

}
