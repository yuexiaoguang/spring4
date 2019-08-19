package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;

/**
 * {@link org.springframework.web.servlet.LocaleResolver}实现,
 * 始终返回固定的默认语言环境和可选的时区.
 * 默认是当前JVM的默认语言环境.
 *
 * <p>Note: 不支持{@code setLocale(Context)}, 因为无法更改固定的语言环境和时区.
 */
public class FixedLocaleResolver extends AbstractLocaleContextResolver {

	/**
	 * 公开配置的默认语言环境(或JVM的默认语言环境作为回退).
	 */
	public FixedLocaleResolver() {
		setDefaultLocale(Locale.getDefault());
	}

	/**
	 * @param locale 要公开的语言环境
	 */
	public FixedLocaleResolver(Locale locale) {
		setDefaultLocale(locale);
	}

	/**
	 * @param locale 要公开的语言环境
	 * @param timeZone 要公开的时区
	 */
	public FixedLocaleResolver(Locale locale, TimeZone timeZone) {
		setDefaultLocale(locale);
		setDefaultTimeZone(timeZone);
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale locale = getDefaultLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		return locale;
	}

	@Override
	public LocaleContext resolveLocaleContext(HttpServletRequest request) {
		return new TimeZoneAwareLocaleContext() {
			@Override
			public Locale getLocale() {
				return getDefaultLocale();
			}
			@Override
			public TimeZone getTimeZone() {
				return getDefaultTimeZone();
			}
		};
	}

	@Override
	public void setLocaleContext(HttpServletRequest request, HttpServletResponse response, LocaleContext localeContext) {
		throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
	}

}
