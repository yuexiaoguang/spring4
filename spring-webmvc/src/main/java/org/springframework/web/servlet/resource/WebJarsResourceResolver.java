package org.springframework.web.servlet.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.webjars.WebJarAssetLocator;

import org.springframework.core.io.Resource;

/**
 * {@code ResourceResolver}, 它委托给链来查找资源, 然后尝试查找WebJar JAR文件中包含的匹配版本的资源.
 *
 * <p>这允许WebJars.org用户在其模板中写入与版本无关的路径, 例如{@code <script src="/jquery/jquery.min.js"/>}.
 * 此路径将解析为唯一版本 {@code <script src="/jquery/1.2.0/jquery.min.js"/>},
 * 这更适合应用程序中的HTTP缓存和版本管理.
 *
 * <p>这也解析了与版本无关的HTTP请求 {@code "GET /jquery/jquery.min.js"}的资源.
 *
 * <p>此解析器需要类路径上的 "org.webjars:webjars-locator"库, 并且如果该库存在则会自动注册.
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	private final static String WEBJARS_LOCATION = "META-INF/resources/webjars/";

	private final static int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();


	private final WebJarAssetLocator webJarAssetLocator;


	/**
	 * 使用默认的{@code WebJarAssetLocator}实例创建{@code WebJarsResourceResolver}.
	 */
	public WebJarsResourceResolver() {
		this(new WebJarAssetLocator());
	}

	/**
	 * 使用自定义{@code WebJarAssetLocator}实例创建{@code WebJarsResourceResolver}, e.g. 使用自定义索引.
	 */
	public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
		this.webJarAssetLocator = webJarAssetLocator;
	}


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved == null) {
			String webJarResourcePath = findWebJarResourcePath(requestPath);
			if (webJarResourcePath != null) {
				return chain.resolveResource(request, webJarResourcePath, locations);
			}
		}
		return resolved;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		if (path == null) {
			String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
			if (webJarResourcePath != null) {
				return chain.resolveUrlPath(webJarResourcePath, locations);
			}
		}
		return path;
	}

	protected String findWebJarResourcePath(String path) {
		int startOffset = (path.startsWith("/") ? 1 : 0);
		int endOffset = path.indexOf('/', 1);
		if (endOffset != -1) {
			String webjar = path.substring(startOffset, endOffset);
			String partialPath = path.substring(endOffset + 1);
			String webJarPath = webJarAssetLocator.getFullPathExact(webjar, partialPath);
			if (webJarPath != null) {
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		return null;
	}

}
