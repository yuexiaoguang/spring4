package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * 适配器, 使用普通{@link org.springframework.web.HttpRequestHandler}接口
 * 与通用{@link org.springframework.web.servlet.DispatcherServlet}.
 * 支持实现{@link LastModified}接口的处理器.
 *
 * <p>这是一个SPI类, 不是由应用程序代码直接使用.
 */
public class HttpRequestHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof HttpRequestHandler);
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((HttpRequestHandler) handler).handleRequest(request, response);
		return null;
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		if (handler instanceof LastModified) {
			return ((LastModified) handler).getLastModified(request);
		}
		return -1L;
	}
}
