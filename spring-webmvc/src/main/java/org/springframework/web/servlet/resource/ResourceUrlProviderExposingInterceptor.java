package org.springframework.web.servlet.resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * 一个拦截器, 它公开了它配置的{@link ResourceUrlProvider}实例作为请求属性.
 */
public class ResourceUrlProviderExposingInterceptor extends HandlerInterceptorAdapter {

	/**
	 * 包含{@link ResourceUrlProvider}的请求属性的名称.
	 */
	public static final String RESOURCE_URL_PROVIDER_ATTR = ResourceUrlProvider.class.getName();

	private final ResourceUrlProvider resourceUrlProvider;


	public ResourceUrlProviderExposingInterceptor(ResourceUrlProvider resourceUrlProvider) {
		Assert.notNull(resourceUrlProvider, "ResourceUrlProvider is required");
		this.resourceUrlProvider = resourceUrlProvider;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		request.setAttribute(RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		return true;
	}

}
