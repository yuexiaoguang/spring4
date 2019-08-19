package org.springframework.web.servlet.theme;

import org.springframework.web.servlet.ThemeResolver;

/**
 * {@link ThemeResolver}实现的抽象基类.
 * 提供对默认主题名称的支持.
 */
public abstract class AbstractThemeResolver implements ThemeResolver {

	/**
	 * 默认主题名称的开箱即用值: "theme".
	 */
	public final static String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	/**
	 * 设置默认主题的名称.
	 * 开箱即用值 "theme".
	 */
	public void setDefaultThemeName(String defaultThemeName) {
		this.defaultThemeName = defaultThemeName;
	}

	/**
	 * 返回默认主题的名称.
	 */
	public String getDefaultThemeName() {
		return this.defaultThemeName;
	}

}
