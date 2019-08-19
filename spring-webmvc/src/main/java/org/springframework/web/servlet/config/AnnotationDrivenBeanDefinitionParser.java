package org.springframework.web.servlet.config;

import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * {@link BeanDefinitionParser}, 它为{@code <annotation-driven/>} MVC命名空间元素提供配置.
 *
 * <p>该类注册以下{@link HandlerMapping}:</p>
 * <ul>
 * <li>{@link RequestMappingHandlerMapping}在0处排序, 用于将请求映射到带注解的控制器方法.
 * <li>{@link BeanNameUrlHandlerMapping}在2处排序, 将URL路径映射到控制器bean名称.
 * </ul>
 *
 * <p><strong>Note:</strong> 由于使用{@code <view-controller>}或{@code <resources>} MVC命名空间元素,
 * 可能会注册其他HandlerMapping.
 *
 * <p>该类注册以下{@link HandlerAdapter}:
 * <ul>
 * <li>{@link RequestMappingHandlerAdapter}用于使用带注解的控制器方法处理请求.
 * <li>{@link HttpRequestHandlerAdapter}用于使用{@link HttpRequestHandler}处理请求.
 * <li>{@link SimpleControllerHandlerAdapter}用于处理基于接口的{@link Controller}的请求.
 * </ul>
 *
 * <p>该类注册以下{@link HandlerExceptionResolver}:
 * <ul>
 * <li>{@link ExceptionHandlerExceptionResolver}用于通过
 * {@link org.springframework.web.bind.annotation.ExceptionHandler}方法处理异常.
 * <li>{@link ResponseStatusExceptionResolver}用于带
 * {@link org.springframework.web.bind.annotation.ResponseStatus}注解的异常.
 * <li>{@link DefaultHandlerExceptionResolver}用于解析已知的Spring异常类型
 * </ul>
 *
 * <p>此类注册要使用的{@link org.springframework.util.AntPathMatcher}
 * 和{@link org.springframework.web.util.UrlPathHelper}:
 * <ul>
 * <li>{@link RequestMappingHandlerMapping},
 * <li>{@link HandlerMapping}用于ViewControllers
 * <li>{@link HandlerMapping}用于服务资源
 * </ul>
 * 请注意, 可以使用{@code path-matching} MVC命名空间元素配置这些bean.
 *
 * <p>默认情况下, {@link RequestMappingHandlerAdapter}
 * 和{@link ExceptionHandlerExceptionResolver}都配置了以下实例:
 * <ul>
 * <li>{@link ContentNegotiationManager}
 * <li>{@link DefaultFormattingConversionService}
 * <li>{@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean},
 * 如果类路径上有JSR-303实现可用
 * <li>一系列{@link HttpMessageConverter}, 取决于类路径上可用的第三方库.
 * </ul>
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	public static final String HANDLER_MAPPING_BEAN_NAME = RequestMappingHandlerMapping.class.getName();

	public static final String HANDLER_ADAPTER_BEAN_NAME = RequestMappingHandlerAdapter.class.getName();

	public static final String CONTENT_NEGOTIATION_MANAGER_BEAN_NAME = "mvcContentNegotiationManager";


	private static final boolean javaxValidationPresent =
			ClassUtils.isPresent("javax.validation.Validator",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.rometools.rome.feed.WireFeed",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader()) &&
			ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean jackson2XmlPresent =
			ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());

	private static final boolean gsonPresent =
			ClassUtils.isPresent("com.google.gson.Gson",
					AnnotationDrivenBeanDefinitionParser.class.getClassLoader());


	@Override
	public BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		XmlReaderContext readerContext = context.getReaderContext();

		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		context.pushContainingComponent(compDefinition);

		RuntimeBeanReference contentNegotiationManager = getContentNegotiationManager(element, source, context);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("order", 0);
		handlerMappingDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);

		if (element.hasAttribute("enable-matrix-variables")) {
			Boolean enableMatrixVariables = Boolean.valueOf(element.getAttribute("enable-matrix-variables"));
			handlerMappingDef.getPropertyValues().add("removeSemicolonContent", !enableMatrixVariables);
		}
		else if (element.hasAttribute("enableMatrixVariables")) {
			Boolean enableMatrixVariables = Boolean.valueOf(element.getAttribute("enableMatrixVariables"));
			handlerMappingDef.getPropertyValues().add("removeSemicolonContent", !enableMatrixVariables);
		}

		configurePathMatchingProperties(handlerMappingDef, element, context);
		readerContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME , handlerMappingDef);

		RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		handlerMappingDef.getPropertyValues().add("corsConfigurations", corsRef);

		RuntimeBeanReference conversionService = getConversionService(element, source, context);
		RuntimeBeanReference validator = getValidator(element, source, context);
		RuntimeBeanReference messageCodesResolver = getMessageCodesResolver(element);

		RootBeanDefinition bindingDef = new RootBeanDefinition(ConfigurableWebBindingInitializer.class);
		bindingDef.setSource(source);
		bindingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bindingDef.getPropertyValues().add("conversionService", conversionService);
		bindingDef.getPropertyValues().add("validator", validator);
		bindingDef.getPropertyValues().add("messageCodesResolver", messageCodesResolver);

		ManagedList<?> messageConverters = getMessageConverters(element, source, context);
		ManagedList<?> argumentResolvers = getArgumentResolvers(element, context);
		ManagedList<?> returnValueHandlers = getReturnValueHandlers(element, context);
		String asyncTimeout = getAsyncTimeout(element);
		RuntimeBeanReference asyncExecutor = getAsyncExecutor(element);
		ManagedList<?> callableInterceptors = getCallableInterceptors(element, source, context);
		ManagedList<?> deferredResultInterceptors = getDeferredResultInterceptors(element, source, context);

		RootBeanDefinition handlerAdapterDef = new RootBeanDefinition(RequestMappingHandlerAdapter.class);
		handlerAdapterDef.setSource(source);
		handlerAdapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerAdapterDef.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		handlerAdapterDef.getPropertyValues().add("webBindingInitializer", bindingDef);
		handlerAdapterDef.getPropertyValues().add("messageConverters", messageConverters);
		addRequestBodyAdvice(handlerAdapterDef);
		addResponseBodyAdvice(handlerAdapterDef);

		if (element.hasAttribute("ignore-default-model-on-redirect")) {
			Boolean ignoreDefaultModel = Boolean.valueOf(element.getAttribute("ignore-default-model-on-redirect"));
			handlerAdapterDef.getPropertyValues().add("ignoreDefaultModelOnRedirect", ignoreDefaultModel);
		}
		else if (element.hasAttribute("ignoreDefaultModelOnRedirect")) {
			// "ignoreDefaultModelOnRedirect" spelling is deprecated
			Boolean ignoreDefaultModel = Boolean.valueOf(element.getAttribute("ignoreDefaultModelOnRedirect"));
			handlerAdapterDef.getPropertyValues().add("ignoreDefaultModelOnRedirect", ignoreDefaultModel);
		}

		if (argumentResolvers != null) {
			handlerAdapterDef.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			handlerAdapterDef.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		if (asyncTimeout != null) {
			handlerAdapterDef.getPropertyValues().add("asyncRequestTimeout", asyncTimeout);
		}
		if (asyncExecutor != null) {
			handlerAdapterDef.getPropertyValues().add("taskExecutor", asyncExecutor);
		}

		handlerAdapterDef.getPropertyValues().add("callableInterceptors", callableInterceptors);
		handlerAdapterDef.getPropertyValues().add("deferredResultInterceptors", deferredResultInterceptors);
		readerContext.getRegistry().registerBeanDefinition(HANDLER_ADAPTER_BEAN_NAME , handlerAdapterDef);

		RootBeanDefinition uriContributorDef =
				new RootBeanDefinition(CompositeUriComponentsContributorFactoryBean.class);
		uriContributorDef.setSource(source);
		uriContributorDef.getPropertyValues().addPropertyValue("handlerAdapter", handlerAdapterDef);
		uriContributorDef.getPropertyValues().addPropertyValue("conversionService", conversionService);
		String uriContributorName = MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME;
		readerContext.getRegistry().registerBeanDefinition(uriContributorName, uriContributorDef);

		RootBeanDefinition csInterceptorDef = new RootBeanDefinition(ConversionServiceExposingInterceptor.class);
		csInterceptorDef.setSource(source);
		csInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, conversionService);
		RootBeanDefinition mappedInterceptorDef = new RootBeanDefinition(MappedInterceptor.class);
		mappedInterceptorDef.setSource(source);
		mappedInterceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
		mappedInterceptorDef.getConstructorArgumentValues().addIndexedArgumentValue(1, csInterceptorDef);
		String mappedInterceptorName = readerContext.registerWithGeneratedName(mappedInterceptorDef);

		RootBeanDefinition methodExceptionResolver = new RootBeanDefinition(ExceptionHandlerExceptionResolver.class);
		methodExceptionResolver.setSource(source);
		methodExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		methodExceptionResolver.getPropertyValues().add("contentNegotiationManager", contentNegotiationManager);
		methodExceptionResolver.getPropertyValues().add("messageConverters", messageConverters);
		methodExceptionResolver.getPropertyValues().add("order", 0);
		addResponseBodyAdvice(methodExceptionResolver);
		if (argumentResolvers != null) {
			methodExceptionResolver.getPropertyValues().add("customArgumentResolvers", argumentResolvers);
		}
		if (returnValueHandlers != null) {
			methodExceptionResolver.getPropertyValues().add("customReturnValueHandlers", returnValueHandlers);
		}
		String methodExResolverName = readerContext.registerWithGeneratedName(methodExceptionResolver);

		RootBeanDefinition statusExceptionResolver = new RootBeanDefinition(ResponseStatusExceptionResolver.class);
		statusExceptionResolver.setSource(source);
		statusExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		statusExceptionResolver.getPropertyValues().add("order", 1);
		String statusExResolverName = readerContext.registerWithGeneratedName(statusExceptionResolver);

		RootBeanDefinition defaultExceptionResolver = new RootBeanDefinition(DefaultHandlerExceptionResolver.class);
		defaultExceptionResolver.setSource(source);
		defaultExceptionResolver.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		defaultExceptionResolver.getPropertyValues().add("order", 2);
		String defaultExResolverName = readerContext.registerWithGeneratedName(defaultExceptionResolver);

		context.registerComponent(new BeanComponentDefinition(handlerMappingDef, HANDLER_MAPPING_BEAN_NAME));
		context.registerComponent(new BeanComponentDefinition(handlerAdapterDef, HANDLER_ADAPTER_BEAN_NAME));
		context.registerComponent(new BeanComponentDefinition(uriContributorDef, uriContributorName));
		context.registerComponent(new BeanComponentDefinition(mappedInterceptorDef, mappedInterceptorName));
		context.registerComponent(new BeanComponentDefinition(methodExceptionResolver, methodExResolverName));
		context.registerComponent(new BeanComponentDefinition(statusExceptionResolver, statusExResolverName));
		context.registerComponent(new BeanComponentDefinition(defaultExceptionResolver, defaultExResolverName));

		// Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
		MvcNamespaceUtils.registerDefaultComponents(context, source);

		context.popAndRegisterContainingComponent();

		return null;
	}

	protected void addRequestBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			beanDef.getPropertyValues().add("requestBodyAdvice",
					new RootBeanDefinition(JsonViewRequestBodyAdvice.class));
		}
	}

	protected void addResponseBodyAdvice(RootBeanDefinition beanDef) {
		if (jackson2Present) {
			beanDef.getPropertyValues().add("responseBodyAdvice",
					new RootBeanDefinition(JsonViewResponseBodyAdvice.class));
		}
	}

	private RuntimeBeanReference getConversionService(Element element, Object source, ParserContext context) {
		RuntimeBeanReference conversionServiceRef;
		if (element.hasAttribute("conversion-service")) {
			conversionServiceRef = new RuntimeBeanReference(element.getAttribute("conversion-service"));
		}
		else {
			RootBeanDefinition conversionDef = new RootBeanDefinition(FormattingConversionServiceFactoryBean.class);
			conversionDef.setSource(source);
			conversionDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String conversionName = context.getReaderContext().registerWithGeneratedName(conversionDef);
			context.registerComponent(new BeanComponentDefinition(conversionDef, conversionName));
			conversionServiceRef = new RuntimeBeanReference(conversionName);
		}
		return conversionServiceRef;
	}

	private RuntimeBeanReference getValidator(Element element, Object source, ParserContext context) {
		if (element.hasAttribute("validator")) {
			return new RuntimeBeanReference(element.getAttribute("validator"));
		}
		else if (javaxValidationPresent) {
			RootBeanDefinition validatorDef = new RootBeanDefinition(
					"org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean");
			validatorDef.setSource(source);
			validatorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String validatorName = context.getReaderContext().registerWithGeneratedName(validatorDef);
			context.registerComponent(new BeanComponentDefinition(validatorDef, validatorName));
			return new RuntimeBeanReference(validatorName);
		}
		else {
			return null;
		}
	}

	private RuntimeBeanReference getContentNegotiationManager(Element element, Object source, ParserContext context) {
		RuntimeBeanReference beanRef;
		if (element.hasAttribute("content-negotiation-manager")) {
			String name = element.getAttribute("content-negotiation-manager");
			beanRef = new RuntimeBeanReference(name);
		}
		else {
			RootBeanDefinition factoryBeanDef = new RootBeanDefinition(ContentNegotiationManagerFactoryBean.class);
			factoryBeanDef.setSource(source);
			factoryBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			factoryBeanDef.getPropertyValues().add("mediaTypes", getDefaultMediaTypes());
			String name = CONTENT_NEGOTIATION_MANAGER_BEAN_NAME;
			context.getReaderContext().getRegistry().registerBeanDefinition(name , factoryBeanDef);
			context.registerComponent(new BeanComponentDefinition(factoryBeanDef, name));
			beanRef = new RuntimeBeanReference(name);
		}
		return beanRef;
	}

	private void configurePathMatchingProperties(
			RootBeanDefinition handlerMappingDef, Element element, ParserContext context) {

		Element pathMatchingElement = DomUtils.getChildElementByTagName(element, "path-matching");
		if (pathMatchingElement != null) {
			Object source = context.extractSource(element);

			if (pathMatchingElement.hasAttribute("suffix-pattern")) {
				Boolean useSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("suffix-pattern"));
				handlerMappingDef.getPropertyValues().add("useSuffixPatternMatch", useSuffixPatternMatch);
			}
			if (pathMatchingElement.hasAttribute("trailing-slash")) {
				Boolean useTrailingSlashMatch = Boolean.valueOf(pathMatchingElement.getAttribute("trailing-slash"));
				handlerMappingDef.getPropertyValues().add("useTrailingSlashMatch", useTrailingSlashMatch);
			}
			if (pathMatchingElement.hasAttribute("registered-suffixes-only")) {
				Boolean useRegisteredSuffixPatternMatch = Boolean.valueOf(pathMatchingElement.getAttribute("registered-suffixes-only"));
				handlerMappingDef.getPropertyValues().add("useRegisteredSuffixPatternMatch", useRegisteredSuffixPatternMatch);
			}

			RuntimeBeanReference pathHelperRef = null;
			if (pathMatchingElement.hasAttribute("path-helper")) {
				pathHelperRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-helper"));
			}
			pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(pathHelperRef, context, source);
			handlerMappingDef.getPropertyValues().add("urlPathHelper", pathHelperRef);

			RuntimeBeanReference pathMatcherRef = null;
			if (pathMatchingElement.hasAttribute("path-matcher")) {
				pathMatcherRef = new RuntimeBeanReference(pathMatchingElement.getAttribute("path-matcher"));
			}
			pathMatcherRef = MvcNamespaceUtils.registerPathMatcher(pathMatcherRef, context, source);
			handlerMappingDef.getPropertyValues().add("pathMatcher", pathMatcherRef);
		}
	}

	private Properties getDefaultMediaTypes() {
		Properties defaultMediaTypes = new Properties();
		if (romePresent) {
			defaultMediaTypes.put("atom", MediaType.APPLICATION_ATOM_XML_VALUE);
			defaultMediaTypes.put("rss", MediaType.APPLICATION_RSS_XML_VALUE);
		}
		if (jaxb2Present || jackson2XmlPresent) {
			defaultMediaTypes.put("xml", MediaType.APPLICATION_XML_VALUE);
		}
		if (jackson2Present || gsonPresent) {
			defaultMediaTypes.put("json", MediaType.APPLICATION_JSON_VALUE);
		}
		return defaultMediaTypes;
	}

	private RuntimeBeanReference getMessageCodesResolver(Element element) {
		if (element.hasAttribute("message-codes-resolver")) {
			return new RuntimeBeanReference(element.getAttribute("message-codes-resolver"));
		}
		else {
			return null;
		}
	}

	private String getAsyncTimeout(Element element) {
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		return (asyncElement != null ? asyncElement.getAttribute("default-timeout") : null);
	}

	private RuntimeBeanReference getAsyncExecutor(Element element) {
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null && asyncElement.hasAttribute("task-executor")) {
			return new RuntimeBeanReference(asyncElement.getAttribute("task-executor"));
		}
		return null;
	}

	private ManagedList<?> getCallableInterceptors(Element element, Object source, ParserContext context) {
		ManagedList<Object> interceptors = new ManagedList<Object>();
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(asyncElement, "callable-interceptors");
			if (interceptorsElement != null) {
				interceptors.setSource(source);
				for (Element converter : DomUtils.getChildElementsByTagName(interceptorsElement, "bean")) {
					BeanDefinitionHolder beanDef = context.getDelegate().parseBeanDefinitionElement(converter);
					beanDef = context.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
					interceptors.add(beanDef);
				}
			}
		}
		return interceptors;
	}

	private ManagedList<?> getDeferredResultInterceptors(Element element, Object source, ParserContext context) {
		ManagedList<Object> interceptors = new ManagedList<Object>();
		Element asyncElement = DomUtils.getChildElementByTagName(element, "async-support");
		if (asyncElement != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(asyncElement, "deferred-result-interceptors");
			if (interceptorsElement != null) {
				interceptors.setSource(source);
				for (Element converter : DomUtils.getChildElementsByTagName(interceptorsElement, "bean")) {
					BeanDefinitionHolder beanDef = context.getDelegate().parseBeanDefinitionElement(converter);
					beanDef = context.getDelegate().decorateBeanDefinitionIfRequired(converter, beanDef);
					interceptors.add(beanDef);
				}
			}
		}
		return interceptors;
	}

	private ManagedList<?> getArgumentResolvers(Element element, ParserContext context) {
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			ManagedList<Object> resolvers = extractBeanSubElements(resolversElement, context);
			return wrapLegacyResolvers(resolvers, context);
		}
		return null;
	}

	private ManagedList<Object> wrapLegacyResolvers(List<Object> list, ParserContext context) {
		ManagedList<Object> result = new ManagedList<Object>();
		for (Object object : list) {
			if (object instanceof BeanDefinitionHolder) {
				BeanDefinitionHolder beanDef = (BeanDefinitionHolder) object;
				String className = beanDef.getBeanDefinition().getBeanClassName();
				Class<?> clazz = ClassUtils.resolveClassName(className, context.getReaderContext().getBeanClassLoader());
				if (WebArgumentResolver.class.isAssignableFrom(clazz)) {
					RootBeanDefinition adapter = new RootBeanDefinition(ServletWebArgumentResolverAdapter.class);
					adapter.getConstructorArgumentValues().addIndexedArgumentValue(0, beanDef);
					result.add(new BeanDefinitionHolder(adapter, beanDef.getBeanName() + "Adapter"));
					continue;
				}
			}
			result.add(object);
		}
		return result;
	}

	private ManagedList<?> getReturnValueHandlers(Element element, ParserContext context) {
		Element handlers = DomUtils.getChildElementByTagName(element, "return-value-handlers");
		return (handlers != null ? extractBeanSubElements(handlers, context) : null);
	}

	private ManagedList<?> getMessageConverters(Element element, Object source, ParserContext context) {
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		ManagedList<Object> messageConverters = new ManagedList<Object>();
		if (convertersElement != null) {
			messageConverters.setSource(source);
			for (Element beanElement : DomUtils.getChildElementsByTagName(convertersElement, "bean", "ref")) {
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				messageConverters.add(object);
			}
		}

		if (convertersElement == null || Boolean.valueOf(convertersElement.getAttribute("register-defaults"))) {
			messageConverters.setSource(source);
			messageConverters.add(createConverterDefinition(ByteArrayHttpMessageConverter.class, source));

			RootBeanDefinition stringConverterDef = createConverterDefinition(StringHttpMessageConverter.class, source);
			stringConverterDef.getPropertyValues().add("writeAcceptCharset", false);
			messageConverters.add(stringConverterDef);

			messageConverters.add(createConverterDefinition(ResourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterDefinition(SourceHttpMessageConverter.class, source));
			messageConverters.add(createConverterDefinition(AllEncompassingFormHttpMessageConverter.class, source));

			if (romePresent) {
				messageConverters.add(createConverterDefinition(AtomFeedHttpMessageConverter.class, source));
				messageConverters.add(createConverterDefinition(RssChannelHttpMessageConverter.class, source));
			}

			if (jackson2XmlPresent) {
				Class<?> type = MappingJackson2XmlHttpMessageConverter.class;
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				jacksonFactoryDef.getPropertyValues().add("createXmlMapper", true);
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				messageConverters.add(jacksonConverterDef);
			}
			else if (jaxb2Present) {
				messageConverters.add(createConverterDefinition(Jaxb2RootElementHttpMessageConverter.class, source));
			}

			if (jackson2Present) {
				Class<?> type = MappingJackson2HttpMessageConverter.class;
				RootBeanDefinition jacksonConverterDef = createConverterDefinition(type, source);
				GenericBeanDefinition jacksonFactoryDef = createObjectMapperFactoryDefinition(source);
				jacksonConverterDef.getConstructorArgumentValues().addIndexedArgumentValue(0, jacksonFactoryDef);
				messageConverters.add(jacksonConverterDef);
			}
			else if (gsonPresent) {
				messageConverters.add(createConverterDefinition(GsonHttpMessageConverter.class, source));
			}
		}
		return messageConverters;
	}

	private GenericBeanDefinition createObjectMapperFactoryDefinition(Object source) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(Jackson2ObjectMapperFactoryBean.class);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return beanDefinition;
	}

	private RootBeanDefinition createConverterDefinition(Class<?> converterClass, Object source) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(converterClass);
		beanDefinition.setSource(source);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		return beanDefinition;
	}

	private ManagedList<Object> extractBeanSubElements(Element parentElement, ParserContext context) {
		ManagedList<Object> list = new ManagedList<Object>();
		list.setSource(context.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
			Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
			list.add(object);
		}
		return list;
	}


	/**
	 * CompositeUriComponentsContributor的FactoryBean,
	 * 它在完全初始化后获取在RequestMappingHandlerAdapter中配置的HandlerMethodArgumentResolver.
	 */
	static class CompositeUriComponentsContributorFactoryBean
			implements FactoryBean<CompositeUriComponentsContributor>, InitializingBean {

		private RequestMappingHandlerAdapter handlerAdapter;

		private ConversionService conversionService;

		private CompositeUriComponentsContributor uriComponentsContributor;

		public void setHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
			this.handlerAdapter = handlerAdapter;
		}

		public void setConversionService(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public void afterPropertiesSet() {
			this.uriComponentsContributor = new CompositeUriComponentsContributor(
					this.handlerAdapter.getArgumentResolvers(), this.conversionService);
		}

		@Override
		public CompositeUriComponentsContributor getObject() {
			return this.uriComponentsContributor;
		}

		@Override
		public Class<?> getObjectType() {
			return CompositeUriComponentsContributor.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}
}
