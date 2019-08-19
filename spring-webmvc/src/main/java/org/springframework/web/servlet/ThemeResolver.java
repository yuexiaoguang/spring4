package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基于Web的主题解析策略的接口, 允许通过请求进行主题解析, 并通过请求和响应进行主题修改.
 *
 * <p>该接口允许基于会话, cookies等的实现.
 * 默认实现是{@link org.springframework.web.servlet.theme.FixedThemeResolver}, 只需使用配置的默认主题.
 *
 * <p>请注意, 此解析器仅负责确定当前主题名称.
 * DispatcherServlet通过相应的ThemeSource (i.e. 当前的 WebApplicationContext) 查找已解析主题名称的Theme实例.
 *
 * <p>使用{@link org.springframework.web.servlet.support.RequestContext#getTheme()}
 * 检索控制器或​​视图中的当前主题, 与实际解析策略无关.
 */
public interface ThemeResolver {

	/**
	 * 通过给定的请求解析当前主题名称.
	 * 在任何情况下都应该返回默认主题作为后备.
	 * 
	 * @param request 用于解析的请求
	 * 
	 * @return 当前的主题名称
	 */
	String resolveThemeName(HttpServletRequest request);

	/**
	 * 将当前主题名称设置为给定的主题名称.
	 * 
	 * @param request 用于主题名称修改的请求
	 * @param response 用于主题名称修改的响应
	 * @param themeName 新的主题名称
	 * 
	 * @throws UnsupportedOperationException 如果ThemeResolver实现不支持动态更改主题
	 */
	void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName);

}
