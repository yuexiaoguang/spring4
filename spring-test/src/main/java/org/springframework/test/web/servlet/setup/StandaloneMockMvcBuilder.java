package org.springframework.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * {@code MockMvcBuilder}, 接受{@code @Controller}注册, 从而允许完全控制控制器及其依赖关系的实例化和初始化,
 * 类似于普通单元测试, 并且还可以一次测试一个控制器.
 *
 * <p>此构建器创建{@link DispatcherServlet}所需的最小基础结构, 以使用带注解的控制器来处理请求, 并提供自定义方法.
 * 生成的配置和自定义选项等同于使用MVC Java配置, 但使用构建器样式方法.
 *
 * <p>要配置视图解析, 请选择"固定"视图以用于每个执行的请求 (see {@link #setSingleView(View)})
 * 或提供​​{@code ViewResolver}的列表 (see {@link #setViewResolvers(ViewResolver...)}).
 */
public class StandaloneMockMvcBuilder extends AbstractMockMvcBuilder<StandaloneMockMvcBuilder> {

	private final Object[] controllers;

	private List<Object> controllerAdvice;

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();

	private final List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>();

	private Validator validator = null;

	private ContentNegotiationManager contentNegotiationManager;

	private FormattingConversionService conversionService = null;

	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	private Long asyncRequestTimeout;

	private List<ViewResolver> viewResolvers;

	private LocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

	private FlashMapManager flashMapManager = null;

	private boolean useSuffixPatternMatch = true;

	private boolean useTrailingSlashPatternMatch = true;

	private Boolean removeSemicolonContent;

	private Map<String, String> placeholderValues = new HashMap<String, String>();


	/**
	 * 不适用于直接实例化.
	 */
	protected StandaloneMockMvcBuilder(Object... controllers) {
		this.controllers = controllers;
	}


	/**
	 * 注册一个或多个要在测试中使用的{@link org.springframework.web.bind.annotation.ControllerAdvice}实例.
	 * <p>通常{@code @ControllerAdvice}是自动检测的, 只要它们被声明为Spring bean即可.
	 * 但是, 由于独立安装程序不加载任何Spring配置, 因此需要在此处明确注册, 而不是像控制器一样.
	 */
	public StandaloneMockMvcBuilder setControllerAdvice(Object... controllerAdvice) {
		this.controllerAdvice = Arrays.asList(controllerAdvice);
		return this;
	}

	/**
	 * 设置消息转换器, 以在参数解析器和返回值处理器中使用, 它们支持读取和/或写入请求和响应的主体.
	 * 如果列表中未添加任何消息转换器, 则会添加默认的转换器列表.
	 */
	public StandaloneMockMvcBuilder setMessageConverters(HttpMessageConverter<?>...messageConverters) {
		this.messageConverters = Arrays.asList(messageConverters);
		return this;
	}

	/**
	 * 提供自定义{@link Validator}, 而不是默认创建的自定义{@link Validator}.
	 * 假设JSR-303在类路径上, 使用的默认实现是
	 * {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}.
	 */
	public StandaloneMockMvcBuilder setValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * 使用自定义格式化器和转换器提供转换服务.
	 * 如果未设置, 则默认使用{@link DefaultFormattingConversionService}.
	 */
	public StandaloneMockMvcBuilder setConversionService(FormattingConversionService conversionService) {
		this.conversionService = conversionService;
		return this;
	}

	/**
	 * 添加映射到所有传入请求的拦截器.
	 */
	public StandaloneMockMvcBuilder addInterceptors(HandlerInterceptor... interceptors) {
		addMappedInterceptors(null, interceptors);
		return this;
	}

	/**
	 * 添加映射到一组路径模式的拦截器.
	 */
	public StandaloneMockMvcBuilder addMappedInterceptors(String[] pathPatterns, HandlerInterceptor... interceptors) {
		for (HandlerInterceptor interceptor : interceptors) {
			this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, interceptor));
		}
		return this;
	}

	/**
	 * 设置ContentNegotiationManager.
	 */
	public StandaloneMockMvcBuilder setContentNegotiationManager(ContentNegotiationManager manager) {
		this.contentNegotiationManager = manager;
		return this;
	}

	/**
	 * 指定异步执行的超时值.
	 * 在Spring MVC Test中, 此值用于确定如何等待异步执行完成, 以便测试可以同步验证结果.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public StandaloneMockMvcBuilder setAsyncRequestTimeout(long timeout) {
		this.asyncRequestTimeout = timeout;
		return this;
	}

	/**
	 * 为控制器方法参数提供自定义解析器.
	 */
	public StandaloneMockMvcBuilder setCustomArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
		this.customArgumentResolvers = Arrays.asList(argumentResolvers);
		return this;
	}

	/**
	 * 为控制器方法返回值提供自定义处理器.
	 */
	public StandaloneMockMvcBuilder setCustomReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
		this.customReturnValueHandlers = Arrays.asList(handlers);
		return this;
	}

	/**
	 * 设置HandlerExceptionResolver类型以用作列表.
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		this.handlerExceptionResolvers = exceptionResolvers;
		return this;
	}

	/**
	 * 设置HandlerExceptionResolver类型以用作数组.
	 */
	public StandaloneMockMvcBuilder setHandlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
		this.handlerExceptionResolvers = Arrays.asList(exceptionResolvers);
		return this;
	}

	/**
	 * 使用给定的{@link ViewResolver}设置视图解析.
	 * 如果未设置, 则默认使用{@link InternalResourceViewResolver}.
	 */
	public StandaloneMockMvcBuilder setViewResolvers(ViewResolver...resolvers) {
		this.viewResolvers = Arrays.asList(resolvers);
		return this;
	}

	/**
	 * 设置一个{@link ViewResolver}, 它始终返回提供的视图实例.
	 * 如果只需要使用一个View实例, 这是一个方便的快捷方式 -- e.g. 渲染生成的内容 (JSON, XML, Atom).
	 */
	public StandaloneMockMvcBuilder setSingleView(View view) {
		this.viewResolvers = Collections.<ViewResolver>singletonList(new StaticViewResolver(view));
		return this;
	}

	/**
	 * 提供LocaleResolver实例.
	 * 如果未提供, 则使用的默认值为{@link AcceptHeaderLocaleResolver}.
	 */
	public StandaloneMockMvcBuilder setLocaleResolver(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
		return this;
	}

	/**
	 * 提供自定义FlashMapManager实例.
	 * 如果未提供, 则默认使用{@code SessionFlashMapManager}.
	 */
	public StandaloneMockMvcBuilder setFlashMapManager(FlashMapManager flashMapManager) {
		this.flashMapManager = flashMapManager;
		return this;
	}

	/**
	 * 在将模式与请求匹配时是否使用后缀模式匹配 (".*").
	 * 如果启用, 映射到"/users"的方法也匹配"/users.*".
	 * <p>默认{@code true}.
	 */
	public StandaloneMockMvcBuilder setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		return this;
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 * 如果启用, 映射到"/users"的方法也匹配"/users/".
	 * <p>默认{@code true}.
	 */
	public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
		this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
		return this;
	}

	/**
	 * 设置是否应从请求URI中删除";" (分号)内容.
	 * 如果提供了值, 则依次在
	 * {@link AbstractHandlerMapping#setRemoveSemicolonContent(boolean)}上设置.
	 */
	public StandaloneMockMvcBuilder setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
		return this;
	}

	/**
	 * 在独立设置中, 不支持嵌入在请求映射中的占位符值.
	 * 此方法允许手动提供占位符值, 以便可以解析它们.
	 * 或者考虑创建一个初始化{@link WebApplicationContext}的测试.
	 */
	public StandaloneMockMvcBuilder addPlaceholderValue(String name, String value) {
		this.placeholderValues.put(name, value);
		return this;
	}

	/**
	 * @deprecated as of 4.2.8, in favor of {@link #addPlaceholderValue(String, String)}
	 */
	@Deprecated
	public StandaloneMockMvcBuilder addPlaceHolderValue(String name, String value) {
		this.placeholderValues.put(name, value);
		return this;
	}


	@Override
	protected WebApplicationContext initWebAppContext() {
		MockServletContext servletContext = new MockServletContext();
		StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);
		registerMvcSingletons(wac);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		return wac;
	}

	private void registerMvcSingletons(StubWebApplicationContext wac) {
		StandaloneConfiguration config = new StandaloneConfiguration();
		config.setApplicationContext(wac);

		wac.addBeans(this.controllerAdvice);

		StaticRequestMappingHandlerMapping hm = config.getHandlerMapping();
		hm.setServletContext(wac.getServletContext());
		hm.setApplicationContext(wac);
		hm.afterPropertiesSet();
		hm.registerHandlers(this.controllers);
		wac.addBean("requestMappingHandlerMapping", hm);

		RequestMappingHandlerAdapter handlerAdapter = config.requestMappingHandlerAdapter();
		handlerAdapter.setServletContext(wac.getServletContext());
		handlerAdapter.setApplicationContext(wac);
		handlerAdapter.afterPropertiesSet();
		wac.addBean("requestMappingHandlerAdapter", handlerAdapter);

		wac.addBean("handlerExceptionResolver", config.handlerExceptionResolver());

		wac.addBeans(initViewResolvers(wac));
		wac.addBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, this.localeResolver);
		wac.addBean(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, new FixedThemeResolver());
		wac.addBean(DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, new DefaultRequestToViewNameTranslator());

		this.flashMapManager = new SessionFlashMapManager();
		wac.addBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, this.flashMapManager);
	}

	private List<ViewResolver> initViewResolvers(WebApplicationContext wac) {
		this.viewResolvers = (this.viewResolvers != null ? this.viewResolvers :
				Collections.<ViewResolver>singletonList(new InternalResourceViewResolver()));
		for (Object viewResolver : this.viewResolvers) {
			if (viewResolver instanceof WebApplicationObjectSupport) {
				((WebApplicationObjectSupport) viewResolver).setApplicationContext(wac);
			}
		}
		return this.viewResolvers;
	}


	/** 使用MVC Java配置作为"独立"设置的起点 */
	private class StandaloneConfiguration extends WebMvcConfigurationSupport {

		public StaticRequestMappingHandlerMapping getHandlerMapping() {
			StaticRequestMappingHandlerMapping handlerMapping = new StaticRequestMappingHandlerMapping();
			handlerMapping.setEmbeddedValueResolver(new StaticStringValueResolver(placeholderValues));
			handlerMapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
			handlerMapping.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
			handlerMapping.setOrder(0);
			handlerMapping.setInterceptors(getInterceptors());
			if (removeSemicolonContent != null) {
				handlerMapping.setRemoveSemicolonContent(removeSemicolonContent);
			}
			return handlerMapping;
		}

		@Override
		protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.addAll(messageConverters);
		}

		@Override
		protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.addAll(customArgumentResolvers);
		}

		@Override
		protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.addAll(customReturnValueHandlers);
		}

		@Override
		protected void addInterceptors(InterceptorRegistry registry) {
			for (MappedInterceptor interceptor : mappedInterceptors) {
				InterceptorRegistration registration = registry.addInterceptor(interceptor.getInterceptor());
				if (interceptor.getPathPatterns() != null) {
					registration.addPathPatterns(interceptor.getPathPatterns());
				}
			}
		}

		@Override
		public ContentNegotiationManager mvcContentNegotiationManager() {
			return (contentNegotiationManager != null) ? contentNegotiationManager : super.mvcContentNegotiationManager();
		}

		@Override
		public FormattingConversionService mvcConversionService() {
			return (conversionService != null) ? conversionService : super.mvcConversionService();
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			if (asyncRequestTimeout != null) {
				configurer.setDefaultTimeout(asyncRequestTimeout);
			}
		}

		@Override
		public Validator mvcValidator() {
			Validator mvcValidator = (validator != null) ? validator : super.mvcValidator();
			if (mvcValidator instanceof InitializingBean) {
				try {
					((InitializingBean) mvcValidator).afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Failed to initialize Validator", ex);
				}
			}
			return mvcValidator;
		}

		@Override
		protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			if (handlerExceptionResolvers == null) {
				return;
			}
			for (HandlerExceptionResolver resolver : handlerExceptionResolvers) {
				if (resolver instanceof ApplicationContextAware) {
					((ApplicationContextAware) resolver).setApplicationContext(getApplicationContext());
				}
				if (resolver instanceof InitializingBean) {
					try {
						((InitializingBean) resolver).afterPropertiesSet();
					}
					catch (Exception ex) {
						throw new IllegalStateException("Failure from afterPropertiesSet", ex);
					}
				}
				exceptionResolvers.add(resolver);
			}
		}
	}


	/**
	 * 允许注册控制器的{@code RequestMappingHandlerMapping}.
	 */
	private static class StaticRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

		public void registerHandlers(Object...handlers) {
			for (Object handler : handlers) {
				detectHandlerMethods(handler);
			}
		}
	}


	/**
	 * 静态解析器占位符, 用于嵌入请求映射中的值.
	 */
	private static class StaticStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public StaticStringValueResolver(final Map<String, String> values) {
			this.helper = new PropertyPlaceholderHelper("${", "}", ":", false);
			this.resolver = new PlaceholderResolver() {
				@Override
				public String resolvePlaceholder(String placeholderName) {
					return values.get(placeholderName);
				}
			};
		}

		@Override
		public String resolveStringValue(String strVal) throws BeansException {
			return this.helper.replacePlaceholders(strVal, this.resolver);
		}
	}


	/**
	 * 一个始终返回相同View的{@link ViewResolver}.
	 */
	private static class StaticViewResolver implements ViewResolver {

		private final View view;

		public StaticViewResolver(View view) {
			this.view = view;
		}

		@Override
		public View resolveViewName(String viewName, Locale locale) {
			return this.view;
		}
	}
}
