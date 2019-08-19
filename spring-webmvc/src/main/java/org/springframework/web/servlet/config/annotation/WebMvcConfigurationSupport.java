package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.util.UrlPathHelper;

/**
 * 这是提供MVC Java配置之外的配置的主类.
 * 通常通过将{@link EnableWebMvc @EnableWebMvc}添加到应用程序{@link Configuration @Configuration}类来导入它.
 * 另一个更高级的选项是直接从这个类扩展并根据需要覆盖方法,
 * 记住将{@link Configuration @Configuration}添加到子类和{@link Bean @Bean}以覆盖{@link Bean @Bean}方法.
 * 有关更多详细信息, 请参阅{@link EnableWebMvc @EnableWebMvc}的javadoc.
 *
 * <p>这个类注册了以下{@link HandlerMapping HandlerMappings}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}在0处排序, 用于将请求映射到带注解的控制器方法.
 * <li>{@link HandlerMapping}在1处排序, 将URL路径直接映射到视图名称.
 * <li>{@link BeanNameUrlHandlerMapping}在2处排序, 将URL路径映射到控制器bean名称.
 * <li>{@link HandlerMapping}在{@code Integer.MAX_VALUE-1}处排序, 提供静态资源请求.
 * <li>{@link HandlerMapping}在{@code Integer.MAX_VALUE}处排序, 将请求转发到默认servlet.
 * </ul>
 *
 * <p>注册这些{@link HandlerAdapter HandlerAdapters}:
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}用于使用带注解的控制器方法处理请求.
 * <li>{@link HttpRequestHandlerAdapter}用于使用{@link HttpRequestHandler HttpRequestHandlers}处理请求.
 * <li>{@link SimpleControllerHandlerAdapter}用于处理基于接口的{@link Controller Controllers}请求.
 * </ul>
 *
 * <p>使用此异常解析器链注册{@link HandlerExceptionResolverComposite}:
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver}用于通过
 * {@link org.springframework.web.bind.annotation.ExceptionHandler}方法处理异常.
 * <li>{@link ResponseStatusExceptionResolver}用于
 * 带{@link org.springframework.web.bind.annotation.ResponseStatus}注解的异常.
 * <li>{@link DefaultHandlerExceptionResolver}用于解析已知的Spring异常类型
 * </ul>
 *
 * <p>注册要使用的{@link AntPathMatcher}和{@link UrlPathHelper}:
 * <ul>
 * <li>{@link RequestMappingHandlerMapping},
 * <li>{@link HandlerMapping}用于ViewController
 * <li>{@link HandlerMapping}用于服务资源
 * </ul>
 * 请注意, 可以使用{@link PathMatchConfigurer}配置这些bean.
 *
 * <p>默认情况下, {@link RequestMappingHandlerAdapter}
 * 和{@link ExceptionHandlerExceptionResolver}都配置了以下默认实例:
 * <ul>
 * <li>{@link ContentNegotiationManager}
 * <li>{@link DefaultFormattingConversionService}
 * <li>{@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean},
 * 如果类路径上有JSR-303实现可用
 * <li>一系列{@link HttpMessageConverter HttpMessageConverters}, 取决于类路径上可用的第三方库.
 * </ul>
 */
public class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	private static final boolean romePresent =
			ClassUtils.isPresent("com.rometools.rome.feed.WireFeed",
					WebMvcConfigurationSupport.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					WebMvcConfigurationSupport.class.getClassLoader());

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					WebMvcConfigurationSupport.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					WebMvcConfigurationSupport.class.getClassLoader());

	private static final boolean jackson2XmlPresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper",
					WebMvcConfigurationSupport.class.getClassLoader());

	private static final boolean gsonPresent =
			ClassUtils.isPresent("com.google.gson.Gson",
					WebMvcConfigurationSupport.class.getClassLoader());


	private ApplicationContext applicationContext;

	private ServletContext servletContext;

	private List<Object> interceptors;

	private PathMatchConfigurer pathMatchConfigurer;

	private ContentNegotiationManager contentNegotiationManager;

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	private List<HttpMessageConverter<?>> messageConverters;

	private Map<String, CorsConfiguration> corsConfigurations;


	/**
	 * 设置Spring {@link ApplicationContext}, e.g. 用于资源加载.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回关联的Spring {@link ApplicationContext}.
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 设置{@link javax.servlet.ServletContext}, e.g. 用于资源处理, 查找文件扩展名等.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 返回关联的{@link javax.servlet.ServletContext}.
	 */
	public ServletContext getServletContext() {
		return this.servletContext;
	}


	/**
	 * 返回顺序值为0的{@link RequestMappingHandlerMapping}, 用于将请求映射到带注解的控制器.
	 */
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();
		mapping.setOrder(0);
		mapping.setInterceptors(getInterceptors());
		mapping.setContentNegotiationManager(mvcContentNegotiationManager());
		mapping.setCorsConfigurations(getCorsConfigurations());

		PathMatchConfigurer configurer = getPathMatchConfigurer();

		Boolean useSuffixPatternMatch = configurer.isUseSuffixPatternMatch();
		if (useSuffixPatternMatch != null) {
			mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
		}
		Boolean useRegisteredSuffixPatternMatch = configurer.isUseRegisteredSuffixPatternMatch();
		if (useRegisteredSuffixPatternMatch != null) {
			mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);
		}
		Boolean useTrailingSlashMatch = configurer.isUseTrailingSlashMatch();
		if (useTrailingSlashMatch != null) {
			mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);
		}

		UrlPathHelper pathHelper = configurer.getUrlPathHelper();
		if (pathHelper != null) {
			mapping.setUrlPathHelper(pathHelper);
		}
		PathMatcher pathMatcher = configurer.getPathMatcher();
		if (pathMatcher != null) {
			mapping.setPathMatcher(pathMatcher);
		}

		return mapping;
	}

	/**
	 * 用于插入{@link RequestMappingHandlerMapping}的自定义子类.
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new RequestMappingHandlerMapping();
	}

	/**
	 * 提供对用于配置{@link HandlerMapping}实例的共享处理器拦截器的访问.
	 * <p>这个方法不能被覆盖; 改用{@link #addInterceptors}.
	 */
	protected final Object[] getInterceptors() {
		if (this.interceptors == null) {
			InterceptorRegistry registry = new InterceptorRegistry();
			addInterceptors(registry);
			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService()));
			registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider()));
			this.interceptors = registry.getInterceptors();
		}
		return this.interceptors.toArray();
	}

	/**
	 * 重写此方法以添加Spring MVC拦截器, 用于控制器调用的预处理和后处理.
	 */
	protected void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 构建{@link PathMatchConfigurer}的回调.
	 * 委托给{@link #configurePathMatch}.
	 */
	protected PathMatchConfigurer getPathMatchConfigurer() {
		if (this.pathMatchConfigurer == null) {
			this.pathMatchConfigurer = new PathMatchConfigurer();
			configurePathMatch(this.pathMatchConfigurer);
		}
		return this.pathMatchConfigurer;
	}

	/**
	 * 重写此方法以配置路径匹配选项.
	 */
	protected void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 返回全局{@link PathMatcher}实例, 用于{@link HandlerMapping HandlerMappings}中的路径匹配模式.
	 * 可以使用{@link #configurePathMatch(PathMatchConfigurer)}中的{@link PathMatchConfigurer}配置此实例.
	 */
	@Bean
	public PathMatcher mvcPathMatcher() {
		PathMatcher pathMatcher = getPathMatchConfigurer().getPathMatcher();
		return (pathMatcher != null ? pathMatcher : new AntPathMatcher());
	}

	/**
	 * 返回全局{@link UrlPathHelper}实例, 用于{@link HandlerMapping HandlerMappings}中的路径匹配模式.
	 * 可以使用{@link #configurePathMatch(PathMatchConfigurer)}中的{@link PathMatchConfigurer}配置此实例.
	 */
	@Bean
	public UrlPathHelper mvcUrlPathHelper() {
		UrlPathHelper pathHelper = getPathMatchConfigurer().getUrlPathHelper();
		return (pathHelper != null ? pathHelper : new UrlPathHelper());
	}

	/**
	 * 返回{@link ContentNegotiationManager}实例, 用于确定给定请求中所请求的{@linkplain MediaType 媒体类型}.
	 */
	@Bean
	public ContentNegotiationManager mvcContentNegotiationManager() {
		if (this.contentNegotiationManager == null) {
			ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer(this.servletContext);
			configurer.mediaTypes(getDefaultMediaTypes());
			configureContentNegotiation(configurer);
			this.contentNegotiationManager = configurer.buildContentNegotiationManager();
		}
		return this.contentNegotiationManager;
	}

	protected Map<String, MediaType> getDefaultMediaTypes() {
		Map<String, MediaType> map = new HashMap<String, MediaType>(4);
		if (romePresent) {
			map.put("atom", MediaType.APPLICATION_ATOM_XML);
			map.put("rss", MediaType.APPLICATION_RSS_XML);
		}
		if (jaxb2Present || jackson2XmlPresent) {
			map.put("xml", MediaType.APPLICATION_XML);
		}
		if (jackson2Present || gsonPresent) {
			map.put("json", MediaType.APPLICATION_JSON);
		}
		return map;
	}

	/**
	 * 重写此方法以配置内容协商.
	 */
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 返回在1处排序的处理器映射, 直接映射URL路径到视图名称.
	 * 要配置视图控制器, 覆盖{@link #addViewControllers}.
	 */
	@Bean
	public HandlerMapping viewControllerHandlerMapping() {
		ViewControllerRegistry registry = new ViewControllerRegistry(this.applicationContext);
		addViewControllers(registry);

		AbstractHandlerMapping handlerMapping = registry.buildHandlerMapping();
		handlerMapping = (handlerMapping != null ? handlerMapping : new EmptyHandlerMapping());
		handlerMapping.setPathMatcher(mvcPathMatcher());
		handlerMapping.setUrlPathHelper(mvcUrlPathHelper());
		handlerMapping.setInterceptors(getInterceptors());
		handlerMapping.setCorsConfigurations(getCorsConfigurations());
		return handlerMapping;
	}

	/**
	 * 重写此方法以添加视图控制器.
	 */
	protected void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * 返回在2处排序的{@link BeanNameUrlHandlerMapping}, 将URL路径映射到控制器bean名称.
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);
		mapping.setInterceptors(getInterceptors());
		mapping.setCorsConfigurations(getCorsConfigurations());
		return mapping;
	}

	/**
	 * 返回在 Integer.MAX_VALUE-1 处排序的处理器映射, 使用映射的资源处理器.
	 * 要配置资源处理, 请覆盖{@link #addResourceHandlers}.
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		Assert.state(this.servletContext != null, "No ServletContext set");

		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(this.applicationContext,
				this.servletContext, mvcContentNegotiationManager(), mvcUrlPathHelper());
		addResourceHandlers(registry);

		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		if (handlerMapping != null) {
			handlerMapping.setPathMatcher(mvcPathMatcher());
			handlerMapping.setUrlPathHelper(mvcUrlPathHelper());
			handlerMapping.setInterceptors(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider()));
			handlerMapping.setCorsConfigurations(getCorsConfigurations());
		}
		else {
			handlerMapping = new EmptyHandlerMapping();
		}
		return handlerMapping;
	}

	/**
	 * 重写此方法, 添加用于提供静态资源的资源处理器.
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 用于MVC调度器的{@link ResourceUrlProvider} bean.
	 */
	@Bean
	public ResourceUrlProvider mvcResourceUrlProvider() {
		ResourceUrlProvider urlProvider = new ResourceUrlProvider();
		UrlPathHelper pathHelper = getPathMatchConfigurer().getUrlPathHelper();
		if (pathHelper != null) {
			urlProvider.setUrlPathHelper(pathHelper);
		}
		PathMatcher pathMatcher = getPathMatchConfigurer().getPathMatcher();
		if (pathMatcher != null) {
			urlProvider.setPathMatcher(pathMatcher);
		}
		return urlProvider;
	}

	/**
	 * 返回在 Integer.MAX_VALUE 处排序的处理器映射, 使用映射的默认servlet处理器.
	 * 要配置"默认" Servlet处理, 请覆盖{@link #configureDefaultServletHandling}.
	 */
	@Bean
	public HandlerMapping defaultServletHandlerMapping() {
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(this.servletContext);
		configureDefaultServletHandling(configurer);

		HandlerMapping handlerMapping = configurer.buildHandlerMapping();
		return (handlerMapping != null ? handlerMapping : new EmptyHandlerMapping());
	}

	/**
	 * 重写此方法以配置"默认" Servlet处理.
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 返回{@link RequestMappingHandlerAdapter}, 用于通过带注解的控制器方法处理请求.
	 * 考虑重写其他一些更细粒度的方法:
	 * <ul>
	 * <li>{@link #addArgumentResolvers} 用于添加自定义参数解析器.
	 * <li>{@link #addReturnValueHandlers} 用于添加自定义返回值处理程序.
	 * <li>{@link #configureMessageConverters} 用于添加自定义消息转换器.
	 * </ul>
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();
		adapter.setContentNegotiationManager(mvcContentNegotiationManager());
		adapter.setMessageConverters(getMessageConverters());
		adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer());
		adapter.setCustomArgumentResolvers(getArgumentResolvers());
		adapter.setCustomReturnValueHandlers(getReturnValueHandlers());

		if (jackson2Present) {
			adapter.setRequestBodyAdvice(
					Collections.<RequestBodyAdvice>singletonList(new JsonViewRequestBodyAdvice()));
			adapter.setResponseBodyAdvice(
					Collections.<ResponseBodyAdvice<?>>singletonList(new JsonViewResponseBodyAdvice()));
		}

		AsyncSupportConfigurer configurer = new AsyncSupportConfigurer();
		configureAsyncSupport(configurer);
		if (configurer.getTaskExecutor() != null) {
			adapter.setTaskExecutor(configurer.getTaskExecutor());
		}
		if (configurer.getTimeout() != null) {
			adapter.setAsyncRequestTimeout(configurer.getTimeout());
		}
		adapter.setCallableInterceptors(configurer.getCallableInterceptors());
		adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());

		return adapter;
	}

	/**
	 * 用于插入{@link RequestMappingHandlerAdapter}的自定义子类.
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestMappingHandlerAdapter();
	}

	/**
	 * 返回{@link ConfigurableWebBindingInitializer}, 用于初始化所有{@link WebDataBinder}实例.
	 */
	protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer() {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(mvcConversionService());
		initializer.setValidator(mvcValidator());
		initializer.setMessageCodesResolver(getMessageCodesResolver());
		return initializer;
	}

	/**
	 * 重写此方法以提供自定义{@link MessageCodesResolver}.
	 */
	protected MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

	/**
	 * 重写此方法以配置异步请求处理选项.
	 */
	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 返回和带注解的控制器一起使用的{@link FormattingConversionService}.
	 * <p>请参阅{@link #addFormatters}作为替代此方法的替代方法.
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		addFormatters(conversionService);
		return conversionService;
	}

	/**
	 * 重写此方法, 将添加委托给公共{@link FormattingConversionService}的自定义{@link Converter}和/或{@link Formatter}.
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 返回全局{@link Validator}实例, 例如用于验证{@code @ModelAttribute} 和 {@code @RequestBody}方法参数.
	 * 首先委托给{@link #getValidator()}, 如果返回{@code null},
	 * 则在创建{@code OptionalValidatorFactoryBean}之前检查类路径是否存在JSR-303实现.
	 * 如果JSR-303实现不可用, 则返回no-op {@link Validator}.
	 */
	@Bean
	public Validator mvcValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException("Could not find default validator class", ex);
				}
				catch (LinkageError ex) {
					throw new BeanInitializationException("Could not load default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new NoOpValidator();
			}
		}
		return validator;
	}

	/**
	 * 重写此方法以提供自定义{@link Validator}.
	 */
	protected Validator getValidator() {
		return null;
	}

	/**
	 * 提供对{@link RequestMappingHandlerAdapter}和{@link ExceptionHandlerExceptionResolver}
	 * 使用的共享自定义参数解析器的访问.
	 * <p>此方法不能被覆盖; 改用{@link #addArgumentResolvers}.
	 */
	protected final List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		if (this.argumentResolvers == null) {
			this.argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
			addArgumentResolvers(this.argumentResolvers);
		}
		return this.argumentResolvers;
	}

	/**
	 * 添加自定义{@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
	 * <p>自定义参数解析器在内置解析器之前被调用, 除了那些依赖于注解的解析器
	 * (e.g. {@code @RequestParameter}, {@code @PathVariable}, etc).
	 * 后者可以通过直接配置{@link RequestMappingHandlerAdapter}来自定义.
	 * 
	 * @param argumentResolvers 自定义转换器列表 (最初是一个空列表)
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * 提供对{@link RequestMappingHandlerAdapter}和{@link ExceptionHandlerExceptionResolver}
	 * 使用的共享返回值处理器的访问.
	 * <p>此方法不能被覆盖; 改用{@link #addReturnValueHandlers}.
	 */
	protected final List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		if (this.returnValueHandlers == null) {
			this.returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
			addReturnValueHandlers(this.returnValueHandlers);
		}
		return this.returnValueHandlers;
	}

	/**
	 * 添加自定义{@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
	 * <p>自定义返回值处理器在内置的依赖于注解的处理器之前调用
	 * (e.g. {@code @ResponseBody}, {@code @ModelAttribute}, etc).
	 * 后者可以通过直接配置{@link RequestMappingHandlerAdapter}来自定义.
	 * 
	 * @param returnValueHandlers 自定义处理器列表 (最初是一个空列表)
	 */
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * 提供对{@link RequestMappingHandlerAdapter}和{@link ExceptionHandlerExceptionResolver}
	 * 使用的共享{@link HttpMessageConverter HttpMessageConverters}的访问.
	 * <p>此方法不能被覆盖; 改用{@link #configureMessageConverters}.
	 * 另请参阅{@link #addDefaultHttpMessageConverters}以添加默认消息转换器.
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<HttpMessageConverter<?>>();
			configureMessageConverters(this.messageConverters);
			if (this.messageConverters.isEmpty()) {
				addDefaultHttpMessageConverters(this.messageConverters);
			}
			extendMessageConverters(this.messageConverters);
		}
		return this.messageConverters;
	}

	/**
	 * 重写此方法以添加自定义{@link HttpMessageConverter HttpMessageConverters}
	 * 以与{@link RequestMappingHandlerAdapter}和{@link ExceptionHandlerExceptionResolver}一起使用.
	 * <p>将转换器添加到列表会关闭默认注册的默认转换器.
	 * 另请参阅{@link #addDefaultHttpMessageConverters}以添加默认消息转换器.
	 * 
	 * @param converters 用于添加消息转换器的列表 (最初是空列表)
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 在配置转换器列表后, 重写此方法以扩展或修改转换器列表.
	 * 例如, 这可能有用, 允许注册默认转换器, 然后通过此方法插入自定义转换器.
	 * 
	 * @param converters 要扩展的配置的转换器列表
	 */
	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 将一组默认的HttpMessageConverter实例添加到给定列表中.
	 * 子类可以从{@link #configureMessageConverters}调用此方法.
	 * 
	 * @param messageConverters 要添加默认消息转换器的列表
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);

		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringConverter);
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		if (romePresent) {
			messageConverters.add(new AtomFeedHttpMessageConverter());
			messageConverters.add(new RssChannelHttpMessageConverter());
		}

		if (jackson2XmlPresent) {
			messageConverters.add(new MappingJackson2XmlHttpMessageConverter(
					Jackson2ObjectMapperBuilder.xml().applicationContext(this.applicationContext).build()));
		}
		else if (jaxb2Present) {
			messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}

		if (jackson2Present) {
			messageConverters.add(new MappingJackson2HttpMessageConverter(
					Jackson2ObjectMapperBuilder.json().applicationContext(this.applicationContext).build()));
		}
		else if (gsonPresent) {
			messageConverters.add(new GsonHttpMessageConverter());
		}
	}

	/**
	 * 返回{@link CompositeUriComponentsContributor}的实例, 以便与
	 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}一起使用.
	 */
	@Bean
	public CompositeUriComponentsContributor mvcUriComponentsContributor() {
		return new CompositeUriComponentsContributor(
				requestMappingHandlerAdapter().getArgumentResolvers(), mvcConversionService());
	}

	/**
	 * 返回{@link HttpRequestHandlerAdapter}, 用于使用{@link HttpRequestHandler}处理请求.
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * 返回{@link SimpleControllerHandlerAdapter}, 用于处理基于接口的控制器的请求.
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * 返回{@link HandlerExceptionResolverComposite}, 其中包含通过
	 * {@link #configureHandlerExceptionResolvers}或{@link #addDefaultHandlerExceptionResolvers}
	 * 获得的异常解析器列表.
	 * <p><strong>Note:</strong> 由于CGLIB约束, 此方法无法成为final方法.
	 * 考虑覆盖{@link #configureHandlerExceptionResolvers}而不是覆盖它, 允许提供一个解析器列表.
	 */
	@Bean
	public HandlerExceptionResolver handlerExceptionResolver() {
		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<HandlerExceptionResolver>();
		configureHandlerExceptionResolvers(exceptionResolvers);
		if (exceptionResolvers.isEmpty()) {
			addDefaultHandlerExceptionResolvers(exceptionResolvers);
		}
		extendHandlerExceptionResolvers(exceptionResolvers);
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(exceptionResolvers);
		return composite;
	}

	/**
	 * 重写此方法以配置要使用的{@link HandlerExceptionResolver HandlerExceptionResolvers}列表.
	 * <p>将解析器添加到列表会关闭默认注册的默认解析器.
	 * 另请参阅可用于添加默认异常解析器的{@link #addDefaultHandlerExceptionResolvers}.
	 * 
	 * @param exceptionResolvers 用于添加异常解析器的列表 (最初为空列表)
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 在配置后, 重写此方法以扩展或修改{@link HandlerExceptionResolver HandlerExceptionResolvers}列表.
	 * <p>这可能很有用, 例如允许注册默认解析器, 然后通过此方法插入自定义解析器.
	 * 
	 * @param exceptionResolvers 要扩展的配置的解析器列表
	 */
	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * 可用于子类的方法, 用于添加默认{@link HandlerExceptionResolver HandlerExceptionResolvers}.
	 * <p>添加以下异常解析器:
	 * <ul>
	 * <li>{@link ExceptionHandlerExceptionResolver}用于通过
	 * {@link org.springframework.web.bind.annotation.ExceptionHandler}方法处理异常.
	 * <li>{@link ResponseStatusExceptionResolver}用于
	 * 带{@link org.springframework.web.bind.annotation.ResponseStatus}注解的异常.
	 * <li>{@link DefaultHandlerExceptionResolver}用于解析已知的Spring异常类型
	 * </ul>
	 */
	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();
		exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager());
		exceptionHandlerResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());
		exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());
		if (jackson2Present) {
			exceptionHandlerResolver.setResponseBodyAdvice(
					Collections.<ResponseBodyAdvice<?>>singletonList(new JsonViewResponseBodyAdvice()));
		}
		exceptionHandlerResolver.setApplicationContext(this.applicationContext);
		exceptionHandlerResolver.afterPropertiesSet();
		exceptionResolvers.add(exceptionHandlerResolver);

		ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();
		responseStatusResolver.setMessageSource(this.applicationContext);
		exceptionResolvers.add(responseStatusResolver);

		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	/**
	 * 用于插入{@link ExceptionHandlerExceptionResolver}的自定义子类.
	 */
	protected ExceptionHandlerExceptionResolver createExceptionHandlerExceptionResolver() {
		return new ExceptionHandlerExceptionResolver();
	}

	/**
	 * 注册一个{@link ViewResolverComposite}, 其中包含一系列视图解析器.
	 * 默认此解析器的排序为0, 除非使用内容协商视图解析, 在这种情况下顺序被提升为
	 * {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE Ordered.HIGHEST_PRECEDENCE}.
	 * <p>如果没有配置其他解析器, {@link ViewResolverComposite#resolveViewName(String, Locale)}将返回null,
	 * 以允许其他潜在的{@link ViewResolver} bean解析视图.
	 */
	@Bean
	public ViewResolver mvcViewResolver() {
		ViewResolverRegistry registry = new ViewResolverRegistry(
				mvcContentNegotiationManager(), this.applicationContext);
		configureViewResolvers(registry);

		if (registry.getViewResolvers().isEmpty()) {
			String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.applicationContext, ViewResolver.class, true, false);
			if (names.length == 1) {
				registry.getViewResolvers().add(new InternalResourceViewResolver());
			}
		}

		ViewResolverComposite composite = new ViewResolverComposite();
		composite.setOrder(registry.getOrder());
		composite.setViewResolvers(registry.getViewResolvers());
		composite.setApplicationContext(this.applicationContext);
		composite.setServletContext(this.servletContext);
		return composite;
	}

	/**
	 * 重写此方法以配置视图解析.
	 */
	protected void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * 返回注册的{@link CorsConfiguration}对象, 由路径模式作为键.
	 */
	protected final Map<String, CorsConfiguration> getCorsConfigurations() {
		if (this.corsConfigurations == null) {
			CorsRegistry registry = new CorsRegistry();
			addCorsMappings(registry);
			this.corsConfigurations = registry.getCorsConfigurations();
		}
		return this.corsConfigurations;
	}

	/**
	 * 重写此方法以配置跨源请求处理.
	 */
	protected void addCorsMappings(CorsRegistry registry) {
	}

	@Bean
	@Lazy
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}



	private static final class EmptyHandlerMapping extends AbstractHandlerMapping {

		@Override
		protected Object getHandlerInternal(HttpServletRequest request) {
			return null;
		}
	}


	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}
	}
}
