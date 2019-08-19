package org.springframework.web.servlet;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;

/**
 * 扩展{@link LocaleResolver}, 添加对丰富的语言环境上下文的支持 (可能包括语言环境和时区信息).
 */
public interface LocaleContextResolver extends LocaleResolver {

	/**
	 * 通过给定的请求解析当前的语言环境上下文.
	 * <p>这主要用于框架级处理; 考虑使用
	 * {@link org.springframework.web.servlet.support.RequestContextUtils}
	 * 或 {@link org.springframework.web.servlet.support.RequestContext}
	 * 对当前区域设置和/或时区进行应用程序级访问.
	 * <p>返回的上下文可以是{@link org.springframework.context.i18n.TimeZoneAwareLocaleContext},
	 * 其中包含具有关联时区信息的语言环境.
	 * 只需相应地应用{@code instanceof}检查和向下转型.
	 * <p>自定义解析器实现还可以在返回的上下文中返回额外的设置, 也可以通过向下转型来访问.
	 * 
	 * @param request 用于解析语言环境上下文的请求
	 * 
	 * @return 当前的语言环境 (never {@code null}
	 */
	LocaleContext resolveLocaleContext(HttpServletRequest request);

	/**
	 * 将当前区域设置上下文设置为给定的区域设置, 可能包括具有关联时区信息的区域设置.
	 * 
	 * @param request 用于区域设置修改的请求
	 * @param response 用于区域设置修改的响应
	 * @param localeContext 新的语言环境上下文, 或{@code null}来清除语言环境
	 * 
	 * @throws UnsupportedOperationException 如果LocaleResolver实现不支持动态更改语言环境或时区
	 */
	void setLocaleContext(HttpServletRequest request, HttpServletResponse response, LocaleContext localeContext);

}
