package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.web.servlet.LocaleResolver}实现, 在自定义设置的情况下, 使用用户会话中的locale属性,
 * 回退到指定的默认语言环境或请求的accept-header语言环境.
 *
 * <p>如果应用程序无论如何都需要用户会话, 那么这是最合适, i.e. 不必为了存储用户的语言环境而创建{@code HttpSession}.
 * 会话也可以选择包含相关的时区属性; 或者, 可以指定默认时区.
 *
 * <p>自定义控制器可以通过在解析器上调用{@code #setLocale(Context)}来覆盖用户的语言环境和时区, e.g. 响应区域设置更改请求.
 * 作为更方便的替代方案, 考虑使用
 * {@link org.springframework.web.servlet.support.RequestContext#changeLocale}.
 *
 * <p>与{@link CookieLocaleResolver}相反, 此策略将本地选择的区域设置存储在Servlet容器的{@code HttpSession}中.
 * 因此, 这些设置对于每个会话来说都是临时的, 因此在每个会话终止时都会丢失.
 *
 * <p>请注意, 与"Spring Session"项目等外部会话管理机制没有直接关系.
 * 这个{@code LocaleResolver}将简单地针对当前的{@code HttpServletRequest}评估和修改相应的{@code HttpSession}属性.
 */
public class SessionLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 包含Locale的会话属性的名称.
	 * 仅在此实现内部使用.
	 * <p>使用{@code RequestContext(Utils).getLocale()}检索控制器或​​视图中的当前区域设置.
	 */
	public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 包含TimeZone的会话属性的名称.
	 * 仅在此实现内部使用.
	 * <p>使用{@code RequestContext(Utils).getTimeZone()}检索控制器或​​视图中的当前时区.
	 */
	public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".TIME_ZONE";


	private String localeAttributeName = LOCALE_SESSION_ATTRIBUTE_NAME;

	private String timeZoneAttributeName = TIME_ZONE_SESSION_ATTRIBUTE_NAME;


	/**
	 * 在{@code HttpSession}中指定相应属性的名称, 保留当前的{@link Locale}值.
	 * <p>默认是内部{@link #LOCALE_SESSION_ATTRIBUTE_NAME}.
	 */
	public void setLocaleAttributeName(String localeAttributeName) {
		this.localeAttributeName = localeAttributeName;
	}

	/**
	 * 在{@code HttpSession}中指定相应属性的名称, 保留当前的{@link TimeZone}值.
	 * <p>默认是内部{@link #TIME_ZONE_SESSION_ATTRIBUTE_NAME}.
	 */
	public void setTimeZoneAttributeName(String timeZoneAttributeName) {
		this.timeZoneAttributeName = timeZoneAttributeName;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = (Locale) WebUtils.getSessionAttribute(request, this.localeAttributeName);
		if (locale == null) {
			locale = determineDefaultLocale(request);
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				Locale locale = (Locale) WebUtils.getSessionAttribute(request, localeAttributeName);
				if (locale == null) {
					locale = determineDefaultLocale(request);
				}
				return locale;
			}
			@Override
			public TimeZone getTimeZone() {
				TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, timeZoneAttributeName);
				if (timeZone == null) {
					timeZone = determineDefaultTimeZone(request);
				}
				return timeZone;
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, HttpServletResponse response, LocaleContext localeContext) {
		Locale locale = null;
		TimeZone timeZone = null;
		if (localeContext != null) {
			locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		WebUtils.setSessionAttribute(request, this.localeAttributeName, locale);
		WebUtils.setSessionAttribute(request, this.timeZoneAttributeName, timeZone);
	}


	/**
	 * 确定给定请求的默认语言环境, 如果未找到Locale会话属性, 则调用.
	 * <p>默认实现返回指定的默认语言环境, 否则返回请求的accept-header语言环境.
	 * 
	 * @param request 解析语言环境的请求
	 * 
	 * @return 默认语言环境 (never {@code null})
	 */
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}

	/**
	 * 确定给定请求的默认时区, 如果未找到TimeZone会话属性, 则调用.
	 * <p>默认实现返回指定的默认时区, 否则返回{@code null}.
	 * 
	 * @param request 解析时区的请求
	 * 
	 * @return 默认时区 (或{@code null})
	 */
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}
}
