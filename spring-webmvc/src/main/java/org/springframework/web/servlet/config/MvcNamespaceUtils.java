package org.springframework.web.servlet.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.util.UrlPathHelper;

/**
 * 在MVC命名空间BeanDefinitionParsers中使用的便捷方法.
 */
public abstract class MvcNamespaceUtils {

	private static final String BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME =
			BeanNameUrlHandlerMapping.class.getName();

	private static final String SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME =
			SimpleControllerHandlerAdapter.class.getName();

	private static final String HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME =
			HttpRequestHandlerAdapter.class.getName();

	private static final String URL_PATH_HELPER_BEAN_NAME = "mvcUrlPathHelper";

	private static final String PATH_MATCHER_BEAN_NAME = "mvcPathMatcher";

	private static final String CORS_CONFIGURATION_BEAN_NAME = "mvcCorsConfigurations";

	private static final String HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME = "mvcHandlerMappingIntrospector";


	public static void registerDefaultComponents(ParserContext parserContext, Object source) {
		registerBeanNameUrlHandlerMapping(parserContext, source);
		registerHttpRequestHandlerAdapter(parserContext, source);
		registerSimpleControllerHandlerAdapter(parserContext, source);
		registerHandlerMappingIntrospector(parserContext, source);
	}

	/**
	 * 除非已经注册, 否则在现有的已知名称中添加别名, 或在该已知名称下注册{@link UrlPathHelper}的新实例.
	 * 
	 * @return 此{@link UrlPathHelper}实例的RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerUrlPathHelper(
			RuntimeBeanReference urlPathHelperRef, ParserContext parserContext, Object source) {

		if (urlPathHelperRef != null) {
			if (parserContext.getRegistry().isAlias(URL_PATH_HELPER_BEAN_NAME)) {
				parserContext.getRegistry().removeAlias(URL_PATH_HELPER_BEAN_NAME);
			}
			parserContext.getRegistry().registerAlias(urlPathHelperRef.getBeanName(), URL_PATH_HELPER_BEAN_NAME);
		}
		else if (!parserContext.getRegistry().isAlias(URL_PATH_HELPER_BEAN_NAME)
				&& !parserContext.getRegistry().containsBeanDefinition(URL_PATH_HELPER_BEAN_NAME)) {
			RootBeanDefinition urlPathHelperDef = new RootBeanDefinition(UrlPathHelper.class);
			urlPathHelperDef.setSource(source);
			urlPathHelperDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(URL_PATH_HELPER_BEAN_NAME, urlPathHelperDef);
			parserContext.registerComponent(new BeanComponentDefinition(urlPathHelperDef, URL_PATH_HELPER_BEAN_NAME));
		}
		return new RuntimeBeanReference(URL_PATH_HELPER_BEAN_NAME);
	}

	/**
	 * 除非已经注册, 否则在现有的已知名称中添加别名, 或在该已知名称下注册{@link PathMatcher}的新实例.
	 * 
	 * @return 此{@link PathMatcher}实例的RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerPathMatcher(
			RuntimeBeanReference pathMatcherRef, ParserContext parserContext, Object source) {

		if (pathMatcherRef != null) {
			if (parserContext.getRegistry().isAlias(PATH_MATCHER_BEAN_NAME)) {
				parserContext.getRegistry().removeAlias(PATH_MATCHER_BEAN_NAME);
			}
			parserContext.getRegistry().registerAlias(pathMatcherRef.getBeanName(), PATH_MATCHER_BEAN_NAME);
		}
		else if (!parserContext.getRegistry().isAlias(PATH_MATCHER_BEAN_NAME)
				&& !parserContext.getRegistry().containsBeanDefinition(PATH_MATCHER_BEAN_NAME)) {
			RootBeanDefinition pathMatcherDef = new RootBeanDefinition(AntPathMatcher.class);
			pathMatcherDef.setSource(source);
			pathMatcherDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			parserContext.getRegistry().registerBeanDefinition(PATH_MATCHER_BEAN_NAME, pathMatcherDef);
			parserContext.registerComponent(new BeanComponentDefinition(pathMatcherDef, PATH_MATCHER_BEAN_NAME));
		}
		return new RuntimeBeanReference(PATH_MATCHER_BEAN_NAME);
	}

	/**
	 * 除非已经注册, 否则使用已知名称注册{@link HttpRequestHandlerAdapter}.
	 */
	private static void registerBeanNameUrlHandlerMapping(ParserContext context, Object source) {
		if (!context.getRegistry().containsBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME)){
			RootBeanDefinition mappingDef = new RootBeanDefinition(BeanNameUrlHandlerMapping.class);
			mappingDef.setSource(source);
			mappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			mappingDef.getPropertyValues().add("order", 2);	// consistent with WebMvcConfigurationSupport
			RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
			mappingDef.getPropertyValues().add("corsConfigurations", corsRef);
			context.getRegistry().registerBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME, mappingDef);
			context.registerComponent(new BeanComponentDefinition(mappingDef, BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME));
		}
	}

	/**
	 * 除非已经注册, 否则使用已知名称注册{@link HttpRequestHandlerAdapter}.
	 */
	private static void registerHttpRequestHandlerAdapter(ParserContext context, Object source) {
		if (!context.getRegistry().containsBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition adapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			adapterDef.setSource(source);
			adapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			context.getRegistry().registerBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME, adapterDef);
			context.registerComponent(new BeanComponentDefinition(adapterDef, HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME));
		}
	}

	/**
	 * 除非已经注册, 否则使用已知名称注册{@link SimpleControllerHandlerAdapter}.
	 */
	private static void registerSimpleControllerHandlerAdapter(ParserContext context, Object source) {
		if (!context.getRegistry().containsBeanDefinition(SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME)) {
			RootBeanDefinition beanDef = new RootBeanDefinition(SimpleControllerHandlerAdapter.class);
			beanDef.setSource(source);
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			context.getRegistry().registerBeanDefinition(SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME, beanDef);
			context.registerComponent(new BeanComponentDefinition(beanDef, SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME));
		}
	}

	/**
	 * 除非已经注册, 否则使用已知名称注册{@code Map<String, CorsConfiguration>} (映射的{@code CorsConfiguration}s).
	 * 如果提供了非null CORS配置, 则可以更新bean定义.
	 * 
	 * @return 此{@code Map<String, CorsConfiguration>}实例的RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerCorsConfigurations(
			Map<String, CorsConfiguration> corsConfigurations, ParserContext context, Object source) {

		if (!context.getRegistry().containsBeanDefinition(CORS_CONFIGURATION_BEAN_NAME)) {
			RootBeanDefinition corsDef = new RootBeanDefinition(LinkedHashMap.class);
			corsDef.setSource(source);
			corsDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			if (corsConfigurations != null) {
				corsDef.getConstructorArgumentValues().addIndexedArgumentValue(0, corsConfigurations);
			}
			context.getReaderContext().getRegistry().registerBeanDefinition(CORS_CONFIGURATION_BEAN_NAME, corsDef);
			context.registerComponent(new BeanComponentDefinition(corsDef, CORS_CONFIGURATION_BEAN_NAME));
		}
		else if (corsConfigurations != null) {
			BeanDefinition corsDef = context.getRegistry().getBeanDefinition(CORS_CONFIGURATION_BEAN_NAME);
			corsDef.getConstructorArgumentValues().addIndexedArgumentValue(0, corsConfigurations);
		}
		return new RuntimeBeanReference(CORS_CONFIGURATION_BEAN_NAME);
	}

	/**
	 * 除非已经注册, 否则使用已知名称注册{@link HandlerMappingIntrospector}.
	 */
	private static void registerHandlerMappingIntrospector(ParserContext parserContext, Object source) {
		if (!parserContext.getRegistry().containsBeanDefinition(HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME)){
			RootBeanDefinition beanDef = new RootBeanDefinition(HandlerMappingIntrospector.class);
			beanDef.setSource(source);
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			beanDef.setLazyInit(true);
			parserContext.getRegistry().registerBeanDefinition(HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME, beanDef);
			parserContext.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME));
		}
	}

	/**
	 * 查找由{@code annotation-driven}元素创建或注册的{@code ContentNegotiationManager} bean.
	 * 
	 * @return bean定义, bean引用或{@code null}
	 */
	public static Object getContentNegotiationManager(ParserContext context) {
		String name = AnnotationDrivenBeanDefinitionParser.HANDLER_MAPPING_BEAN_NAME;
		if (context.getRegistry().containsBeanDefinition(name)) {
			BeanDefinition handlerMappingBeanDef = context.getRegistry().getBeanDefinition(name);
			return handlerMappingBeanDef.getPropertyValues().get("contentNegotiationManager");
		}
		name = AnnotationDrivenBeanDefinitionParser.CONTENT_NEGOTIATION_MANAGER_BEAN_NAME;
		if (context.getRegistry().containsBeanDefinition(name)) {
			return new RuntimeBeanReference(name);
		}
		return null;
	}
}
