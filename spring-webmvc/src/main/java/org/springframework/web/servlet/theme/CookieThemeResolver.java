package org.springframework.web.servlet.theme;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * {@link ThemeResolver}实现, 使用在自定义设置的情况下发送回用户的cookie, 并回退到默认主题.
 * 这对于没有用户会话的无状态应用程序特别有用.
 *
 * <p>因此, 自定义控制器可以通过调用{@code setThemeName}来覆盖用户的主题, e.g. 响应某个主题更改请求.
 */
public class CookieThemeResolver extends CookieGenerator implements ThemeResolver {

	public final static String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	/**
	 * 保存主题名称的请求属性的名称.
	 * 仅用于在当前请求过程中更改主题时覆盖cookie值!
	 * 使用 RequestContext.getTheme() 检索控制器或​​视图中的当前主题.
	 */
	public static final String THEME_REQUEST_ATTRIBUTE_NAME = CookieThemeResolver.class.getName() + ".THEME";

	public static final String DEFAULT_COOKIE_NAME = CookieThemeResolver.class.getName() + ".THEME";


	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	public CookieThemeResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 设置默认主题的名称.
	 */
	public void setDefaultThemeName(String defaultThemeName) {
		this.defaultThemeName = defaultThemeName;
	}

	/**
	 * 返回默认主题的名称.
	 */
	public String getDefaultThemeName() {
		return defaultThemeName;
	}


	@Override
	public String resolveThemeName(HttpServletRequest request) {
		// 检查预解析或预设主题的请求.
		String themeName = (String) request.getAttribute(THEME_REQUEST_ATTRIBUTE_NAME);
		if (themeName != null) {
			return themeName;
		}

		// 从请求中检索cookie值.
		Cookie cookie = WebUtils.getCookie(request, getCookieName());
		if (cookie != null) {
			String value = cookie.getValue();
			if (StringUtils.hasText(value)) {
				themeName = value;
			}
		}

		// 回退到默认主题.
		if (themeName == null) {
			themeName = getDefaultThemeName();
		}
		request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
		return themeName;
	}

	@Override
	public void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName) {
		if (StringUtils.hasText(themeName)) {
			// 设置请求属性并添加cookie.
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, themeName);
			addCookie(response, themeName);
		}
		else {
			// 将请求属性设置为回退主题并删除co​​okie.
			request.setAttribute(THEME_REQUEST_ATTRIBUTE_NAME, getDefaultThemeName());
			removeCookie(response);
		}
	}

}
