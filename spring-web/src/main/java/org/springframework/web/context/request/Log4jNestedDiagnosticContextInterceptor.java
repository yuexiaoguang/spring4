package org.springframework.web.context.request;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import org.springframework.ui.ModelMap;

/**
 * 请求日志拦截器, 在处理请求之前将请求上下文消息添加到Log4J嵌套诊断上下文(NDC), 在处理请求后再次将其删除.
 *
 * @deprecated 从Spring 4.2.1开始, 支持Apache Log4j 2 (遵循Apache的log4j 1.x的EOL声明)
 */
@Deprecated
public class Log4jNestedDiagnosticContextInterceptor implements AsyncWebRequestInterceptor {

	/** Logger available to subclasses */
	protected final Logger log4jLogger = Logger.getLogger(getClass());

	private boolean includeClientInfo = false;


	/**
	 * Set whether or not the session id and user name should be included
	 * in the log message.
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * Return whether or not the session id and user name should be included
	 * in the log message.
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}


	/**
	 * Adds a message the Log4J NDC before the request is processed.
	 */
	@Override
	public void preHandle(WebRequest request) throws Exception {
		NDC.push(getNestedDiagnosticContextMessage(request));
	}

	/**
	 * Determine the message to be pushed onto the Log4J nested diagnostic context.
	 * <p>Default is the request object's {@code getDescription} result.
	 * @param request current HTTP request
	 * @return the message to be pushed onto the Log4J NDC
	 * @see WebRequest#getDescription
	 * @see #isIncludeClientInfo()
	 */
	protected String getNestedDiagnosticContextMessage(WebRequest request) {
		return request.getDescription(isIncludeClientInfo());
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
	}

	/**
	 * Removes the log message from the Log4J NDC after the request is processed.
	 */
	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
	}

	/**
	 * Removes the log message from the Log4J NDC when the processing thread is
	 * exited after the start of asynchronous request handling.
	 */
	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
	}

}
