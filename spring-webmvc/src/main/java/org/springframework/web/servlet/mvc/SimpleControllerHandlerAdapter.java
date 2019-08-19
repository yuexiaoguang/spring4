package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * 适配器, 使用普通{@link Controller}工作流接口和通用{@link org.springframework.web.servlet.DispatcherServlet}.
 * 支持实现{@link LastModified}接口的处理器.
 *
 * <p>这是一个SPI类, 不是由应用程序代码直接使用.
 */
public class SimpleControllerHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Controller);
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return ((Controller) handler).handleRequest(request, response);
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		if (handler instanceof LastModified) {
			return ((LastModified) handler).getLastModified(request);
		}
		return -1L;
	}

}
