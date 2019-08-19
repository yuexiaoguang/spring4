package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * 特定于请求的状态的上下文保存器, 如当前Web应用程序上下文, 当前区域设置, 当前主题和潜在的绑定错误.
 * 提供对本地化消息和Errors实例的轻松访问.
 *
 * <p>适合于在JSP的"useBean"标签, JSP scriptlets, JSTL EL等中展示视图和用法.
 * 对于无法访问servlet请求的视图, 例如FreeMarker模板, 这是必需的.
 *
 * <p>可以手动实例化, 也可以通过AbstractView的"requestContextAttribute"属性自动将视图作为模型属性公开.
 *
 * <p>也将在DispatcherServlet请求之外工作, 访问根WebApplicationContext并使用适当的语言环境回退 (HttpServletRequest的主要语言环境).
 */
public class RequestContext {

	/**
	 * 如果RequestContext找不到ThemeResolver, 使用的默认主题名称.
	 * 仅适用于非DispatcherServlet请求.
	 * <p>与AbstractThemeResolver的默认值相同, 但此处未链接以避免包相互依赖性.
	 */
	public static final String DEFAULT_THEME_NAME = "theme";

	/**
	 * Request属性, 用于保存RequestContext用法的当前Web应用程序上下文.
	 * 默认公开DispatcherServlet的上下文 (或根上下文作为后备).
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = RequestContext.class.getName() + ".CONTEXT";


	protected static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", RequestContext.class.getClassLoader());

	private HttpServletRequest request;

	private HttpServletResponse response;

	private Map<String, Object> model;

	private WebApplicationContext webApplicationContext;

	private Locale locale;

	private TimeZone timeZone;

	private Theme theme;

	private Boolean defaultHtmlEscape;

	private Boolean responseEncodedHtmlEscape;

	private UrlPathHelper urlPathHelper;

	private RequestDataValueProcessor requestDataValueProcessor;

	private Map<String, Errors> errorsMap;


	/**
	 * 使用Errors检索的请求属性为给定请求创建新的RequestContext.
	 * <p>这仅适用于InternalResourceViews, 因为Errors实例是模型的一部分, 通常不作为请求属性公开.
	 * 它通常用于JSP或自定义标记中.
	 * <p><b>只能在DispatcherServlet请求中使用.</b>
	 * 传入ServletContext以便能够回退到根WebApplicationContext.
	 * 
	 * @param request 当前的HTTP请求
	 */
	public RequestContext(HttpServletRequest request) {
		initContext(request, null, null, null);
	}

	/**
	 * 使用Errors检索的请求属性为给定请求创建新的RequestContext.
	 * <p>这仅适用于InternalResourceViews, 因为Errors实例是模型的一部分, 通常不作为请求属性公开.
	 * 它通常用于JSP或自定义标记中.
	 * <p><b>只能在DispatcherServlet请求中使用.</b>
	 * 传入ServletContext以便能够回退到根WebApplicationContext.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response) {
		initContext(request, response, null, null);
	}

	/**
	 * 使用给定的模型属性进行Errors检索, 为给定请求创建新的RequestContext.
	 * <p>这仅适用于InternalResourceViews, 因为Errors实例是模型的一部分, 通常不作为请求属性公开.
	 * 它通常用于JSP或自定义标记中.
	 * <p>如果指定了ServletContext, 则RequestContext也将与根WebApplicationContext一起使用 (在DispatcherServlet之外).
	 * 
	 * @param request 当前的HTTP请求
	 * @param servletContext Web应用程序的servlet上下文 (可以是{@code null}; 是回退到根WebApplicationContext所必需的)
	 */
	public RequestContext(HttpServletRequest request, ServletContext servletContext) {
		initContext(request, null, servletContext, null);
	}

	/**
	 * 使用给定的模型属性进行Errors检索, 为给定请求创建新的RequestContext.
	 * <p>这适用于所有View实现. 它通常由View实现使用.
	 * <p><b>只能在DispatcherServlet请求中使用.</b>
	 * 传入ServletContext以便能够回退到根WebApplicationContext.
	 * 
	 * @param request 当前的HTTP请求
	 * @param model 当前视图的模型属性 (可以是{@code null}, 使用Errors检索的请求属性)
	 */
	public RequestContext(HttpServletRequest request, Map<String, Object> model) {
		initContext(request, null, null, model);
	}

	/**
	 * 使用给定的模型属性进行Errors检索, 为给定请求创建新的RequestContext.
	 * <p>这适用于所有View实现. 它通常由View实现使用.
	 * <p>如果指定了ServletContext, 则RequestContext也将与根WebApplicationContext一起使用 (在DispatcherServlet之外).
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param servletContext Web应用程序的servlet上下文 (可以是{@code null}; 是回退到根WebApplicationContext所必需的)
	 * @param model 当前视图的模型属性 (可以是{@code null}, 使用Errors检索的请求属性)
	 */
	public RequestContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext,
			Map<String, Object> model) {

		initContext(request, response, servletContext, model);
	}

	protected RequestContext() {
	}


	/**
	 * 使用给定的模型属性进行Errors检索, 使用给定的请求初始化此上下文.
	 * <p>如果在请求中找不到LocaleResolver和/或ThemeResolver,
	 * 则分别委托给{@code getFallbackLocale}和{@code getFallbackTheme}来确定回退区域设置和主题.
	 * 
	 * @param request 当前的HTTP请求
	 * @param servletContext Web应用程序的servlet上下文 (可以是{@code null}; 是回退到根WebApplicationContext所必需的)
	 * @param model 当前视图的模型属性 (可以是{@code null}, 使用Errors检索的请求属性)
	 */
	protected void initContext(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext,
			Map<String, Object> model) {

		this.request = request;
		this.response = response;
		this.model = model;

		// 从DispatcherServlet或根上下文中获取WebApplicationContext.
		// 需要指定ServletContext才能回退到根上下文!
		this.webApplicationContext = (WebApplicationContext) request.getAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (this.webApplicationContext == null) {
			this.webApplicationContext = RequestContextUtils.findWebApplicationContext(request, servletContext);
			if (this.webApplicationContext == null) {
				throw new IllegalStateException("No WebApplicationContext found: not in a DispatcherServlet " +
						"request and no ContextLoaderListener registered?");
			}
		}

		// 确定要用于此RequestContext的区域设置.
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContext localeContext = ((LocaleContextResolver) localeResolver).resolveLocaleContext(request);
			this.locale = localeContext.getLocale();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				this.timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			}
		}
		else if (localeResolver != null) {
			// 尝试LocaleResolver (在DispatcherServlet请求中).
			this.locale = localeResolver.resolveLocale(request);
		}

		// 如有必要, 尝试JSTL后备.
		if (this.locale == null) {
			this.locale = getFallbackLocale();
		}
		if (this.timeZone == null) {
			this.timeZone = getFallbackTimeZone();
		}

		// 从web.xml中的"defaultHtmlEscape" context-param 确定默认的HTML转义设置.
		this.defaultHtmlEscape = WebUtils.getDefaultHtmlEscape(this.webApplicationContext.getServletContext());

		// 从web.xml中的"responseEncodedHtmlEscape" context-param确定响应编码的HTML转义设置.
		this.responseEncodedHtmlEscape = WebUtils.getResponseEncodedHtmlEscape(this.webApplicationContext.getServletContext());

		this.urlPathHelper = new UrlPathHelper();

		if (this.webApplicationContext.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			this.requestDataValueProcessor = this.webApplicationContext.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
	}


	/**
	 * 返回底层的HttpServletRequest. 仅适用于此包中的协作类.
	 */
	protected final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 返回底层的ServletContext. 仅适用于此包中的协作类.
	 */
	protected final ServletContext getServletContext() {
		return this.webApplicationContext.getServletContext();
	}

	/**
	 * 返回当前的WebApplicationContext.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}

	/**
	 * 返回当前的WebApplicationContext.
	 */
	public final MessageSource getMessageSource() {
		return this.webApplicationContext;
	}

	/**
	 * 返回此RequestContext封装的模型Map.
	 * 
	 * @return 填充的模型Map, 或{@code null}
	 */
	public final Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * 返回当前的Locale (回退到请求语言环境; never {@code null}).
	 * <p>通常来自DispatcherServlet的{@link LocaleResolver}.
	 * 还包括对JSTL的Locale属性的回退检查.
	 */
	public final Locale getLocale() {
		return this.locale;
	}

	/**
	 * 返回当前TimeZone (如果无法从请求中派生, 则返回{@code null}).
	 * <p>通常来自DispatcherServlet的{@link LocaleContextResolver}.
	 * 还包括JSTL的TimeZone属性的后备检查.
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * 确定此上下文的回退区域设置.
	 * <p>默认实现检查request, session 或 application范围中的JSTL语言环境属性;
	 * 如果找不到, 则返回{@code HttpServletRequest.getLocale()}.
	 * 
	 * @return 后备区域设置 (never {@code null})
	 */
	protected Locale getFallbackLocale() {
		if (jstlPresent) {
			Locale locale = JstlLocaleResolver.getJstlLocale(getRequest(), getServletContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * 确定此上下文的回退时区.
	 * <p>默认实现检查request, session 或 application范围中的JSTL时区属性; 或{@code null}.
	 * 
	 * @return 回退时区 (如果无法从请求中派生, 则为{@code null})
	 */
	protected TimeZone getFallbackTimeZone() {
		if (jstlPresent) {
			TimeZone timeZone = JstlLocaleResolver.getJstlTimeZone(getRequest(), getServletContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}

	/**
	 * 将当前语言环境更改为指定的语言环境, 通过配置的{@link LocaleResolver}存储新语言环境.
	 * 
	 * @param locale 新的区域设置
	 */
	public void changeLocale(Locale locale) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (localeResolver == null) {
			throw new IllegalStateException("Cannot change locale if no LocaleResolver configured");
		}
		localeResolver.setLocale(this.request, this.response, locale);
		this.locale = locale;
	}

	/**
	 * 将当前语言环境更改为指定的语言环境和时区上下文, 通过配置的{@link LocaleResolver}存储新的语言环境上下文.
	 * 
	 * @param locale 新的语言环境
	 * @param timeZone 新的时区
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(this.request);
		if (!(localeResolver instanceof LocaleContextResolver)) {
			throw new IllegalStateException("Cannot change locale context if no LocaleContextResolver configured");
		}
		((LocaleContextResolver) localeResolver).setLocaleContext(this.request, this.response,
				new SimpleTimeZoneAwareLocaleContext(locale, timeZone));
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * 返回当前主题 (never {@code null}).
	 * <p>在没有使用主题支持时, 延迟解析以提高效率.
	 */
	public Theme getTheme() {
		if (this.theme == null) {
			// 延迟确定用于此RequestContext的主题.
			this.theme = RequestContextUtils.getTheme(this.request);
			if (this.theme == null) {
				// 没有ThemeResolver和ThemeSource可用 -> 尝试回退.
				this.theme = getFallbackTheme();
			}
		}
		return this.theme;
	}

	/**
	 * 确定此上下文的回退主题.
	 * <p>默认实现返回默认主题 (名称为"theme").
	 * 
	 * @return 回退主题 (never {@code null})
	 */
	protected Theme getFallbackTheme() {
		ThemeSource themeSource = RequestContextUtils.getThemeSource(getRequest());
		if (themeSource == null) {
			themeSource = new ResourceBundleThemeSource();
		}
		Theme theme = themeSource.getTheme(DEFAULT_THEME_NAME);
		if (theme == null) {
			throw new IllegalStateException("No theme defined and no fallback theme found");
		}
		return theme;
	}

	/**
	 * 将当前主题更改为指定的主题, 通过配置的{@link ThemeResolver}存储新的主题名称.
	 * 
	 * @param theme 新的主题
	 */
	public void changeTheme(Theme theme) {
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);
		if (themeResolver == null) {
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}
		themeResolver.setThemeName(this.request, this.response, (theme != null ? theme.getName() : null));
		this.theme = theme;
	}

	/**
	 * 按名称将当前主题更改为指定主题, 通过配置的{@link ThemeResolver}存储新的主题名称.
	 * 
	 * @param themeName 新的主题名称
	 */
	public void changeTheme(String themeName) {
		ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(this.request);
		if (themeResolver == null) {
			throw new IllegalStateException("Cannot change theme if no ThemeResolver configured");
		}
		themeResolver.setThemeName(this.request, this.response, themeName);
		// 要求在下一个getTheme调用上重新解析.
		this.theme = null;
	}

	/**
	 * 为此RequestContext的范围激活(取消激活)消息和错误的默认HTML转义.
	 * <p>默认是应用程序范围的设置 (web.xml中的"defaultHtmlEscape" context-param).
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * 默认的HTML转义是否激活? 如果没有给出明确的默认值, 则回退到{@code false}.
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * 返回默认的HTML转义设置, 区分未指定的默认值和显式值.
	 * 
	 * @return 是否启用了默认的HTML转义 (null = 没有显式默认值)
	 */
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * 默认情况下, HTML转义是否使用响应编码?
	 * 如果启用，则只有XML标记重要字符将使用 UTF-* 编码进行转义.
	 * <p>从Spring 4.2开始, 如果没有给出明确的默认值, 则回退到{@code true}.
	 */
	public boolean isResponseEncodedHtmlEscape() {
		return (this.responseEncodedHtmlEscape == null || this.responseEncodedHtmlEscape.booleanValue());
	}

	/**
	 * 返回有关HTML转义设置使用响应编码的默认设置, 区分未指定的默认值和显式值.
	 * 
	 * @return 是否默认使用响应编码HTML转义 (null = 无显式默认值)
	 */
	public Boolean getResponseEncodedHtmlEscape() {
		return this.responseEncodedHtmlEscape;
	}


	/**
	 * 设置用于上下文路径和请求URI解码的UrlPathHelper.
	 * 可用于传递共享的UrlPathHelper实例.
	 * <p>默认的UrlPathHelper始终可用.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回用于上下文路径和请求URI解码的UrlPathHelper.
	 * 可用于配置当前的UrlPathHelper.
	 * <p>默认的UrlPathHelper始终可用.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 返回从名称为{@code "requestDataValueProcessor"}的WebApplicationContext获取的RequestDataValueProcessor实例.
	 * 如果找不到匹配的bean, 则为{@code null}.
	 */
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.requestDataValueProcessor;
	}

	/**
	 * 返回原始请求的上下文路径, 即指示当前Web应用程序的路径.
	 * 这对于构建应用程序中其他资源的链接很有用.
	 * <p>委托给UrlPathHelper进行解码.
	 */
	public String getContextPath() {
		return this.urlPathHelper.getOriginatingContextPath(this.request);
	}

	/**
	 * 返回给定相对URL的上下文感知URl.
	 * 
	 * @param relativeUrl 相对URL部分
	 * 
	 * @return 指向具有绝对路径的服务器的URL (也相应地进行URL编码)
	 */
	public String getContextUrl(String relativeUrl) {
		String url = getContextPath() + relativeUrl;
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * 返回具有占位符的 (带括号 {@code {}}的命名键)给定相对URL的上下文感知URl.
	 * 例如, 发送相对URL {@code foo/{bar}?spam={spam}} 和参数map {@code {bar=baz,spam=nuts}},
	 * 结果将是{@code [contextpath]/foo/baz?spam=nuts}.
	 * 
	 * @param relativeUrl 相对URL部分
	 * @param params 要在URL中插入占位符的参数Map
	 * 
	 * @return 指向具有绝对路径的服务器的URL (也相应地进行URL编码)
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = getContextPath() + relativeUrl;
		UriTemplate template = new UriTemplate(url);
		url = template.expand(params).toASCIIString();
		if (this.response != null) {
			url = this.response.encodeURL(url);
		}
		return url;
	}

	/**
	 * 返回当前servlet中URL映射的路径, 包括原始请求的上下文路径和servlet路径.
	 * 这对于构建应用程序中其他资源的链接很有用, 其中使用{@code "/main/*"}样式的servlet映射.
	 * <p>委托给UrlPathHelper确定上下文和servlet路径.
	 */
	public String getPathToServlet() {
		String path = this.urlPathHelper.getOriginatingContextPath(this.request);
		if (StringUtils.hasText(this.urlPathHelper.getPathWithinServletMapping(this.request))) {
			path += this.urlPathHelper.getOriginatingServletPath(this.request);
		}
		return path;
	}

	/**
	 * 返回原始请求的请求URI，即不带参数的调用URL. 这对于HTML表单操作目标特别有用, 可能与原始查询字符串结合使用.
	 * <p>委托给UrlPathHelper进行解码.
	 */
	public String getRequestUri() {
		return this.urlPathHelper.getOriginatingRequestUri(this.request);
	}

	/**
	 * 返回当前请求的查询字符串, 即请求路径后的部分.
	 * 这对于与原始请求URI一起构建HTML表单操作目标特别有用.
	 * <p>委托给UrlPathHelper进行解码.
	 */
	public String getQueryString() {
		return this.urlPathHelper.getOriginatingQueryString(this.request);
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * @param htmlEscape 是否HTML转义消息?
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, Object[] args, String defaultMessage, boolean htmlEscape) {
		String msg = this.webApplicationContext.getMessage(code, args, defaultMessage, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param htmlEscape 是否HTML转义消息?
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(code, args, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定的MessageSourceResolvable (e.g. ObjectError实例).
	 * 
	 * @param resolvable the MessageSourceResolvable
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定的MessageSourceResolvable (e.g. ObjectError实例).
	 * 
	 * @param resolvable the MessageSourceResolvable
	 * @param htmlEscape 是否HTML转义消息?
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.webApplicationContext.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getThemeMessage(String code, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, null, defaultMessage, this.locale);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getThemeMessage(String code, Object[] args, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, args, defaultMessage, this.locale);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 返回的字符串
	 * 
	 * @return 消息
	 */
	public String getThemeMessage(String code, List<?> args, String defaultMessage) {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), defaultMessage,
				this.locale);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, null, this.locale);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code, Object[] args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, args, this.locale);
	}

	/**
	 * 检索给定代码的主题消息.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param code 消息的代码
	 * @param args 消息的参数, 或{@code null}
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(String code, List<?> args) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(code, (args != null ? args.toArray() : null), this.locale);
	}

	/**
	 * 检索当前主题中的给定MessageSourceResolvable.
	 * <p>请注意, 主题消息永远不会被HTML转义, 因为它们通常表示特定于主题的资源路径, 而不是客户端可见的消息.
	 * 
	 * @param resolvable the MessageSourceResolvable
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getThemeMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getTheme().getMessageSource().getMessage(resolvable, this.locale);
	}

	/**
	 * 使用"defaultHtmlEscape"设置检索给定绑定对象的Errors实例.
	 * 
	 * @param name 绑定对象的名称
	 * 
	 * @return Errors实例, 或{@code null}
	 */
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定绑定对象的Errors实例.
	 * 
	 * @param name 绑定对象的名称
	 * @param htmlEscape 是否使用自动HTML转义创建一个Errors实例?
	 * 
	 * @return Errors实例, 或{@code null}
	 */
	public Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<String, Errors>();
		}
		Errors errors = this.errorsMap.get(name);
		boolean put = false;
		if (errors == null) {
			errors = (Errors) getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			// 检查旧的BindException前缀以获得向后兼容性.
			if (errors instanceof BindException) {
				errors = ((BindException) errors).getBindingResult();
			}
			if (errors == null) {
				return null;
			}
			put = true;
		}
		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
			put = true;
		}
		else if (!htmlEscape && errors instanceof EscapedErrors) {
			errors = ((EscapedErrors) errors).getSource();
			put = true;
		}
		if (put) {
			this.errorsMap.put(name, errors);
		}
		return errors;
	}

	/**
	 * 从模型或请求属性中检索给定模型名称的模型对象.
	 * 
	 * @param modelName 模型对象的名称
	 * 
	 * @return 模型对象
	 */
	protected Object getModelObject(String modelName) {
		if (this.model != null) {
			return this.model.get(modelName);
		}
		else {
			return this.request.getAttribute(modelName);
		}
	}

	/**
	 * 使用"defaultHtmlEscape"设置为给定的绑定对象创建BindStatus.
	 * 
	 * @param path 将为其解析值和错误的bean和属性路径 (e.g. "person.age")
	 * 
	 * @return BindStatus实例
	 * @throws IllegalStateException 如果没有找到相应的Errors对象
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * 使用"defaultHtmlEscape"设置为给定的绑定对象创建BindStatus.
	 * 
	 * @param path 将为其解析值和错误的bean和属性路径 (e.g. "person.age")
	 * @param htmlEscape 是否使用自动HTML转义创建BindStatus?
	 * 
	 * @return BindStatus实例
	 * @throws IllegalStateException 如果没有找到相应的Errors对象
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}


	/**
	 * 隔离JSTL依赖项的内部类.
	 * 如果存在JSTL API, 则调用以解析回退区域设置.
	 */
	private static class JstlLocaleResolver {

		public static Locale getJstlLocale(HttpServletRequest request, ServletContext servletContext) {
			Object localeObject = Config.get(request, Config.FMT_LOCALE);
			if (localeObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					localeObject = Config.get(session, Config.FMT_LOCALE);
				}
				if (localeObject == null && servletContext != null) {
					localeObject = Config.get(servletContext, Config.FMT_LOCALE);
				}
			}
			return (localeObject instanceof Locale ? (Locale) localeObject : null);
		}

		public static TimeZone getJstlTimeZone(HttpServletRequest request, ServletContext servletContext) {
			Object timeZoneObject = Config.get(request, Config.FMT_TIME_ZONE);
			if (timeZoneObject == null) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					timeZoneObject = Config.get(session, Config.FMT_TIME_ZONE);
				}
				if (timeZoneObject == null && servletContext != null) {
					timeZoneObject = Config.get(servletContext, Config.FMT_TIME_ZONE);
				}
			}
			return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
		}
	}
}
