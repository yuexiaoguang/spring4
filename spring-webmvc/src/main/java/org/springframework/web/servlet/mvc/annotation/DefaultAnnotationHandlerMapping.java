package org.springframework.web.servlet.mvc.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.AbstractDetectingUrlHandlerMapping;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}接口的实现,
 * 基于类型或方法级别的{@link RequestMapping}注解表示的HTTP路径映射处理器.
 *
 * <p>在Java 5+上默认注册在{@link org.springframework.web.servlet.DispatcherServlet}.
 * <b>NOTE:</b> 如果在DispatcherServlet上下文中定义自定义HandlerMapping bean,
 * 则需要显式添加DefaultAnnotationHandlerMapping bean, 因为自定义HandlerMapping bean会替换默认映射策略.
 * 定义还允许注册自定义拦截器的DefaultAnnotationHandlerMapping:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     ...
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 带注解的控制器通常在类型级别使用{@link Controller}标记.
 * 当在类型级别应用{@link RequestMapping}时, 这不是绝对必要的
 * (因为这样的处理器通常实现{@link org.springframework.web.servlet.mvc.Controller}接口).
 * 但是, 如果{@link RequestMapping}在类型级别不存在,
 * 则需要{@link Controller}来检测方法级别的{@link RequestMapping}注解.
 *
 * <p><b>NOTE:</b> 方法级映射仅允许缩小在类级别表示的映射.
 * HTTP路径需要唯一地映射到特定的处理器bean, 任何给定的HTTP路径只允许映射到一个特定的处理器bean (不分布在多个处理器bean中).
 * 强烈建议将相关的处理器方法放在同一个bean中.
 *
 * <p>{@link AnnotationMethodHandlerAdapter}负责处理带注解的处理器方法, 由此HandlerMapping映射.
 * 对于类型级别的{@link RequestMapping}, 特定的HandlerAdapter适用, 例如
 * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}.
 *
 * @deprecated as of Spring 3.2, in favor of
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping RequestMappingHandlerMapping}
 */
@Deprecated
public class DefaultAnnotationHandlerMapping extends AbstractDetectingUrlHandlerMapping {

	static final String USE_DEFAULT_SUFFIX_PATTERN = DefaultAnnotationHandlerMapping.class.getName() + ".useDefaultSuffixPattern";

	private boolean useDefaultSuffixPattern = true;

	private final Map<Class<?>, RequestMapping> cachedMappings = new HashMap<Class<?>, RequestMapping>();


	/**
	 * Set whether to register paths using the default suffix pattern as well:
	 * i.e. whether "/users" should be registered as "/users.*" and "/users/" too.
	 * <p>Default is "true". Turn this convention off if you intend to interpret
	 * your {@code @RequestMapping} paths strictly.
	 * <p>Note that paths which include a ".xxx" suffix or end with "/" already will not be
	 * transformed using the default suffix pattern in any case.
	 */
	public void setUseDefaultSuffixPattern(boolean useDefaultSuffixPattern) {
		this.useDefaultSuffixPattern = useDefaultSuffixPattern;
	}


	/**
	 * Checks for presence of the {@link org.springframework.web.bind.annotation.RequestMapping}
	 * annotation on the handler class and on any of its methods.
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		ApplicationContext context = getApplicationContext();
		Class<?> handlerType = context.getType(beanName);
		RequestMapping mapping = context.findAnnotationOnBean(beanName, RequestMapping.class);
		if (mapping != null) {
			// @RequestMapping found at type level
			this.cachedMappings.put(handlerType, mapping);
			Set<String> urls = new LinkedHashSet<String>();
			String[] typeLevelPatterns = mapping.value();
			if (typeLevelPatterns.length > 0) {
				// @RequestMapping specifies paths at type level
				String[] methodLevelPatterns = determineUrlsForHandlerMethods(handlerType, true);
				for (String typeLevelPattern : typeLevelPatterns) {
					if (!typeLevelPattern.startsWith("/")) {
						typeLevelPattern = "/" + typeLevelPattern;
					}
					boolean hasEmptyMethodLevelMappings = false;
					for (String methodLevelPattern : methodLevelPatterns) {
						if (methodLevelPattern == null) {
							hasEmptyMethodLevelMappings = true;
						}
						else {
							String combinedPattern = getPathMatcher().combine(typeLevelPattern, methodLevelPattern);
							addUrlsForPath(urls, combinedPattern);
						}
					}
					if (hasEmptyMethodLevelMappings ||
							org.springframework.web.servlet.mvc.Controller.class.isAssignableFrom(handlerType)) {
						addUrlsForPath(urls, typeLevelPattern);
					}
				}
				return StringUtils.toStringArray(urls);
			}
			else {
				// actual paths specified by @RequestMapping at method level
				return determineUrlsForHandlerMethods(handlerType, false);
			}
		}
		else if (AnnotationUtils.findAnnotation(handlerType, Controller.class) != null) {
			// @RequestMapping to be introspected at method level
			return determineUrlsForHandlerMethods(handlerType, false);
		}
		else {
			return null;
		}
	}

	/**
	 * Derive URL mappings from the handler's method-level mappings.
	 * @param handlerType the handler type to introspect
	 * @param hasTypeLevelMapping whether the method-level mappings are nested
	 * within a type-level mapping
	 * @return the array of mapped URLs
	 */
	protected String[] determineUrlsForHandlerMethods(Class<?> handlerType, final boolean hasTypeLevelMapping) {
		String[] subclassResult = determineUrlsForHandlerMethods(handlerType);
		if (subclassResult != null) {
			return subclassResult;
		}

		final Set<String> urls = new LinkedHashSet<String>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
		handlerTypes.add(handlerType);
		handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
		for (Class<?> currentHandlerType : handlerTypes) {
			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) {
					RequestMapping mapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
					if (mapping != null) {
						String[] mappedPatterns = mapping.value();
						if (mappedPatterns.length > 0) {
							for (String mappedPattern : mappedPatterns) {
								if (!hasTypeLevelMapping && !mappedPattern.startsWith("/")) {
									mappedPattern = "/" + mappedPattern;
								}
								addUrlsForPath(urls, mappedPattern);
							}
						}
						else if (hasTypeLevelMapping) {
							// empty method-level RequestMapping
							urls.add(null);
						}
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		return StringUtils.toStringArray(urls);
	}

	/**
	 * Derive URL mappings from the handler's method-level mappings.
	 * @param handlerType the handler type to introspect
	 * @return the array of mapped URLs
	 */
	protected String[] determineUrlsForHandlerMethods(Class<?> handlerType) {
		return null;
	}

	/**
	 * Add URLs and/or URL patterns for the given path.
	 * @param urls the Set of URLs for the current bean
	 * @param path the currently introspected path
	 */
	protected void addUrlsForPath(Set<String> urls, String path) {
		urls.add(path);
		if (this.useDefaultSuffixPattern && path.indexOf('.') == -1 && !path.endsWith("/")) {
			urls.add(path + ".*");
			urls.add(path + "/");
		}
	}


	/**
	 * Validate the given annotated handler against the current request.
	 * @see #validateMapping
	 */
	@Override
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
		RequestMapping mapping = this.cachedMappings.get(handler.getClass());
		if (mapping == null) {
			mapping = AnnotationUtils.findAnnotation(handler.getClass(), RequestMapping.class);
		}
		if (mapping != null) {
			validateMapping(mapping, request);
		}
		request.setAttribute(USE_DEFAULT_SUFFIX_PATTERN, this.useDefaultSuffixPattern);
	}

	/**
	 * Validate the given type-level mapping metadata against the current request,
	 * checking HTTP request method and parameter conditions.
	 * @param mapping the mapping metadata to validate
	 * @param request current HTTP request
	 * @throws Exception if validation failed
	 */
	protected void validateMapping(RequestMapping mapping, HttpServletRequest request) throws Exception {
		RequestMethod[] mappedMethods = mapping.method();
		if (!ServletAnnotationMappingUtils.checkRequestMethod(mappedMethods, request)) {
			String[] supportedMethods = new String[mappedMethods.length];
			for (int i = 0; i < mappedMethods.length; i++) {
				supportedMethods[i] = mappedMethods[i].name();
			}
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), supportedMethods);
		}

		String[] mappedParams = mapping.params();
		if (!ServletAnnotationMappingUtils.checkParameters(mappedParams, request)) {
			throw new UnsatisfiedServletRequestParameterException(mappedParams, request.getParameterMap());
		}

		String[] mappedHeaders = mapping.headers();
		if (!ServletAnnotationMappingUtils.checkHeaders(mappedHeaders, request)) {
			throw new ServletRequestBindingException("Header conditions \"" +
					StringUtils.arrayToDelimitedString(mappedHeaders, ", ") +
					"\" not met for actual request");
		}
	}

	@Override
	protected boolean supportsTypeLevelMappings() {
		return true;
	}
}
