package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

/**
 * Additional interface that a {@link HandlerMapping} can implement to expose
 * a request matching API aligned with its internal request matching
 * configuration and implementation.
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * Determine whether the given request matches the request criteria.
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 */
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
