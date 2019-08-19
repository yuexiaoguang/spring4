package org.springframework.web.servlet.mvc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * 处理器拦截器, 用于检查请求并准备响应.
 * 检查支持的方法和必需的会话, 并应用指定的{@link org.springframework.http.CacheControl}构建器.
 * 有关配置选项, 请参阅超类bean属性.
 *
 * <p>此拦截器支持的所有设置也可以在{@link AbstractController}上设置.
 * 此拦截器主要用于对由HandlerMapping映射的一组控制器应用检查和准备.
 */
public class WebContentInterceptor extends WebContentGenerator implements HandlerInterceptor {

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private Map<String, Integer> cacheMappings = new HashMap<String, Integer>();

	private Map<String, CacheControl> cacheControlMappings = new HashMap<String, CacheControl>();


	public WebContentInterceptor() {
		// 默认不限制HTTP方法, 特别是与带注解的控制器一起使用时...
		super(false);
	}


	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (i.e. 在web.xml中的".../*" servlet映射的情况下).
	 * 默认为"false".
	 * <p>仅与"cacheMappings"设置相关.
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * 设置是否应对上下文路径和请求URI进行URL解码.
	 * 与servlet路径相比, Servlet API都返回<i>未解码</i>.
	 * <p>根据Servlet规范 (ISO-8859-1)使用请求编码或默认编码.
	 * <p>仅与"cacheMappings"设置相关.
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * 设置用于查找路径的解析的UrlPathHelper.
	 * <p>使用此选项可以使用自定义子类覆盖默认的UrlPathHelper,
	 * 或者在多个HandlerMappings和MethodNameResolvers之间共享常用的UrlPathHelper设置.
	 * <p>仅与"cacheMappings"设置相关.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 将特定URL路径映射到特定的缓存秒数.
	 * <p>覆盖此拦截器的默认缓存秒设置. 可以指定 "-1"以从默认缓存中排除URL路径.
	 * <p>支持直接匹配, e.g. 注册的"/test" 匹配 "/test", 以及各种Ant样式模式匹配, e.g. 注册的"/t*" 匹配 "/test" 和 "/team".
	 * 有关详细信息, 请参阅AntPathMatcher javadoc.
	 * <p><b>NOTE:</b> 路径模式不应该重叠.
	 * 如果请求匹配多个映射, 则实际上未定义哪个将应用 (由于{@code java.util.Properties}中缺少键排序).
	 * 
	 * @param cacheMappings URL路径 (作为键) 和缓存秒数 (作为值, 需要是可解析的整数)之间的映射
	 */
	public void setCacheMappings(Properties cacheMappings) {
		this.cacheMappings.clear();
		Enumeration<?> propNames = cacheMappings.propertyNames();
		while (propNames.hasMoreElements()) {
			String path = (String) propNames.nextElement();
			int cacheSeconds = Integer.valueOf(cacheMappings.getProperty(path));
			this.cacheMappings.put(path, cacheSeconds);
		}
	}

	/**
	 * 将特定URL路径映射到特定的{@link org.springframework.http.CacheControl}.
	 * <p>覆盖此拦截器的默认缓存秒数设置.
	 * 可以指定一个空的{@link org.springframework.http.CacheControl}实例, 以从默认缓存中排除URL路径.
	 * <p>支持直接匹配, e.g. 注册的"/test" 匹配 "/test", 以及各种Ant样式模式匹配, e.g. 注册的"/t*" 匹配 "/test" 和 "/team".
	 * 有关详细信息, 请参阅AntPathMatcher javadoc.
	 * <p><b>NOTE:</b> 路径模式不应该重叠.
	 * 如果请求匹配多个映射, 则实际上未定义哪个将应用 (由于底层{@code java.util.HashMap}中缺少键排序).
	 * 
	 * @param cacheControl 要使用的{@code CacheControl}
	 * @param paths 将映射到给定{@code CacheControl}的URL路径
	 */
	public void addCacheMapping(CacheControl cacheControl, String... paths) {
		for (String path : paths) {
			this.cacheControlMappings.put(path, cacheControl);
		}
	}

	/**
	 * 设置用于使用注册的URL模式匹配URL路径的PathMatcher实现, 以确定缓存映射.
	 * 默认为AntPathMatcher.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		checkRequest(request);

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up cache seconds for [" + lookupPath + "]");
		}

		CacheControl cacheControl = lookupCacheControl(lookupPath);
		Integer cacheSeconds = lookupCacheSeconds(lookupPath);
		if (cacheControl != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying CacheControl to [" + lookupPath + "]");
			}
			applyCacheControl(response, cacheControl);
		}
		else if (cacheSeconds != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying CacheControl to [" + lookupPath + "]");
			}
			applyCacheSeconds(response, cacheSeconds);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying default cache seconds to [" + lookupPath + "]");
			}
			prepareResponse(response);
		}

		return true;
	}

	/**
	 * 查找给定URL路径的{@link org.springframework.http.CacheControl}实例.
	 * <p>支持直接匹配, e.g. 注册的"/test" 匹配 "/test", 以及各种Ant样式模式匹配, e.g. 注册的"/t*" 匹配 "/test" 和 "/team".
	 * 有关详细信息, 请参阅AntPathMatcher javadoc.
	 * 
	 * @param urlPath bean映射到的URL
	 * 
	 * @return 关联的{@code CacheControl}, 或{@code null}
	 */
	protected CacheControl lookupCacheControl(String urlPath) {
		// Direct match?
		CacheControl cacheControl = this.cacheControlMappings.get(urlPath);
		if (cacheControl != null) {
			return cacheControl;
		}
		// Pattern match?
		for (String registeredPath : this.cacheControlMappings.keySet()) {
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return this.cacheControlMappings.get(registeredPath);
			}
		}
		return null;
	}

	/**
	 * 查找给定URL路径的cacheSeconds整数值.
	 * <p>支持直接匹配, e.g. 注册的"/test" 匹配 "/test", 以及各种Ant样式模式匹配, e.g. 注册的"/t*" 匹配 "/test" 和 "/team".
	 * 有关详细信息, 请参阅AntPathMatcher javadoc.
	 * 
	 * @param urlPath bean映射到的URL
	 * 
	 * @return cacheSeconds整数值, 或{@code null}
	 */
	protected Integer lookupCacheSeconds(String urlPath) {
		// Direct match?
		Integer cacheSeconds = this.cacheMappings.get(urlPath);
		if (cacheSeconds != null) {
			return cacheSeconds;
		}
		// Pattern match?
		for (String registeredPath : this.cacheMappings.keySet()) {
			if (this.pathMatcher.match(registeredPath, urlPath)) {
				return this.cacheMappings.get(registeredPath);
			}
		}
		return null;
	}


	/**
	 * This implementation is empty.
	 */
	@Override
	public void postHandle(
			HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
			throws Exception {
	}

	/**
	 * This implementation is empty.
	 */
	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
	}

}
