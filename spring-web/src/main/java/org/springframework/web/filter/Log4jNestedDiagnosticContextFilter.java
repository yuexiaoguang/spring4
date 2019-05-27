package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Request logging filter that adds the request log message to the Log4J
 * nested diagnostic context (NDC) before the request is processed,
 * removing it again after the request is processed.
 *
 * @deprecated as of Spring 4.2.1, in favor of Apache Log4j 2
 * (following Apache's EOL declaration for log4j 1.x)
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
