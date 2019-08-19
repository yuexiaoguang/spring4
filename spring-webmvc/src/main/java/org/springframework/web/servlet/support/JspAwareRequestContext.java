package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;

/**
 * RequestContext的JSP-aware (和JSTL-aware)子类, 允许来自{@code javax.servlet.jsp.PageContext}的上下文的填充.
 *
 * <p>除了基类提供的回退区域设置策略之外, 此上下文还将检测 page/request/session/application范围中的JSTL区域设置属性.
 */
public class JspAwareRequestContext extends RequestContext {

	private PageContext pageContext;


	/**
	 * 使用错误检索的请求属性为给定的页面上下文创建新的JspAwareRequestContext.
	 * 
	 * @param pageContext 当前的JSP页面上下文
	 */
	public JspAwareRequestContext(PageContext pageContext) {
		initContext(pageContext, null);
	}

	/**
	 * 使用给定的模型属性进行错误检索, 为给定的页面上下文创建新的JspAwareRequestContext.
	 * 
	 * @param pageContext 当前的JSP页面上下文
	 * @param model 当前视图的模型属性 (可以是{@code null}, 使用Errors检索的请求属性)
	 */
	public JspAwareRequestContext(PageContext pageContext, Map<String, Object> model) {
		initContext(pageContext, model);
	}

	/**
	 * 使用给定的模型属性进行错误检索, 使用给定的页面上下文初始化此上下文.
	 * 
	 * @param pageContext 当前的JSP页面上下文
	 * @param model 当前视图的模型属性 (可以是{@code null}, 使用Errors检索的请求属性)
	 */
	protected void initContext(PageContext pageContext, Map<String, Object> model) {
		if (!(pageContext.getRequest() instanceof HttpServletRequest)) {
			throw new IllegalArgumentException("RequestContext only supports HTTP requests");
		}
		this.pageContext = pageContext;
		initContext((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
				pageContext.getServletContext(), model);
	}


	/**
	 * 返回底层的PageContext.
	 * 仅适用于此程序包中的协作类.
	 */
	protected final PageContext getPageContext() {
		return this.pageContext;
	}

	/**
	 * 此实现检查 page, request, session 或 application范围中的JSTL语言环境属性; 如果找不到, 则返回{@code HttpServletRequest.getLocale()}.
	 */
	@Override
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			Locale locale = JstlPageLocaleResolver.getJstlLocale(getPageContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * 此实现检查 page, request, session 或 application范围中的JSTL时区属性; 如果没有找到, 则返回{@code null}.
	 */
	@Override
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			TimeZone timeZone = JstlPageLocaleResolver.getJstlTimeZone(getPageContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}


	/**
	 * 隔离JSTL依赖项的内部类.
	 * 如果存在JSTL API, 则调用以解析回退区域设置.
	 */
	private static class JstlPageLocaleResolver {

		public static Locale getJstlLocale(PageContext pageContext) {
			Object localeObject = Config.find(pageContext, Config.FMT_LOCALE);
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		public static TimeZone getJstlTimeZone(PageContext pageContext) {
			Object timeZoneObject = Config.find(pageContext, Config.FMT_TIME_ZONE);
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}

}
