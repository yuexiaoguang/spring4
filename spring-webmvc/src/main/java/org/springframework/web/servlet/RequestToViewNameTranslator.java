package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * Strategy interface for translating an incoming
 * {@link javax.servlet.http.HttpServletRequest} into a
 * logical view name when no view name is explicitly supplied.
 */
public interface RequestToViewNameTranslator {

	/**
	 * Translate the given {@link HttpServletRequest} into a view name.
	 * @param request the incoming {@link HttpServletRequest} providing
	 * the context from which a view name is to be resolved
	 * @return the view name (or {@code null} if no default found)
	 * @throws Exception if view name translation fails
	 */
	String getViewName(HttpServletRequest request) throws Exception;

}
