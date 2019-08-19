package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerExecutionChain;

/**
 * URL映射的{@link org.springframework.web.servlet.HandlerMapping}实现的抽象基类.
 * 提供将处理器映射到URL和可配置URL查找的基础结构.
 * 有关后者的信息, 请参阅"alwaysUseFullPath"属性.
 *
 * <p>支持直接匹配, e.g. 注册的"/test"匹配"/test",
 * 以及各种Ant样式模式匹配, e.g. 注册的"/t*"模式匹配"/test"和"/team",
 * "/test/*"匹配"/test"录中的所有路径, "/test/**"匹配"/test"下面的所有路径.
 * 有关详细信息, 请参阅{@link org.springframework.util.AntPathMatcher AntPathMatcher} javadoc.
 *
 * <p>将搜索所有路径模式以查找当前请求路径的最精确匹配.
 * 最精确的匹配被定义为与当前请求路径匹配的最长路径模式.
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping implements MatchableHandlerMapping {

	private Object rootHandler;

	private boolean useTrailingSlashMatch = false;

	private boolean lazyInitHandlers = false;

	private final Map<String, Object> handlerMap = new LinkedHashMap<String, Object>();


	/**
	 * 设置此处理器映射的根处理器, 即要为根路径 ("/")注册的处理器.
	 * <p>默认为{@code null}, 表示没有根处理器.
	 */
	public void setRootHandler(Object rootHandler) {
		this.rootHandler = rootHandler;
	}

	/**
	 * 返回此处理器映射的根处理器 (注册为"/"), 或{@code null}.
	 */
	public Object getRootHandler() {
		return this.rootHandler;
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 * 如果启用, 则"/users"等URL模式也与"/users/"匹配.
	 * <p>默认为{@code false}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}

	/**
	 * 是否匹配URL而不管是否存在尾部斜杠.
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 设置是否延迟初始化处理器.
	 * 仅适用于单例处理器, 因为原型总是被延迟初始化.
	 * 默认为"false", 因为实时初始化可以通过直接引用控制器对象来提高效率.
	 * <p>如果要允许延迟初始化控制器, 将它们设置为"lazy-init"并将此标志设置为true.
	 * 只是使它们成为"lazy-init"是行不通的, 因为在这种情况下它们是通过处理器映射的引用初始化的.
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * 查找给定请求的URL路径的处理器.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 处理器实例, 或{@code null}
	 */
	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		Object handler = lookupHandler(lookupPath, request);
		if (handler == null) {
			// 需要直接关注默认处理器, 因为还需要为它公开PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE.
			Object rawHandler = null;
			if ("/".equals(lookupPath)) {
				rawHandler = getRootHandler();
			}
			if (rawHandler == null) {
				rawHandler = getDefaultHandler();
			}
			if (rawHandler != null) {
				// Bean名称或已解析的处理器?
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = getApplicationContext().getBean(handlerName);
				}
				validateHandler(rawHandler, request);
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		if (handler != null && logger.isDebugEnabled()) {
			logger.debug("Mapping [" + lookupPath + "] to " + handler);
		}
		else if (handler == null && logger.isTraceEnabled()) {
			logger.trace("No handler mapping found for [" + lookupPath + "]");
		}
		return handler;
	}

	/**
	 * 查找给定URL路径的处理器实例.
	 * <p>支持直接匹配, e.g. 注册的"/test"匹配"/test",
	 * 以及各种Ant样式模式匹配, e.g. 注册的"/t*"匹配"/test"和"/team".
	 * 有关详细信息, 请参阅AntPathMatcher类.
	 * <p>寻找最精确的模式, 其中最精确的是被定义为最长的路径模式.
	 * 
	 * @param urlPath bean映射到的URL
	 * @param request 当前的HTTP请求 (用于公开映射中的路径)
	 * 
	 * @return 关联的处理器实例, 或{@code null}
	 */
	protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
		// Direct match?
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}

		// Pattern match?
		List<String> matchingPatterns = new ArrayList<String>();
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, urlPath)) {
				matchingPatterns.add(registeredPattern);
			}
			else if (useTrailingSlashMatch()) {
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", urlPath)) {
					matchingPatterns.add(registeredPattern +"/");
				}
			}
		}

		String bestMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
		if (!matchingPatterns.isEmpty()) {
			Collections.sort(matchingPatterns, patternComparator);
			if (logger.isDebugEnabled()) {
				logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
			}
			bestMatch = matchingPatterns.get(0);
		}
		if (bestMatch != null) {
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				if (bestMatch.endsWith("/")) {
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = getApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, urlPath);

			// 可能存在多个'最佳模式', 确保所有这些都具有正确的URI模板变量
			Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
			}
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// No handler found...
		return null;
	}

	/**
	 * 根据当前请求验证给定的处理器.
	 * <p>默认实现为空. 可以在子类中重写, 例如, 强制执行URL映射中表示的特定前提条件.
	 * 
	 * @param handler 要验证的处理器对象
	 * @param request 当前的HTTP请求
	 * 
	 * @throws Exception 如果验证失败
	 */
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
	}

	/**
	 * 为给定的原始处理器构建处理器对象, 在执行处理器之前, 公开实际的处理器,
	 * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}, 以及{@link #URI_TEMPLATE_VARIABLES_ATTRIBUTE}.
	 * <p>默认实现使用特殊拦截器构建{@link HandlerExecutionChain}, 该拦截器公开path属性和uri模板变量
	 * 
	 * @param rawHandler 要公开的原始处理器
	 * @param pathWithinMapping 在执行处理器之前公开的路径
	 * @param uriTemplateVariables URI模板变量, 可以是{@code null}
	 * 
	 * @return 最终的处理器对象
	 */
	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, Map<String, String> uriTemplateVariables) {

		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

	/**
	 * 将当前映射中的路径公开为请求属性.
	 * 
	 * @param pathWithinMapping 当前映射中的路径
	 * @param request 公开路径的请求
	 */
	protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping, HttpServletRequest request) {
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
		request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
	}

	/**
	 * 将URI模板变量公开为请求属性.
	 * 
	 * @param uriTemplateVariables URI模板变量
	 * @param request 公开路径的请求
	 */
	protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		if (getPathMatcher().match(pattern, lookupPath)) {
			return new RequestMatchResult(pattern, lookupPath, getPathMatcher());
		}
		else if (useTrailingSlashMatch()) {
			if (!pattern.endsWith("/") && getPathMatcher().match(pattern + "/", lookupPath)) {
				return new RequestMatchResult(pattern + "/", lookupPath, getPathMatcher());
			}
		}
		return null;
	}

	/**
	 * 为给定的URL路径注册指定的处理器.
	 * 
	 * @param urlPaths bean应该映射到的URL
	 * @param beanName 处理器bean的名称
	 * 
	 * @throws BeansException 如果处理器无法注册
	 * @throws IllegalStateException 如果注册的处理器存在冲突
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * 注册给定URL路径的指定处理器.
	 * 
	 * @param urlPath bean应该映射到的URL
	 * @param handler 处理器实例或处理器bean名称String (bean名称将自动解析为相应的处理器bean)
	 * 
	 * @throws BeansException 如果处理器无法注册
	 * @throws IllegalStateException 如果注册的处理器存在冲突
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// 如果通过名称引用singleton, 则实时解析处理器.
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			if (getApplicationContext().isSingleton(handlerName)) {
				resolvedHandler = getApplicationContext().getBean(handlerName);
			}
		}

		Object mappedHandler = this.handlerMap.get(urlPath);
		if (mappedHandler != null) {
			if (mappedHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
						"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
			}
		}
		else {
			if (urlPath.equals("/")) {
				if (logger.isInfoEnabled()) {
					logger.info("Root mapping to " + getHandlerDescription(handler));
				}
				setRootHandler(resolvedHandler);
			}
			else if (urlPath.equals("/*")) {
				if (logger.isInfoEnabled()) {
					logger.info("Default mapping to " + getHandlerDescription(handler));
				}
				setDefaultHandler(resolvedHandler);
			}
			else {
				this.handlerMap.put(urlPath, resolvedHandler);
				if (logger.isInfoEnabled()) {
					logger.info("Mapped URL path [" + urlPath + "] onto " + getHandlerDescription(handler));
				}
			}
		}
	}

	private String getHandlerDescription(Object handler) {
		return "handler " + (handler instanceof String ? "'" + handler + "'" : "of type [" + handler.getClass() + "]");
	}


	/**
	 * 将注册的处理器作为不可修改的Map返回, 将注册的路径作为键, 将处理器对象 (或lazy-init处理器中的处理器bean名称)作为值.
	 */
	public final Map<String, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * 指示此处理器映射是否支持类型级别映射. 默认为{@code false}.
	 */
	protected boolean supportsTypeLevelMappings() {
		return false;
	}


	/**
	 * 用于公开{@link AbstractUrlHandlerMapping#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}属性的特殊拦截器.
	 */
	private class PathExposingHandlerInterceptor extends HandlerInterceptorAdapter {

		private final String bestMatchingPattern;

		private final String pathWithinMapping;

		public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
			this.bestMatchingPattern = bestMatchingPattern;
			this.pathWithinMapping = pathWithinMapping;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
			request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
			return true;
		}

	}

	/**
	 * 用于公开{@link AbstractUrlHandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}属性的特殊拦截器.
	 */
	private class UriTemplateVariablesHandlerInterceptor extends HandlerInterceptorAdapter {

		private final Map<String, String> uriTemplateVariables;

		public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
			this.uriTemplateVariables = uriTemplateVariables;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposeUriTemplateVariables(this.uriTemplateVariables, request);
			return true;
		}
	}

}
