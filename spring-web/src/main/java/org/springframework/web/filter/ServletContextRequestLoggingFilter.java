package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * 简单请求日志过滤器, 用于将请求URI (以及可选的查询字符串)写入ServletContext日志.
 */
public class ServletContextRequestLoggingFilter extends AbstractRequestLoggingFilter {

	/**
	 * 在处理请求之前写入日志消息.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

	/**
	 * 处理请求后写入日志消息.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

}
