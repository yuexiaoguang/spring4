package org.springframework.web.servlet.theme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.ThemeResolver}实现,
 * 在自定义设置的情况下使用用户会话中的主题属性, 并回退到默认主题.
 * 如果应用程序仍然需要用户会话, 这是最合适的.
 *
 * <p>自定义控制器可以通过调用{@code setThemeName}覆盖用户的主题, e.g. 响应主题更改请求.
 */
public class SessionThemeResolver extends AbstractThemeResolver {

	/**
	 * 保存主题名称的会话属性的名称.
	 * 仅在此实现内部使用.
	 * 使用{@code RequestContext(Utils).getTheme()}检索控制器或​​视图中的当前主题.
	 */
	public static final String THEME_SESSION_ATTRIBUTE_NAME = SessionThemeResolver.class.getName() + ".THEME";


	@Override
	public String resolveThemeName(HttpServletRequest request) {
		String themeName = (String) WebUtils.getSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME);
		// 指出了一个特定主题, 或者是否需要回退到默认值?
		return (themeName != null ? themeName : getDefaultThemeName());
	}

	@Override
	public void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName) {
		WebUtils.setSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME,
				(StringUtils.hasText(themeName) ? themeName : null));
	}

}
