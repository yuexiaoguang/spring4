package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.web.servlet.LocaleContextResolver;

/**
 * {@link LocaleContextResolver}实现的抽象基类.
 * 提供对默认语言环境和默认时区的支持.
 *
 * <p>还提供{@link #resolveLocale}和{@link #setLocale}的预先实现版本,
 * 委托给{@link #resolveLocaleContext}和{@link #setLocaleContext}.
 */
public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {

	private TimeZone defaultTimeZone;


	/**
	 * 设置默认TimeZone.
	 */
	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * 返回默认TimeZone.
	 */
	public TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		return resolveLocaleContext(request).getLocale();
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

}
