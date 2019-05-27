package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link TimeZoneAwareLocaleContext}接口的简单实现, 总是返回指定的{@code Locale} 和 {@code TimeZone}.
 *
 * <p>Note: 仅在设置Locale但不设置TimeZone时, 首选使用{@link SimpleLocaleContext}.
 */
public class SimpleTimeZoneAwareLocaleContext extends SimpleLocaleContext implements TimeZoneAwareLocaleContext {

	private final TimeZone timeZone;


	/**
	 * 创建一个新的SimpleTimeZoneAwareLocaleContext, 暴露指定的Locale和TimeZone.
	 * 每个{@link #getLocale()}调用都将返回给定的Locale, 并且每个{@link #getTimeZone()}调用都将返回给定的TimeZone.
	 * 
	 * @param locale 要暴露的Locale
	 * @param timeZone 要暴露的TimeZone
	 */
	public SimpleTimeZoneAwareLocaleContext(Locale locale, TimeZone timeZone) {
		super(locale);
		this.timeZone = timeZone;
	}


	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	@Override
	public String toString() {
		return super.toString() + " " + (this.timeZone != null ? this.timeZone.toString() : "-");
	}

}
