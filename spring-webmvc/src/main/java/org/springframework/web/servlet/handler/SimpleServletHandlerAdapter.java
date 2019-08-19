package org.springframework.web.servlet.handler;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * 将Servlet接口与通用DispatcherServlet一起使用的适配器.
 * 调用Servlet的{@code service}方法来处理请求.
 *
 * <p>未明确支持Last-modified的检查:
 * 这通常由Servlet实现本身处理 (通常源自HttpServlet基类).
 *
 * <p>默认情况下不会激活此适配器; 它需要在DispatcherServlet上下文中定义为bean.
 * 它将自动应用于实现Servlet接口的映射的处理器bean.
 *
 * <p>请注意, 定义为bean的Servlet实例不会接收初始化和销毁​​回调,
 * 除非在DispatcherServlet上下文中定义了一个特殊的后处理器, 如SimpleServletPostProcessor.
 *
 * <p><b>或者, 考虑使用Spring的ServletWrappingController包装Servlet.</b>
 * 这特别适用于现有的Servlet类, 允许指定Servlet初始化参数等.
 */
public class SimpleServletHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Servlet);
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((Servlet) handler).service(request, response);
		return null;
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}

}
