package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;

/**
 * 用于轻松访问特定于请求的状态的工具类, 该状态由{@link org.springframework.web.servlet.DispatcherServlet}设置.
 *
 * <p>支持查找当前的WebApplicationContext, LocaleResolver, Locale, ThemeResolver, Theme, 和MultipartResolver.
 */
public abstract class RequestContextUtils {

	/**
	 * 配置用于在{@link RequestDataValueProcessor}的实现中查找bean的名称.
	 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";


	/**
	 * 查找与已启动请求处理的DispatcherServlet关联的WebApplicationContext.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 特定于请求的Web应用程序上下文
	 * @throws IllegalStateException 如果没有找到特定于servlet的上下文
	 * @deprecated as of Spring 4.2.1, in favor of
	 * {@link #findWebApplicationContext(HttpServletRequest)}
	 */
	@Deprecated
	public static WebApplicationContext getWebApplicationContext(ServletRequest request) throws IllegalStateException {
		return getWebApplicationContext(request, null);
	}

	/**
	 * 查找与已启动请求处理的DispatcherServlet关联的WebApplicationContext,
	 * 如果未找到与当前请求关联的上下文, 则查找全局上下文.
	 * 此方法可用于允许框架外部的组件(如JSP标记处理器)访问可用的最具体的应用程序上下文.
	 * 
	 * @param request 当前的HTTP请求
	 * @param servletContext 当前的servlet上下文
	 * 
	 * @return 特定于请求的WebApplicationContext, 如果未找到特定于请求的上下文, 则为全局的WebApplicationContext
	 * @throws IllegalStateException 如果既没有找到特定于servlet的上下文, 也没有找到全局上下文
	 * @deprecated as of Spring 4.2.1, in favor of
	 * {@link #findWebApplicationContext(HttpServletRequest, ServletContext)}
	 */
	@Deprecated
	public static WebApplicationContext getWebApplicationContext(
			ServletRequest request, ServletContext servletContext) throws IllegalStateException {

		WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			if (servletContext == null) {
				throw new IllegalStateException("No WebApplicationContext found: not in a DispatcherServlet request?");
			}
			webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		}
		return webApplicationContext;
	}

	/**
	 * 查找与已启动请求处理的DispatcherServlet关联的WebApplicationContext,
	 * 如果未找到与当前请求关联的上下文, 则查找全局上下文.
	 * 可以通过ServletContext或ContextLoader的当前上下文找到全局上下文.
	 * <p>NOTE: 此变体与Servlet 2.5保持兼容, 显式检查给定的ServletContext, 而不是从请求中派生它.
	 * 
	 * @param request 当前的HTTP请求
	 * @param servletContext 当前的servlet上下文
	 * 
	 * @return 特定于请求的WebApplicationContext;
	 * 如果没有找到特定于请求的上下文, 则为全局的WebApplicationContext; 如果没有, 则为{@code null}
	 */
	public static WebApplicationContext findWebApplicationContext(
			HttpServletRequest request, ServletContext servletContext) {

		WebApplicationContext webApplicationContext = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (webApplicationContext == null) {
			if (servletContext != null) {
				webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			}
			if (webApplicationContext == null) {
				webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
			}
		}
		return webApplicationContext;
	}

	/**
	 * 查找与已启动请求处理的DispatcherServlet关联的WebApplicationContext,
	 * 如果未找到与当前请求关联的上下文, 则查找全局上下文.
	 * 可以通过ServletContext或ContextLoader的当前上下文找到全局上下文.
	 * <p>NOTE: 此变体需要Servlet 3.0+, 通常建议用于具有前瞻性的自定义用户代码.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 特定于请求的WebApplicationContext;
	 * 如果没有找到特定于请求的上下文, 则为全局的WebApplicationContext; 如果没有, 则为{@code null}
	 */
	public static WebApplicationContext findWebApplicationContext(HttpServletRequest request) {
		return findWebApplicationContext(request, request.getServletContext());
	}

	/**
	 * 返回已由DispatcherServlet绑定到请求的LocaleResolver.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 当前的LocaleResolver, 或{@code null}
	 */
	public static LocaleResolver getLocaleResolver(HttpServletRequest request) {
		return (LocaleResolver) request.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 使用由DispatcherServlet绑定到请求的LocaleResolver, 从给定请求中检索当前语言环境, 并回退到请求的accept-header Locale.
	 * <p>此方法可作为标准Servlet {@link javax.servlet.http.HttpServletRequest#getLocale()}方法的直接替代方法,
	 * 如果找不到更具体的语言环境, 则返回后者.
	 * <p>考虑使用{@link org.springframework.context.i18n.LocaleContextHolder#getLocale()}, 它通常会使用相同的Locale填充.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 给定请求的当前语言环境, 可以从LocaleResolver, 也可以从普通请求本身
	 */
	public static Locale getLocale(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		return (localeResolver != null ? localeResolver.resolveLocale(request) : request.getLocale());
	}

	/**
	 * 使用由DispatcherServlet绑定到请求的TimeZoneAwareLocaleResolver, 从给定请求中检索当前时区, 并回退到系统的默认时区.
	 * <p>Note: 如果无法为给定请求解析特定时区, 则此方法返回{@code null}.
	 * 这与{@link #getLocale}相反, 因为其总是可以回退到请求的accept-header语言环境.
	 * <p>考虑使用{@link org.springframework.context.i18n.LocaleContextHolder#getTimeZone()},
	 * 它通常使用相同的TimeZone填充:
	 * 如果LocaleResolver未提供特定时区 (而不是此方法的{@code null}), 该方法仅在回退到系统时区方面有所不同.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 给定请求的当前时区, 来自TimeZoneAwareLocaleResolver 或{@code null}
	 */
	public static TimeZone getTimeZone(HttpServletRequest request) {
		LocaleResolver localeResolver = getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				return ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		return null;
	}

	/**
	 * 返回已由DispatcherServlet绑定到请求的ThemeResolver.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 当前的ThemeResolver, 或{@code null}
	 */
	public static ThemeResolver getThemeResolver(HttpServletRequest request) {
		return (ThemeResolver) request.getAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE);
	}

	/**
	 * 返回已由DispatcherServlet绑定到请求的ThemeSource.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 当前的ThemeSource
	 */
	public static ThemeSource getThemeSource(HttpServletRequest request) {
		return (ThemeSource) request.getAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE);
	}

	/**
	 * 使用由DispatcherServlet绑定到请求的ThemeResolver和ThemeSource从给定请求中检索当前主题.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 当前的主题, 或{@code null}
	 */
	public static Theme getTheme(HttpServletRequest request) {
		ThemeResolver themeResolver = getThemeResolver(request);
		ThemeSource themeSource = getThemeSource(request);
		if (themeResolver != null && themeSource != null) {
			String themeName = themeResolver.resolveThemeName(request);
			return themeSource.getTheme(themeName);
		}
		else {
			return null;
		}
	}

	/**
	 * 返回只读{@link Map}, 其中包含先前请求中保存的"input" flash属性.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return 只读的Map, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, ?> getInputFlashMap(HttpServletRequest request) {
		return (Map<String, ?>) request.getAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回带有属性的"output" FlashMap以保存后续请求.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return {@link FlashMap}实例 (在DispatcherServlet请求中从不为{@code null})
	 */
	public static FlashMap getOutputFlashMap(HttpServletRequest request) {
		return (FlashMap) request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
	}

	/**
	 * 返回FlashMapManager实例以在重定向之前保存Flash属性.
	 * 
	 * @param request 当前的请求
	 * 
	 * @return {@link FlashMapManager}实例 (在DispatcherServlet请求中从不为{@code null})
	 */
	public static FlashMapManager getFlashMapManager(HttpServletRequest request) {
		return (FlashMapManager) request.getAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE);
	}
}
