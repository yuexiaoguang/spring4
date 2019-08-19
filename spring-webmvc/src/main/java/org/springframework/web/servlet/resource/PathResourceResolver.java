package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 一个简单的{@code ResourceResolver}, 它尝试在与请求路径匹配的给定位置下查找资源.
 *
 * <p>此解析器不会委托给{@code ResourceResolverChain}, 并且预计将在解析器链中的最后配置.
 */
public class PathResourceResolver extends AbstractResourceResolver {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private Resource[] allowedLocations;

	private final Map<Resource, Charset> locationCharsets = new HashMap<Resource, Charset>(4);

	private UrlPathHelper urlPathHelper;


	/**
	 * 默认情况下, 在找到资源时, 会比较已解析的资源的路径, 以确保它位于找到资源的输入位置下.
	 * 然而, 有时情况可能并非如此, e.g. 当
	 * {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}
	 * 解析它包含的链接的公共URL时, CSS文件就是位置, 正在解析的资源是css文件, 图像, 字体以及位于相邻或父目录中的其他文件.
	 * <p>此属性允许配置资源必须在其下的完整位置列表, 以便如果资源不在相对于其找到的位置下, 则也可以检查此列表.
	 * <p>默认情况下, {@link ResourceHttpRequestHandler}初始化此属性以匹配其位置列表.
	 * 
	 * @param locations 允许的位置列表
	 */
	public void setAllowedLocations(Resource... locations) {
		this.allowedLocations = locations;
	}

	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}

	/**
	 * 配置与位置关联的字符集.
	 * 如果在{@link org.springframework.core.io.UrlResource URL资源}位置下找到静态资源, 则使用charset对相对路径进行编码.
	 * <p><strong>Note:</strong> 只有在配置了{@link #setUrlPathHelper urlPathHelper}属性,
	 * 且其{@code urlDecode}属性设置为true时才使用charset.
	 */
	public void setLocationCharsets(Map<Resource, Charset> locationCharsets) {
		this.locationCharsets.clear();
		this.locationCharsets.putAll(locationCharsets);
	}

	/**
	 * 返回与静态资源位置关联的字符集.
	 */
	public Map<Resource, Charset> getLocationCharsets() {
		return Collections.unmodifiableMap(this.locationCharsets);
	}

	/**
	 * 提供对用于将请求映射到静态资源的{@link UrlPathHelper}的引用.
	 * 这有助于获得有关查找路径的信息, 例如是否已解码.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 配置的{@link UrlPathHelper}.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, request, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return (StringUtils.hasText(resourcePath) &&
				getResource(resourcePath, null, locations) != null ? resourcePath : null);
	}

	private Resource getResource(String resourcePath, HttpServletRequest request,
			List<? extends Resource> locations) {

		for (Resource location : locations) {
			try {
				if (logger.isTraceEnabled()) {
					logger.trace("Checking location: " + location);
				}
				String pathToUse = encodeIfNecessary(resourcePath, request, location);
				Resource resource = getResource(pathToUse, location);
				if (resource != null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Found match: " + resource);
					}
					return resource;
				}
				else if (logger.isTraceEnabled()) {
					logger.trace("No match for location: " + location);
				}
			}
			catch (IOException ex) {
				logger.trace("Failure checking for relative resource - trying next location", ex);
			}
		}
		return null;
	}

	/**
	 * 查找给定位置下的资源.
	 * <p>默认实现检查相对于该位置的给定路径是否存在可读{@code Resource}.
	 * 
	 * @param resourcePath 资源的路径
	 * @param location 要检查的位置
	 * 
	 * @return 资源, 或{@code null}
	 */
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource resource = location.createRelative(resourcePath);
		if (resource.exists() && resource.isReadable()) {
			if (checkResource(resource, location)) {
				return resource;
			}
			else if (logger.isTraceEnabled()) {
				Resource[] allowedLocations = getAllowedLocations();
				logger.trace("Resource path \"" + resourcePath + "\" was successfully resolved " +
						"but resource \"" +	resource.getURL() + "\" is neither under the " +
						"current location \"" + location.getURL() + "\" nor under any of the " +
						"allowed locations " + (allowedLocations != null ? Arrays.asList(allowedLocations) : "[]"));
			}
		}
		return null;
	}

	/**
	 * 除了检查资源是否存在且可读之外, 还对已解析的资源执行其他检查.
	 * 默认实现还验证资源是否位于相对于其找到的位置或位于{@link #setAllowedLocations 允许的位置}之一下.
	 * 
	 * @param resource 要检查的资源
	 * @param location 找到资源的相对位置
	 * 
	 * @return "true" 如果资源位于有效位置, 否则为"false".
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}
		if (getAllowedLocations() != null) {
			for (Resource current : getAllowedLocations()) {
				if (isResourceUnderLocation(resource, current)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
		if (resource.getClass() != location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		}
		else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		}
		else if (resource instanceof ServletContextResource) {
			resourcePath = ((ServletContextResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
		}
		else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}

		if (locationPath.equals(resourcePath)) {
			return true;
		}
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
	}

	private String encodeIfNecessary(String path, HttpServletRequest request, Resource location) {
		if (shouldEncodeRelativePath(location) && request != null) {
			Charset charset = this.locationCharsets.get(location);
			charset = charset != null ? charset : DEFAULT_CHARSET;
			StringBuilder sb = new StringBuilder();
			StringTokenizer tokenizer = new StringTokenizer(path, "/");
			while (tokenizer.hasMoreTokens()) {
				String value = null;
				try {
					value = UriUtils.encode(tokenizer.nextToken(), charset.name());
				}
				catch (UnsupportedEncodingException ex) {
					// Should never happen
					throw new IllegalStateException("Unexpected error", ex);
				}
				sb.append(value);
				sb.append("/");
			}
			if (!path.endsWith("/")) {
				sb.setLength(sb.length() - 1);
			}
			return sb.toString();
		}
		else {
			return path;
		}
	}

	private boolean shouldEncodeRelativePath(Resource location) {
		return (location instanceof UrlResource && this.urlPathHelper != null && this.urlPathHelper.isUrlDecode());
	}

	private boolean isInvalidEncodedPath(String resourcePath) {
		if (resourcePath.contains("%")) {
			// 使用URLDecoder (vs UriUtils) 来保留可能解码的UTF-8字符...
			try {
				String decodedPath = URLDecoder.decode(resourcePath, "UTF-8");
				if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
					if (logger.isTraceEnabled()) {
						logger.trace("Resolved resource path contains encoded \"../\" or \"..\\\": " + resourcePath);
					}
					return true;
				}
			}
			catch (UnsupportedEncodingException ex) {
				// Should never happen...
			}
		}
		return false;
	}

}
