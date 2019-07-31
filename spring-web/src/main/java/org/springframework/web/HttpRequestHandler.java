package org.springframework.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于处理HTTP请求的组件的普通处理器接口, 类似于Servlet.
 * 只声明{@link javax.servlet.ServletException}和{@link java.io.IOException},
 * 以允许在任何{@link javax.servlet.http.HttpServlet}中使用.
 * 这个接口本质上是HttpServlet的直接等价物, 简化为中央句柄方法.
 *
 * <p>在Spring样式中公开HttpRequestHandler bean的最简单方法是在Spring的根Web应用程序上下文中定义它,
 * 并在{@code web.xml}中定义{@link org.springframework.web.context.support.HttpRequestHandlerServlet},
 * 通过其需要匹配目标bean名称的{@code servlet-name}指向目标HttpRequestHandler bean.
 *
 * <p>在Spring的{@link org.springframework.web.servlet.DispatcherServlet}中作为处理器类型支持,
 * 能够与调度器的高级映射和拦截工具进行交互.
 * 这是公开HttpRequestHandler的推荐方法, 同时保持处理器实现不直接依赖DispatcherServlet环境.
 *
 * <p>通常实现为直接生成二进制响应, 不涉及单独的视图资源.
 * 这与Spring的Web MVC框架中的{@link org.springframework.web.servlet.mvc.Controller}不同.
 * 缺少{@link org.springframework.web.servlet.ModelAndView}返回值为DispatcherServlet以外的调用者提供了更清晰的签名,
 * 表明永远不会有视图呈现.
 *
 * <p>从Spring 2.0开始, Spring的基于HTTP的远程导出器,
 * 如{@link org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter}
 * 和{@link org.springframework.remoting.caucho.HessianServiceExporter},
 * 实现了此接口, 而不是更广泛的Controller接口, 以最小化对特定于Spring的Web基础结构的依赖性.
 *
 * <p>请注意, HttpRequestHandlers可以选择实现
 * {@link org.springframework.web.servlet.mvc.LastModified}接口,
 * 就像 Controller, <i>前提是它们在Spring的DispatcherServlet中运行</i>.
 * 但是, 这通常不是必需的, 因为HttpRequestHandlers通常仅支持POST请求.
 * 或者, 处理器可以在其{@code handle}方法中手动实现"If-Modified-Since" HTTP header处理.
 */
public interface HttpRequestHandler {

	/**
	 * 处理给定的请求, 生成响应.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

}
