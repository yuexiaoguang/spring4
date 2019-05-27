package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * Simple request logging filter that writes the request URI
 * (and optionally the query string) to the ServletContext log.
 */
public class ServletContextRequestLoggingFilter extends AbstractRequestLoggingFilter {

	/**
	 * Writes a log message before the request is processed.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

	/**
	 * Writes a log message after the request is processed.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

}
