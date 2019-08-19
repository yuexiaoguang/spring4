package org.springframework.web.servlet.i18n;

import java.util.Locale;

import org.springframework.web.servlet.LocaleResolver;

/**
 * {@link LocaleResolver}实现的抽象基类.
 * 提供对默认语言环境的支持.
 */
public abstract class AbstractLocaleResolver implements LocaleResolver {

	private Locale defaultLocale;


	/**
	 * 设置默认区域设置.
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 返回默认区域设置.
	 */
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}

}
