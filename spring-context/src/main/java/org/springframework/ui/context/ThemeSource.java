package org.springframework.ui.context;

/**
 * 由可以解析 {@link Theme Themes}的对象实现的接口.
 * 启用给定“主题”的消息的参数化和国际化.
 */
public interface ThemeSource {

	/**
	 * 返回给定主题名称的Theme实例.
	 * <p>返回的主题将解析特定于主题的消息, 代码, 文件路径等 (e.g. Web环境中的CSS和图像文件).
	 * 
	 * @param themeName 主题的名称
	 * 
	 * @return 相应的主题, 如果没有定义, 则为{@code null}.
	 * 请注意, 按照惯例, ThemeSource至少应该能够返回默认主题名称"theme"的默认主题, 但也可能返回其他主题名称的默认主题.
	 */
	Theme getTheme(String themeName);

}
