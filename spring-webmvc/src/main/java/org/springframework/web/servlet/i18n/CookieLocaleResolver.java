package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.UsesJava7;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

/**
 * {@link LocaleResolver}实现, 它在自定义设置的情况下使用cookie发送回用户,
 * 回退到指定的默认语言环境或请求的accept-header语言环境.
 *
 * <p>这对于没有用户会话的无状态应用程序特别有用.
 * cookie还可以选择包含相关的时区值; 或者, 可以指定默认时区.
 *
 * <p>自定义控制器可以通过在解析器上调用{@code #setLocale(Context)}来覆盖用户的语言环境和时区, e.g. 响应区域设置更改请求.
 * 作为更方便的替代方案, 考虑使用
 * {@link org.springframework.web.servlet.support.RequestContext#changeLocale}.
 */
public class CookieLocaleResolver extends CookieGenerator implements LocaleContextResolver {

	/**
	 * 包含Locale的请求属性的名称.
	 * <p>仅用于在当前请求过程中更改区域设置时覆盖cookie值!
	 * <p>使用{@code RequestContext(Utils).getLocale()}检索控制器或​​视图中的当前区域设置.
	 */
	public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

	/**
	 * 包含TimeZone的请求属性的名称.
	 * <p>仅用于在当前请求过程中更改区域设置时覆盖cookie值!
	 * <p>使用{@code RequestContext(Utils).getTimeZone()}检索控制器或​​视图中的当前时区.
	 */
	public static final String TIME_ZONE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".TIME_ZONE";

	/**
	 * 如果未显式设置, 则使用默认cookie名称.
	 */
	public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";


	private boolean languageTagCompliant = false;

	private Locale defaultLocale;

	private TimeZone defaultTimeZone;


	/**
	 * 使用{@link #DEFAULT_COOKIE_NAME 默认Cookie名称}.
	 */
	public CookieLocaleResolver() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}


	/**
	 * 指定此解析器的cookie是否应符合BCP 47语言标记, 而不是Java旧的区域规范格式.
	 * 默认为{@code false}.
	 * <p>Note: 此模式需要JDK 7或更高版本. 设置为{@code true} 仅适用于JDK 7+上的BCP 47合规性.
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * 返回此解析器的cookie是否应符合BCP 47语言标记而不是Java旧的区域设置规范格式.
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}

	/**
	 * 如果找不到cookie, 设置此解析器将返回的固定区域设置.
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 如果找不到cookie, 则返回此解析器的固定Locale.
	 */
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	/**
	 * 设置一个固定的TimeZone, 如果找不到cookie, 该解析器将返回它.
	 */
	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 如果没有找到cookie, 则返回此解析器的固定TimeZone.
	 */
	protected TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
	}

	@Override
	public LocaleContext resolveLocaleContext(final HttpServletRequest request) {
		parseLocaleCookieIfNecessary(request);
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				return (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
			}
			@Override
			public TimeZone getTimeZone() {
				return (TimeZone) request.getAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
			}
		};
	}

	private void parseLocaleCookieIfNecessary(HttpServletRequest request) {
		if (request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME) == null) {
			// 检索并解析cookie值.
			Cookie cookie = WebUtils.getCookie(request, getCookieName());
			Locale locale = null;
			TimeZone timeZone = null;
			if (cookie != null) {
				String value = cookie.getValue();
				String localePart = value;
				String timeZonePart = null;
				int spaceIndex = localePart.indexOf(' ');
				if (spaceIndex != -1) {
					localePart = value.substring(0, spaceIndex);
					timeZonePart = value.substring(spaceIndex + 1);
				}
				try {
					locale = (!"-".equals(localePart) ? parseLocaleValue(localePart) : null);
					if (timeZonePart != null) {
						timeZone = StringUtils.parseTimeZoneString(timeZonePart);
					}
				}
				catch (IllegalArgumentException ex) {
					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
						// 错误分派: 忽略locale/timezone 解析异常
						if (logger.isDebugEnabled()) {
							logger.debug("Ignoring invalid locale cookie '" + getCookieName() +
									"' with value [" + value + "] due to error dispatch: " + ex.getMessage());
						}
					}
					else {
						throw new IllegalStateException("Invalid locale cookie '" + getCookieName() +
								"' with value [" + value + "]: " + ex.getMessage());
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale +
							"'" + (timeZone != null ? " and time zone '" + timeZone.getID() + "'" : ""));
				}
			}
			request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
					(locale != null ? locale : determineDefaultLocale(request)));
			request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
					(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
		}
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
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
			addCookie(response,
					(locale != null ? toLocaleValue(locale) : "-") + (timeZone != null ? ' ' + timeZone.getID() : ""));
		}
		else {
			removeCookie(response);
		}
		request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME,
				(locale != null ? locale : determineDefaultLocale(request)));
		request.setAttribute(TIME_ZONE_REQUEST_ATTRIBUTE_NAME,
				(timeZone != null ? timeZone : determineDefaultTimeZone(request)));
	}


	/**
	 * 解析来自传入cookie的给定区域设置值.
	 * <p>默认实现调用{@link StringUtils#parseLocaleString(String)}
	 * 或JDK 7的{@link Locale#forLanguageTag(String)},
	 * 具体取决于{@link #setLanguageTagCompliant "languageTagCompliant"}配置属性.
	 * 
	 * @param locale 要解析的区域设置值
	 * 
	 * @return 相应的{@code Locale}实例
	 */
	@UsesJava7
	protected Locale parseLocaleValue(String locale) {
		return (isLanguageTagCompliant() ? Locale.forLanguageTag(locale) : StringUtils.parseLocaleString(locale));
	}

	/**
	 * 将给定的区域设置渲染为文本值以包含在cookie中.
	 * <p>默认实现调用{@link Locale#toString()} 或JDK 7的{@link Locale#toLanguageTag()},
	 * 具体取决于{@link #setLanguageTagCompliant "languageTagCompliant"}配置属性.
	 * 
	 * @param locale 区域设置
	 * 
	 * @return 给定区域设置的字符串值
	 */
	@UsesJava7
	protected String toLocaleValue(Locale locale) {
		return (isLanguageTagCompliant() ? locale.toLanguageTag() : locale.toString());
	}

	/**
	 * 确定给定请求的默认语言环境, 如果未找到语言环境cookie, 则调用.
	 * <p>默认实现返回指定的默认语言环境, 否则返回请求的accept-header语言环境.
	 * 
	 * @param request 要解析语言环境的请求
	 * 
	 * @return 默认区域设置 (never {@code null})
	 */
	protected Locale determineDefaultLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale == null) {
			defaultLocale = request.getLocale();
		}
		return defaultLocale;
	}

	/**
	 * 确定给定请求的默认时区, 如果未找到TimeZone cookie, 则调用.
	 * <p>默认实现返回指定的默认时区, 否则返回{@code null}.
	 * 
	 * @param request 要解析时区的请求
	 * 
	 * @return 默认时区 (或{@code null})
	 */
	protected TimeZone determineDefaultTimeZone(HttpServletRequest request) {
		return getDefaultTimeZone();
	}

}
