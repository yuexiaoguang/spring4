package org.springframework.web.servlet.resource;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * {@code ResourceTransformer}的基类, 带有可选的帮助方法, 用于解析转换后的资源中的公共链接.
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * 配置{@link ResourceUrlProvider}以在解析转换后的资源中的链接的公共URL时使用 (e.g. CSS文件中的import链接).
	 * 这仅对表示为完整路径的链接是必需的, i.e. 包括上下文和servlet路径, 而不是相对链接.
	 * <p>默认未设置此属性.
	 * 在这种情况下, 如果需要{@code ResourceUrlProvider}, 则尝试查找通过
	 * {@link org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor
	 * ResourceUrlProviderExposingInterceptor}公开的{@code ResourceUrlProvider} (默认情况下在MVC Java配置和XML命名空间中配置).
	 * 因此, 在大多数情况下, 不需要显式配置此属性.
	 * 
	 * @param resourceUrlProvider 要使用的URL提供器
	 */
	public void setResourceUrlProvider(ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * 返回配置的{@code ResourceUrlProvider}.
	 */
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * 当转换的资源包含指向其他资源的链接时, 转换器可以使用此方法.
	 * 这些链接需要由资源解析器链确定的公共的链接替换 (e.g. 公共URL可能插入了版本).
	 * 
	 * @param resourcePath 需要重写的资源的路径
	 * @param request 当前的请求
	 * @param resource 要转换的资源
	 * @param transformerChain 转换器链
	 * 
	 * @return 转换后的URL, 或{@code null}
	 */
	protected String resolveUrlPath(String resourcePath, HttpServletRequest request,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// 完整的资源路径
			ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
			return (urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null);
		}
		else {
			// 尝试解析为相对路径
			return transformerChain.getResolverChain().resolveUrlPath(
					resourcePath, Collections.singletonList(resource));
		}
	}

	private ResourceUrlProvider findResourceUrlProvider(HttpServletRequest request) {
		if (this.resourceUrlProvider != null) {
			return this.resourceUrlProvider;
		}
		return (ResourceUrlProvider) request.getAttribute(
				ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
	}

}
