package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * 简单请求日志过滤器, 将请求URI (以及可选的查询字符串)写入Commons Log.
 */
public class CommonsRequestLoggingFilter extends AbstractRequestLoggingFilter {

	@Override
	protected boolean shouldLog(HttpServletRequest request) {
		return logger.isDebugEnabled();
	}

	/**
	 * 在处理请求之前写入日志消息.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		logger.debug(message);
	}

	/**
	 * 处理请求后写入日志消息.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		logger.debug(message);
	}

}
