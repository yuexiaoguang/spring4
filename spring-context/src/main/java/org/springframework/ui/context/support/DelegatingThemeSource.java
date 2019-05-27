package org.springframework.ui.context.support;

import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * 空的ThemeSource, 它将所有调用委托给父ThemeSource.
 * 如果没有父级可用, 则根本无法解析任何主题.
 *
 * <p>如果上下文没有定义自己的ThemeSource, 则用作UiApplicationContextUtils的占位符.
 * 不适合直接用于应用程序.
 */
public class DelegatingThemeSource implements HierarchicalThemeSource {

	private ThemeSource parentThemeSource;


	@Override
	public void setParentThemeSource(ThemeSource parentThemeSource) {
		this.parentThemeSource = parentThemeSource;
	}

	@Override
	public ThemeSource getParentThemeSource() {
		return parentThemeSource;
	}


	@Override
	public Theme getTheme(String themeName) {
		if (this.parentThemeSource != null) {
			return this.parentThemeSource.getTheme(themeName);
		}
		else {
			return null;
		}
	}

}
