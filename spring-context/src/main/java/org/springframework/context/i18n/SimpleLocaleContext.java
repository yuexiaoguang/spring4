package org.springframework.context.i18n;

import java.util.Locale;

/**
 * {@link LocaleContext}接口的简单实现, 始终返回指定的 {@code Locale}.
 */
public class SimpleLocaleContext implements LocaleContext {

	private final Locale locale;


	/**
	 * 创建一个暴露指定Locale的新SimpleLocaleContext.
	 * 每个{@link #getLocale()}调用都将返回此Locale.
	 * 
	 * @param locale 要暴露的Locale
	 */
	public SimpleLocaleContext(Locale locale) {
		this.locale = locale;
	}

	@Override
	public Locale getLocale() {
		return this.locale;
	}

	@Override
	public String toString() {
		return (this.locale != null ? this.locale.toString() : "-");
	}

}
