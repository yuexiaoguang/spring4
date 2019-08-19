package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 用于准备JSTL视图的Helper类, 特别是用于公开JSTL本地化上下文.
 */
public abstract class JstlUtils {

	/**
	 * 检查JSTL的"javax.servlet.jsp.jstl.fmt.localizationContext" context-param,
	 * 并创建一个相应的子消息源, 并将提供的Spring定义的MessageSource作为父级.
	 * 
	 * @param servletContext 正在运行的ServletContext (用于检查{@code web.xml}中与JSTL相关的上下文参数)
	 * @param messageSource 要公开的MessageSource, 通常是当前DispatcherServlet的ApplicationContext
	 * 
	 * @return 要向JSTL公开的MessageSource; 首先检查JSTL定义的bundle, 然后检查Spring定义的MessageSource
	 */
	public static MessageSource getJstlAwareMessageSource(
			ServletContext servletContext, MessageSource messageSource) {

		if (servletContext != null) {
			String jstlInitParam = servletContext.getInitParameter(Config.FMT_LOCALIZATION_CONTEXT);
			if (jstlInitParam != null) {
				// 在web.xml中的JSTL context-param中为指定的资源包basename创建ResourceBundleMessageSource,
				// 并将其与给定的Spring定义的MessageSource连接为父级.
				ResourceBundleMessageSource jstlBundleWrapper = new ResourceBundleMessageSource();
				jstlBundleWrapper.setBasename(jstlInitParam);
				jstlBundleWrapper.setParentMessageSource(messageSource);
				return jstlBundleWrapper;
			}
		}
		return messageSource;
	}

	/**
	 * 使用Spring的语言环境和MessageSource公开JSTL特定的请求属性, 为JSTL的格式和消息标记指定语言环境和资源包.
	 * 
	 * @param request 当前的HTTP请求
	 * @param messageSource 要公开的MessageSource, 通常是当前的ApplicationContext (may be {@code null})
	 */
	public static void exposeLocalizationContext(HttpServletRequest request, MessageSource messageSource) {
		Locale jstlLocale = RequestContextUtils.getLocale(request);
		Config.set(request, Config.FMT_LOCALE, jstlLocale);
		TimeZone timeZone = RequestContextUtils.getTimeZone(request);
		if (timeZone != null) {
			Config.set(request, Config.FMT_TIME_ZONE, timeZone);
		}
		if (messageSource != null) {
			LocalizationContext jstlContext = new SpringLocalizationContext(messageSource, request);
			Config.set(request, Config.FMT_LOCALIZATION_CONTEXT, jstlContext);
		}
	}

	/**
	 * 使用Spring的语言环境和MessageSource公开JSTL特定的请求属性, 为JSTL的格式和消息标记指定语言环境和资源包.
	 * 
	 * @param requestContext 当前HTTP请求的上下文, 包括作为MessageSource公开的ApplicationContext
	 */
	public static void exposeLocalizationContext(RequestContext requestContext) {
		Config.set(requestContext.getRequest(), Config.FMT_LOCALE, requestContext.getLocale());
		TimeZone timeZone = requestContext.getTimeZone();
		if (timeZone != null) {
			Config.set(requestContext.getRequest(), Config.FMT_TIME_ZONE, timeZone);
		}
		MessageSource messageSource = getJstlAwareMessageSource(
				requestContext.getServletContext(), requestContext.getMessageSource());
		LocalizationContext jstlContext = new SpringLocalizationContext(messageSource, requestContext.getRequest());
		Config.set(requestContext.getRequest(), Config.FMT_LOCALIZATION_CONTEXT, jstlContext);
	}


	/**
	 * 特定于Spring的LocalizationContext适配器, 它将会话范围的 JSTL LocalizationContext/Locale属性与本地Spring请求上下文合并.
	 */
	private static class SpringLocalizationContext extends LocalizationContext {

		private final MessageSource messageSource;

		private final HttpServletRequest request;

		public SpringLocalizationContext(MessageSource messageSource, HttpServletRequest request) {
			this.messageSource = messageSource;
			this.request = request;
		}

		@Override
		public ResourceBundle getResourceBundle() {
			HttpSession session = this.request.getSession(false);
			if (session != null) {
				Object lcObject = Config.get(session, Config.FMT_LOCALIZATION_CONTEXT);
				if (lcObject instanceof LocalizationContext) {
					ResourceBundle lcBundle = ((LocalizationContext) lcObject).getResourceBundle();
					return new MessageSourceResourceBundle(this.messageSource, getLocale(), lcBundle);
				}
			}
			return new MessageSourceResourceBundle(this.messageSource, getLocale());
		}

		@Override
		public Locale getLocale() {
			HttpSession session = this.request.getSession(false);
			if (session != null) {
				Object localeObject = Config.get(session, Config.FMT_LOCALE);
				if (localeObject instanceof Locale) {
					return (Locale) localeObject;
				}
			}
			return RequestContextUtils.getLocale(this.request);
		}
	}

}
