package org.springframework.web.servlet.theme;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Interceptor that allows for changing the current theme on every request,
 * via a configurable request parameter (default parameter name: "theme").
 */
public class ThemeChangeInterceptor extends HandlerInterceptorAdapter {

	/**
	 * Default name of the theme specification parameter: "theme".
	 */
	public static final String DEFAULT_PARAM_NAME = "theme";

	private String paramName = DEFAULT_PARAM_NAME;


	/**
	 * Set the name of the parameter that contains a theme specification
	 * in a theme change request. Default is "theme".
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * Return the name of the parameter that contains a theme specification
	 * in a theme change request.
	 */
	public String getParamName() {
		return this.paramName;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		String newTheme = request.getParameter(this.paramName);
		if (newTheme != null) {
			ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(request);
			if (themeResolver == null) {
				throw new IllegalStateException("No ThemeResolver found: not in a DispatcherServlet request?");
			}
			themeResolver.setThemeName(request, response, newTheme);
		}
		// Proceed in any case.
		return true;
	}

}
