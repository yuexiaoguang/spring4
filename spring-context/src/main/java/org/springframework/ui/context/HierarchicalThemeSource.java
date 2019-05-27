package org.springframework.ui.context;

/**
 * ThemeSource的子接口, 由可以分层次地解析主题消息的对象实现.
 */
public interface HierarchicalThemeSource extends ThemeSource {

	/**
	 * 设置将用于尝试解析此对象无法解析的主题消息的父级.
	 * 
	 * @param parent 将用于解析此对象无法解析的消息的父级ThemeSource.
	 * 可能是{@code null}, 在这种情况下无法进一步解析.
	 */
	void setParentThemeSource(ThemeSource parent);

	/**
	 * 返回此ThemeSource的父级, 如果没有, 则返回{@code null}.
	 */
	ThemeSource getParentThemeSource();

}
