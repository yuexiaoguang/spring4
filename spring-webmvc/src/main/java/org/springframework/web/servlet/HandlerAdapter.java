package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * MVC框架SPI, 允许核心MVC工作流的参数化.
 *
 * <p>必须为每个处理器类型实现的接口, 以处理请求.
 * 此接口用于允许{@link DispatcherServlet}无限扩展.
 * {@code DispatcherServlet}通过此接口访问所有已安装的处理器, 这意味着它不包含特定于任何处理器类型的代码.
 *
 * <p>请注意, 处理器可以是{@code Object}类型.
 * 这是为了使其他框架的处理器能够与此框架集成, 而无需自定义编码, 以及允许不遵循任何特定Java接口的注解驱动的处理器对象.
 *
 * <p>此接口不适用于应用程序开发人员.
 * 它适用于想要开发自己的Web工作流程的处理器.
 *
 * <p>Note: {@code HandlerAdapter}实现者可以实现{@link org.springframework.core.Ordered}接口,
 * 以便能够指定由{@code DispatcherServlet}应用的排序顺序(因此也是优先级).
 * 非有序实例被视为最低优先级.
 */
public interface HandlerAdapter {

	/**
	 * 给定一个处理器实例, 返回此{@code HandlerAdapter}是否可以支持它.
	 * 典型的HandlerAdapters将根据处理器类型做出决定.
	 * HandlerAdapter通常只支持一种处理器类型.
	 * <p>典型的实现:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * 
	 * @param handler 要检查的handler对象
	 * 
	 * @return 此对象是否可以使用给定的处理器
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理器来处理此请求.
	 * 所需的工作流程可能差异很大.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 要使用的处理器.
	 * 此对象必须已经过此接口的{@code supports}方法检查, 并且该方法返回{@code true}.
	 * 
	 * @return 包含视图名称和所需的模型数据的ModelAndView对象, 或{@code null} 如果请求已直接处理
	 * @throws Exception
	 */
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * 与HttpServlet的{@code getLastModified}方法相同的约定.
	 * 如果处理器类中没有支持, 则可以简单地返回-1.
	 * 
	 * @param request 当前的HTTP请求
	 * @param handler 要使用的处理器
	 * 
	 * @return 给定处理器的lastModified值
	 */
	long getLastModified(HttpServletRequest request, Object handler);

}
