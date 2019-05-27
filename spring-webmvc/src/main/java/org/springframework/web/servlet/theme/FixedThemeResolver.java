package org.springframework.web.servlet.theme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link org.springframework.web.servlet.ThemeResolver} implementation
 * that simply uses a fixed theme. The fixed name can be defined via
 * the "defaultThemeName" property; out of the box, it is "theme".
 *
 * <p>Note: Does not support {@code setThemeName}, as the fixed theme
 * cannot be changed.
 */
public class FixedThemeResolver extends AbstractThemeResolver {

	@Override
	public String resolveThemeName(HttpServletRequest request) {
		return getDefaultThemeName();
	}

	@Override
	public void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName) {
		throw new UnsupportedOperationException("Cannot change theme - use a different theme resolution strategy");
	}

}
