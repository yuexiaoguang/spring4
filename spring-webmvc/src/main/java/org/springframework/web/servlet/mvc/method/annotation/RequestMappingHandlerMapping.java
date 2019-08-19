package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * 从{@link Controller @Controller}类中的类型和方法级{@link RequestMapping @RequestMapping}注解创建{@link RequestMappingInfo}实例.
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {

	private boolean useSuffixPatternMatch = true;

	private boolean useRegisteredSuffixPatternMatch = false;

	private boolean useTrailingSlashMatch = true;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private StringValueResolver embeddedValueResolver;

	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * 在将模式与请求匹配时是否使用后缀模式匹配 (".*").
	 * 如果启用, 映射到"/users"的方法也匹配"/users.*".
	 * <p>默认为{@code true}.
	 * <p>另请参阅{@link #setUseRegisteredSuffixPatternMatch(boolean)}以获得对允许的特定后缀的更细粒度控制.
	 */
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * 后缀模式匹配是否仅适用于在{@link ContentNegotiationManager}中明确注册的路径扩展.
	 * 通常建议这样做以减少歧义, 并避免诸如路径中的 "."之类的问题.
	 * <p>默认为"false".
	 */
	public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 * 如果启用, 映射到"/users"的方法也匹配"/users/".
	 * <p>默认为{@code true}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}

	/**
	 * 设置用于确定请求的媒体类型的{@link ContentNegotiationManager}.
	 * 如果未设置, 则使用默认构造函数.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的{@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void afterPropertiesSet() {
		this.config = new RequestMappingInfo.BuilderConfiguration();
		this.config.setUrlPathHelper(getUrlPathHelper());
		this.config.setPathMatcher(getPathMatcher());
		this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
		this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
		this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
		this.config.setContentNegotiationManager(getContentNegotiationManager());

		super.afterPropertiesSet();
	}


	/**
	 * 是否使用后缀模式匹配.
	 */
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * 是否使用已注册的后缀进行模式匹配.
	 */
	public boolean useRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 返回用于后缀模式匹配的文件扩展名.
	 */
	public List<String> getFileExtensions() {
		return this.config.getFileExtensions();
	}


	/**
	 * {@inheritDoc}
	 * <p>期望处理器具有类型级别 @{@link Controller}注解或类型级别 @{@link RequestMapping}注解.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * 使用方法和类型级别 @{@link RequestMapping}注解来创建RequestMappingInfo.
	 * 
	 * @return 创建的 RequestMappingInfo, 或{@code null} 如果方法没有{@code @RequestMapping}注解.
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
			}
		}
		return info;
	}

	/**
	 * 委托给{@link #createRequestMappingInfo(RequestMapping, RequestCondition)},
	 * 提供适当的自定义{@link RequestCondition}, 具体取决于提供的{@code annotatedElement}是否为类或方法.
	 */
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		RequestCondition<?> condition = (element instanceof Class ?
				getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
	}

	/**
	 * 提供自定义类型级别请求条件.
	 * 自定义的{@link RequestCondition}可以是任何类型,
	 * 只要从对此方法的所有调用返回相同的条件类型, 以确保可以组合和比较自定义请求条件.
	 * <p>考虑为自定义条件类型扩展{@link AbstractRequestCondition},
	 * 并使用{@link CompositeRequestCondition}提供多个自定义条件.
	 * 
	 * @param handlerType 要为其创建条件的处理器类型
	 * 
	 * @return 条件, 或{@code null}
	 */
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * 提供自定义方法级别请求条件.
	 * 自定义的{@link RequestCondition}可以是任何类型,
	 * 只要从对此方法的所有调用返回相同的条件类型, 以确保可以组合和比较自定义请求条件.
	 * <p>考虑为自定义条件类型扩展{@link AbstractRequestCondition},
	 * 并使用{@link CompositeRequestCondition}提供多个自定义条件.
	 * 
	 * @param method 要为其创建条件的处理器方法
	 * 
	 * @return 条件, 或{@code null}
	 */
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * 从提供的{@link RequestMapping @RequestMapping}注解创建{@link RequestMappingInfo},
	 * 注解可以是直接声明的注解, 元注解, 也可以是在注解层次结构中合并注解属性的综合结果.
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, RequestCondition<?> customCondition) {

		return RequestMappingInfo
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.mappingName(requestMapping.name())
				.customCondition(customCondition)
				.options(this.config)
				.build();
	}

	/**
	 * 解析给定的模式数组中占位符值.
	 * 
	 * @return 新的数组
	 */
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		}
		else {
			String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			return resolvedPatterns;
		}
	}

	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
		RequestMappingInfo matchingInfo = info.getMatchingCondition(request);
		if (matchingInfo == null) {
			return null;
		}
		Set<String> patterns = matchingInfo.getPatternsCondition().getPatterns();
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		return new RequestMatchResult(patterns.iterator().next(), lookupPath, getPathMatcher());
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		CorsConfiguration config = new CorsConfiguration();
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		return config.applyPermitDefaultValues();
	}

	private void updateCorsConfig(CorsConfiguration config, CrossOrigin annotation) {
		if (annotation == null) {
			return;
		}
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		}
		else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		}
		else if (!allowCredentials.isEmpty()) {
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		if (annotation.maxAge() >= 0 && config.getMaxAge() == null) {
			config.setMaxAge(annotation.maxAge());
		}
	}

	private String resolveCorsAnnotationValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}

}
