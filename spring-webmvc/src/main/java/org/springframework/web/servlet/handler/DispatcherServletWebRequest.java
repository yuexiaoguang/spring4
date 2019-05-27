package org.springframework.web.servlet.handler;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * {@link ServletWebRequest} subclass that is aware of
 * {@link org.springframework.web.servlet.DispatcherServlet}'s
 * request context, such as the Locale determined by the configured
 * {@link org.springframework.web.servlet.LocaleResolver}.
 */
public class DispatcherServletWebRequest extends ServletWebRequest {

	/**
	 * Create a new DispatcherServletWebRequest instance for the given request.
	 * @param request current HTTP request
	 */
	public DispatcherServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * Create a new DispatcherServletWebRequest instance for the given request and response.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 */
	public DispatcherServletWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	@Override
	public Locale getLocale() {
		return RequestContextUtils.getLocale(getRequest());
	}

}
