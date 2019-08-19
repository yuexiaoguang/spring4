package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 包含并委托调用给{@link HandlerInterceptor}, 以及拦截器应该应用的包含 (和可选地排除) 路径模式.
 * 还提供匹配逻辑以测试拦截器是否适用于给定的请求路径.
 *
 * <p>MappedInterceptor可以直接在
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping}中注册.
 * 此外, {@code MappedInterceptor}类型的bean由{@code AbstractHandlerMethodMapping} (包括祖先的ApplicationContext)自动检测,
 * 这实际上意味着拦截器已"全局"注册到所有处理器映射.
 */
public final class MappedInterceptor implements HandlerInterceptor {

	private final String[] includePatterns;

	private final String[] excludePatterns;

	private final HandlerInterceptor interceptor;

	private PathMatcher pathMatcher;


	/**
	 * @param includePatterns 要映射的路径模式 (空以匹配所有路径)
	 * @param interceptor 映射到给定的模式的HandlerInterceptor实例
	 */
	public MappedInterceptor(String[] includePatterns, HandlerInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * @param includePatterns 要映射的路径模式 (空以匹配所有路径)
	 * @param excludePatterns 要排除的路径模式 (为空表示没有特定的排除)
	 * @param interceptor 映射到给定的模式的HandlerInterceptor实例
	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, HandlerInterceptor interceptor) {
		this.includePatterns = includePatterns;
		this.excludePatterns = excludePatterns;
		this.interceptor = interceptor;
	}


	/**
	 * @param includePatterns 要映射的路径模式 (空以匹配所有路径)
	 * @param interceptor 映射到给定的模式的WebRequestInterceptor实例
	 */
	public MappedInterceptor(String[] includePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, null, interceptor);
	}

	/**
	 * @param includePatterns 要映射的路径模式 (空以匹配所有路径)
	 * @param excludePatterns 要排除的路径模式 (为空表示没有特定的排除)
	 * @param interceptor 映射到给定的模式的WebRequestInterceptor实例
	 */
	public MappedInterceptor(String[] includePatterns, String[] excludePatterns, WebRequestInterceptor interceptor) {
		this(includePatterns, excludePatterns, new WebRequestHandlerInterceptorAdapter(interceptor));
	}


	/**
	 * 配置PathMatcher以与此MappedInterceptor一起使用,
	 * 而不是默认传递给{@link #matches(String, org.springframework.util.PathMatcher)}方法的PathMatcher.
	 * <p>这是一个高级属性, 仅在使用自定义PathMatcher实现时才需要,
	 * 该实现支持除默认支持的Ant样式路径模式之外的映射元数据.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 配置的PathMatcher, 或{@code null}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 拦截器映射到应用程序的路径.
	 */
	public String[] getPathPatterns() {
		return this.includePatterns;
	}

	/**
	 * 实际的{@link HandlerInterceptor}引用.
	 */
	public HandlerInterceptor getInterceptor() {
		return this.interceptor;
	}


	/**
	 * 确定给定查找路径的匹配项.
	 * 
	 * @param lookupPath 当前的请求路径
	 * @param pathMatcher 路径模式匹配的路径匹配器
	 */
	public boolean matches(String lookupPath, PathMatcher pathMatcher) {
		PathMatcher pathMatcherToUse = (this.pathMatcher != null ? this.pathMatcher : pathMatcher);
		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
			for (String pattern : this.excludePatterns) {
				if (pathMatcherToUse.match(pattern, lookupPath)) {
					return false;
				}
			}
		}
		if (ObjectUtils.isEmpty(this.includePatterns)) {
			return true;
		}
		for (String pattern : this.includePatterns) {
			if (pathMatcherToUse.match(pattern, lookupPath)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return this.interceptor.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		this.interceptor.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {

		this.interceptor.afterCompletion(request, response, handler, ex);
	}

}
