package org.springframework.web.servlet.theme;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 允许通过可配置的请求参数 (默认参数名称: "theme")更改每个请求的当前主题的拦截器.
 */
public class ThemeChangeInterceptor extends HandlerInterceptorAdapter {

	/**
	 * 主题规范参数的默认名称: "theme".
	 */
	public static final String DEFAULT_PARAM_NAME = "theme";

	private String paramName = DEFAULT_PARAM_NAME;


	/**
	 * 在主题更改请求中设置包含主题规范的参数的名称. 默认为"theme".
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * 返回主题更改请求中包含主题规范的参数的名称.
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
