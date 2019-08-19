package org.springframework.web.servlet.i18n;

import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.UsesJava7;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 允许通过可配置的请求参数(默认参数名称: "locale"), 更改每个请求的当前区域设置的拦截器.
 */
public class LocaleChangeInterceptor extends HandlerInterceptorAdapter {

	/**
	 * 语言环境规范参数的默认名称: "locale".
	 */
	public static final String DEFAULT_PARAM_NAME = "locale";


	protected final Log logger = LogFactory.getLog(getClass());

	private String paramName = DEFAULT_PARAM_NAME;

	private String[] httpMethods;

	private boolean ignoreInvalidLocale = false;

	private boolean languageTagCompliant = false;


	/**
	 * 设置区域设置更改请求中包含区域设置规范的参数的名称. 默认为"locale".
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * 返回区域设置更改请求中包含区域设置规范的参数的名称.
	 */
	public String getParamName() {
		return this.paramName;
	}

	/**
	 * 配置可以更改语言环境的HTTP方法.
	 * 
	 * @param httpMethods 方法
	 */
	public void setHttpMethods(String... httpMethods) {
		this.httpMethods = httpMethods;
	}

	/**
	 * 返回配置的HTTP方法.
	 */
	public String[] getHttpMethods() {
		return this.httpMethods;
	}

	/**
	 * 设置是否忽略locale参数的无效值.
	 */
	public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
		this.ignoreInvalidLocale = ignoreInvalidLocale;
	}

	/**
	 * 返回是否忽略locale参数的无效值.
	 */
	public boolean isIgnoreInvalidLocale() {
		return this.ignoreInvalidLocale;
	}

	/**
	 * 指定是否将请求参数值解析为BCP 47语言标记, 而不是Java的旧区域设置规范格式.
	 * 默认为{@code false}.
	 * <p>Note: 此模式需要JDK 7或更高版本. 设置为{@code true}, 仅适用于JDK 7+上的BCP 47合规性.
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * 返回是否使用BCP 47语言标记而不是Java旧的区域设置规范格式.
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			if (checkHttpMethod(request.getMethod())) {
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					throw new IllegalStateException(
							"No LocaleResolver found: not in a DispatcherServlet request?");
				}
				try {
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
				}
				catch (IllegalArgumentException ex) {
					if (isIgnoreInvalidLocale()) {
						logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
					}
					else {
						throw ex;
					}
				}
			}
		}
		// Proceed in any case.
		return true;
	}

	private boolean checkHttpMethod(String currentMethod) {
		String[] configuredMethods = getHttpMethods();
		if (ObjectUtils.isEmpty(configuredMethods)) {
			return true;
		}
		for (String configuredMethod : configuredMethods) {
			if (configuredMethod.equalsIgnoreCase(currentMethod)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 解析来自请求参数的给定的语言环境值.
	 * <p>默认实现调用{@link StringUtils#parseLocaleString(String)}或JDK 7的{@link Locale#forLanguageTag(String)},
	 * 具体取决于{@link #setLanguageTagCompliant "languageTagCompliant"}配置属性.
	 * 
	 * @param locale 要解析的语言环境值
	 * 
	 * @return 相应的{@code Locale}实例
	 */
	@UsesJava7
	protected Locale parseLocaleValue(String locale) {
		return (isLanguageTagCompliant() ? Locale.forLanguageTag(locale) : StringUtils.parseLocaleString(locale));
	}

}
