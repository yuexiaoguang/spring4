package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * 请求日志过滤器, 在处理请求之前将请求日志消息添加到Log4J嵌套诊断上下文 (NDC), 在处理请求后再次将其删除.
 *
 * @deprecated 从Spring 4.2.1开始, 支持Apache Log4j 2 (遵循Apache的log4j 1.x的EOL声明)
 */
@Deprecated
public class Log4jNestedDiagnosticContextFilter extends AbstractRequestLoggingFilter {

	/** Logger available to subclasses */
	protected final Logger log4jLogger = Logger.getLogger(getClass());


	/**
	 * Logs the before-request message through Log4J and
	 * adds a message the Log4J NDC before the request is processed.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		if (log4jLogger.isDebugEnabled()) {
			log4jLogger.debug(message);
		}
		NDC.push(getNestedDiagnosticContextMessage(request));
	}

	/**
	 * Determine the message to be pushed onto the Log4J nested diagnostic context.
	 * <p>Default is a plain request log message without prefix or suffix.
	 * @param request current HTTP request
	 * @return the message to be pushed onto the Log4J NDC
	 * @see #createMessage
	 */
	protected String getNestedDiagnosticContextMessage(HttpServletRequest request) {
		return createMessage(request, "", "");
	}

	/**
	 * Removes the log message from the Log4J NDC after the request is processed
	 * and logs the after-request message through Log4J.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
		if (log4jLogger.isDebugEnabled()) {
			log4jLogger.debug(message);
		}
	}

}
