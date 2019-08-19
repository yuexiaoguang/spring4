package org.springframework.web.servlet.theme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 仅使用固定主题的{@link org.springframework.web.servlet.ThemeResolver}实现.
 * 固定名称可以通过"defaultThemeName"属性定义; 开箱即用, 为"theme".
 *
 * <p>Note: 不支持{@code setThemeName}, 因为无法更改固定主题.
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
