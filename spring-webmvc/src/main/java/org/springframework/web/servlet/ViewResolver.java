package org.springframework.web.servlet;

import java.util.Locale;

/**
 * 由可以按名称解析视图的对象实现的接口.
 *
 * <p>在运行应用程序期间, 视图状态不会更改, 因此实现可以自由地缓存视图.
 *
 * <p>鼓励实现支持国际化, 即本地化视图解析.
 */
public interface ViewResolver {

	/**
	 * 按名称解析给定视图.
	 * <p>Note: 要允许ViewResolver链接, 如果未在其中定义具有给定名称的视图, 则ViewResolver应返回{@code null}.
	 * 但是, 这不是必需的: 某些ViewResolvers将始终尝试使用给定名称构建View对象, 无法返回{@code null}
	 * (而在View创建失败时抛出异常).
	 * 
	 * @param viewName 要解析的视图的名称
	 * @param locale 用于解析视图的区域设置.
	 * 支持国际化的ViewResolver应该尊重这一点.
	 * 
	 * @return View对象, 或{@code null} (可选, 允许ViewResolver链接)
	 * @throws Exception 如果视图无法解析 (通常在创建实际View对象时出现问题)
	 */
	View resolveViewName(String viewName, Locale locale) throws Exception;

}
