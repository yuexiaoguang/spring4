package org.springframework.web.servlet;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基于Web的区域设置解析策略的接口, 允许通过请求进行区域设置解析, 并通过请求和响应修改区域设置.
 *
 * <p>该接口允许基于请求, 会话, cookies等的实现.
 * 默认实现是{@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver},
 * 只需使用相应的HTTP header提供的请求的语言环境.
 *
 * <p>使用{@link org.springframework.web.servlet.support.RequestContext#getLocale()}
 * 检索控制器或​​视图中的当前区域设置, 与实际的解析策略无关.
 *
 * <p>Note: 从Spring 4.0开始, 有一个名为{@link LocaleContextResolver}的扩展策略接口,
 * 允许解析{@link org.springframework.context.i18n.LocaleContext}对象, 可能包括相关的时区信息.
 * Spring提供的解析器实现在适当的地方实现扩展的{@link LocaleContextResolver}接口.
 */
public interface LocaleResolver {

	/**
	 * 通过给定请求解析当前区域设置.
	 * 在任何情况下都可以返回默认语言环境作为回退.
	 * 
	 * @param request 用于解析语言环境的请求
	 * 
	 * @return 当前的语言环境 (never {@code null})
	 */
	Locale resolveLocale(HttpServletRequest request);

	/**
	 * 将当前区域设置设置为给定的区域设置.
	 * 
	 * @param request 用于区域设置修改的请求
	 * @param response 用于区域设置修改的响应
	 * @param locale 新的语言环境, 或{@code null}来清除语言环境
	 * 
	 * @throws UnsupportedOperationException 如果LocaleResolver实现不支持动态更改语言环境
	 */
	void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale);

}
