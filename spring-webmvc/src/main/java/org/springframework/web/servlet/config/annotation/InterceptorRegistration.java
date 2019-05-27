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
 * Assists with the creation of a {@link MappedInterceptor}.
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	private final List<String> includePatterns = new ArrayList<String>();

	private final List<String> excludePatterns = new ArrayList<String>();

	private PathMatcher pathMatcher;


	/**
	 * Create an {@link InterceptorRegistration} instance.
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}


	/**
	 * Add URL patterns to which the registered interceptor should apply to.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		this.includePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * Add URL patterns to which the registered interceptor should not apply to.
	 */
	public InterceptorRegistration excludePathPatterns(String... patterns) {
		this.excludePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * A PathMatcher implementation to use with this interceptor. This is an optional,
	 * advanced property required only if using custom PathMatcher implementations
	 * that support mapping metadata other than the Ant path patterns supported
	 * by default.
	 */
	public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		return this;
	}

	/**
	 * Build the underlying interceptor. If URL patterns are provided, the returned
	 * type is {@link MappedInterceptor}; otherwise {@link HandlerInterceptor}.
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
