package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * 获取客户端用于访问静态资源的公共URL路径的中心组件.
 *
 * <p>此类了解用于提供静态资源的Spring MVC处理器映射,
 * 并使用配置的{@code ResourceHttpRequestHandler}的{@code ResourceResolver}链做出决策.
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent> {

	protected final Log logger = LogFactory.getLog(getClass());

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<String, ResourceHttpRequestHandler>();

	private boolean autodetect = true;


	/**
	 * 配置{@code UrlPathHelper}以在{@link #getForRequestUrl(javax.servlet.http.HttpServletRequest, String)}中使用,
	 * 以便为目标请求URL路径派生查找路径.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回配置的{@code UrlPathHelper}.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * @deprecated as of Spring 4.2.8, in favor of {@link #getUrlPathHelper}
	 */
	@Deprecated
	public UrlPathHelper getPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 配置{@code PathMatcher}以在将目标查找路径与资源映射进行比较时使用.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回配置的{@code PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 手动配置资源映射.
	 * <p><strong>Note:</strong> 默认情况下, 资源映射是从Spring {@code ApplicationContext}自动检测的.
	 * 但是, 如果使用此属性, 则会关闭自动检测.
	 */
	public void setHandlerMap(Map<String, ResourceHttpRequestHandler> handlerMap) {
		if (handlerMap != null) {
			this.handlerMap.clear();
			this.handlerMap.putAll(handlerMap);
			this.autodetect = false;
		}
	}

	/**
	 * 返回资源映射, 手动配置的或在刷新Spring {@code ApplicationContext}时自动检测的.
	 */
	public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
		return this.handlerMap;
	}

	/**
	 * 如果手动配置了资源映射, 则返回{@code false}, 否则返回{@code true}.
	 */
	public boolean isAutodetect() {
		return this.autodetect;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (isAutodetect()) {
			this.handlerMap.clear();
			detectResourceHandlers(event.getApplicationContext());
			if (this.handlerMap.isEmpty() && logger.isDebugEnabled()) {
				logger.debug("No resource handling mappings found");
			}
			if (!this.handlerMap.isEmpty()) {
				this.autodetect = false;
			}
		}
	}


	protected void detectResourceHandlers(ApplicationContext appContext) {
		logger.debug("Looking for resource handler mappings");

		Map<String, SimpleUrlHandlerMapping> beans = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<SimpleUrlHandlerMapping>(beans.values());
		AnnotationAwareOrderComparator.sort(mappings);

		for (SimpleUrlHandlerMapping mapping : mappings) {
			for (String pattern : mapping.getHandlerMap().keySet()) {
				Object handler = mapping.getHandlerMap().get(pattern);
				if (handler instanceof ResourceHttpRequestHandler) {
					ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
					if (logger.isDebugEnabled()) {
						logger.debug("Found resource handler mapping: URL pattern=\"" + pattern + "\", " +
								"locations=" + resourceHandler.getLocations() + ", " +
								"resolvers=" + resourceHandler.getResourceResolvers());
					}
					this.handlerMap.put(pattern, resourceHandler);
				}
			}
		}
	}

	/**
	 * {@link #getForLookupPath(String)}的变体, 它接受完整的请求URL路径 (i.e. 包括上下文和servlet路径),
	 * 并返回完整的请求URL路径以公开.
	 * 
	 * @param request 当前的请求
	 * @param requestUrl 要解析的请求URL路径
	 * 
	 * @return 已解析的公用URL路径, 或{@code null}
	 */
	public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for request URL \"" + requestUrl + "\"");
		}
		int prefixIndex = getLookupPathIndex(request);
		int suffixIndex = getEndPathIndex(requestUrl);
		if (prefixIndex >= suffixIndex) {
			return null;
		}
		String prefix = requestUrl.substring(0, prefixIndex);
		String suffix = requestUrl.substring(suffixIndex);
		String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
		String resolvedLookupPath = getForLookupPath(lookupPath);
		return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
	}

	private int getLookupPathIndex(HttpServletRequest request) {
		UrlPathHelper pathHelper = getUrlPathHelper();
		String requestUri = pathHelper.getRequestUri(request);
		String lookupPath = pathHelper.getLookupPathForRequest(request);
		return requestUri.indexOf(lookupPath);
	}

	private int getEndPathIndex(String lookupPath) {
		int suffixIndex = lookupPath.length();
		int queryIndex = lookupPath.indexOf('?');
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		int hashIndex = lookupPath.indexOf('#');
		if (hashIndex > 0) {
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}
		return suffixIndex;
	}

	/**
	 * 将给定路径与已配置的资源处理器映射进行比较, 如果找到匹配项,
	 * 使用匹配的{@code ResourceHttpRequestHandler}的{@code ResourceResolver}链解析要公开的URL路径.
	 * <p>期望给定路径将用于Spring MVC请求映射, i.e. 排除上下文和servlet路径部分.
	 * <p>如果多个处理器映射匹配, 则使用的处理器将是配置了最具体模式的处理器.
	 * 
	 * @param lookupPath 要检查的查找路径
	 * 
	 * @return 已解析的公用URL路径, 或{@code null}
	 */
	public final String getForLookupPath(String lookupPath) {
		if (logger.isTraceEnabled()) {
			logger.trace("Getting resource URL for lookup path \"" + lookupPath + "\"");
		}

		List<String> matchingPatterns = new ArrayList<String>();
		for (String pattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(pattern, lookupPath)) {
				matchingPatterns.add(pattern);
			}
		}

		if (!matchingPatterns.isEmpty()) {
			Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
			Collections.sort(matchingPatterns, patternComparator);
			for (String pattern : matchingPatterns) {
				String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
				String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
				if (logger.isTraceEnabled()) {
					logger.trace("Invoking ResourceResolverChain for URL pattern \"" + pattern + "\"");
				}
				ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
				ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
				String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
				if (resolved == null) {
					continue;
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Resolved public resource URL path \"" + resolved + "\"");
				}
				return pathMapping + resolved;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("No matching resource mapping for lookup path \"" + lookupPath + "\"");
		}
		return null;
	}

}
