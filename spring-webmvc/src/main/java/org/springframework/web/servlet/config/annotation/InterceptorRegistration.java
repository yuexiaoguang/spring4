package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * 协助创建{@link MappedInterceptor}.
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	private final List<String> includePatterns = new ArrayList<String>();

	private final List<String> excludePatterns = new ArrayList<String>();

	private PathMatcher pathMatcher;


	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}


	/**
	 * 添加已注册的拦截器应应用于的URL模式.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		this.includePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * 添加注册的拦截器不应该应用于的URL模式.
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		this.excludePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * 与此拦截器一起使用的PathMatcher实现.
	 * 这是一个可选的高级属性, 仅当使用支持映射元数据的自定义PathMatcher实现时才需要, 而不是默认支持的Ant路径模式.
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * 构建底层拦截器.
	 * 如果提供了URL模式, 则返回的类型为{@link MappedInterceptor}; 否则{@link HandlerInterceptor}.
	 */
	protected Object getInterceptor() {
		if (this.includePatterns.isEmpty() && this.excludePatterns.isEmpty()) {
			return this.interceptor;
		}

		String[] include = StringUtils.toStringArray(this.includePatterns);
		String[] exclude = StringUtils.toStringArray(this.excludePatterns);
		MappedInterceptor mappedInterceptor = new MappedInterceptor(include, exclude, this.interceptor);
		if (this.pathMatcher != null) {
			mappedInterceptor.setPathMatcher(this.pathMatcher);
		}
		return mappedInterceptor;
	}

}
