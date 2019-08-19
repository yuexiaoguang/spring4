package org.springframework.web.servlet.i18n;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

/**
 * {@link LocaleResolver}实现, 它只使用HTTP请求的"accept-language" header中指定的主要语言环境
 * (即客户端浏览器发送的语言环境, 通常是客户端操作系统的语言环境).
 *
 * <p>Note: 不支持{@code setLocale}, 因为只能通过更改客户端的区域设置来更改accept header.
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {

	private final List<Locale> supportedLocales = new ArrayList<Locale>(4);

	private Locale defaultLocale;


	/**
	 * 配置支持的语言环境以检查通过{@link HttpServletRequest#getLocales()}确定的请求语言环境.
	 * 如果未配置, 则使用{@link HttpServletRequest#getLocale()}.
	 * 
	 * @param locales 支持的区域设置
	 */
	public void setSupportedLocales(List<Locale> locales) {
		this.supportedLocales.clear();
		if (locales != null) {
			this.supportedLocales.addAll(locales);
		}
	}

	/**
	 * 返回配置的受支持的语言环境列表.
	 */
	public List<Locale> getSupportedLocales() {
		return this.supportedLocales;
	}

	/**
	 * 如果请求没有"Accept-Language" header, 配置固定的作为后备的默认语言环境.
	 * <p>默认在没有设置"Accept-Language" header的情况下,
	 * 服务器的默认语言环境按{@link HttpServletRequest#getLocale()}中的定义使用.
	 * 
	 * @param defaultLocale 要使用的默认语言环境
	 */
	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 配置的默认语言环境.
	 */
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale != null && request.getHeader("Accept-Language") == null) {
			return defaultLocale;
		}
		Locale requestLocale = request.getLocale();
		List<Locale> supportedLocales = getSupportedLocales();
		if (supportedLocales.isEmpty() || supportedLocales.contains(requestLocale)) {
			return requestLocale;
		}
		Locale supportedLocale = findSupportedLocale(request, supportedLocales);
		if (supportedLocale != null) {
			return supportedLocale;
		}
		return (defaultLocale != null ? defaultLocale : requestLocale);
	}

	private Locale findSupportedLocale(HttpServletRequest request, List<Locale> supportedLocales) {
		Enumeration<Locale> requestLocales = request.getLocales();
		Locale languageMatch = null;
		while (requestLocales.hasMoreElements()) {
			Locale locale = requestLocales.nextElement();
			if (supportedLocales.contains(locale)) {
				if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
					// 完全匹配: language + country, 可能与之前的语言匹配范围更小
					return locale;
				}
			}
			else if (languageMatch == null) {
				// 尝试查找仅限语言的匹配作为后备
				for (Locale candidate : supportedLocales) {
					if (!StringUtils.hasLength(candidate.getCountry()) &&
							candidate.getLanguage().equals(locale.getLanguage())) {
						languageMatch = candidate;
						break;
					}
				}
			}
		}
		return languageMatch;
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP accept header - use a different locale resolution strategy");
	}

}
