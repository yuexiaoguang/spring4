package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.accept.ServletPathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@code HttpRequestHandler}根据Page Speed, YSlow等指南以优化方式提供静态资源.
 *
 * <p>{@linkplain #setLocations "locations"}属性获取Spring {@link Resource}位置列表,
 * 允许此处理器从中提供静态资源.
 * 可以从类路径位置提供资源, e.g. "classpath:/META-INF/public-web-resources/",
 * 允许在jar文件中方便地打包和提供资源, 如 .js, .css等.
 *
 * <p>此请求处理器还可以配置
 * {@link #setResourceResolvers(List) resourcesResolver}
 * 和{@link #setResourceTransformers(List) resourceTransformer}链, 以支持任意解析和转换所提供的资源.
 * 默认情况下, {@link PathResourceResolver}只根据配置的"locations"查找资源.
 * 应用程序可以配置其他解析器和转换器, 例如{@link VersionResourceResolver}, 它可以解析和准备URL中具有版本的资源的URL.
 *
 * <p>此处理器还正确评估{@code Last-Modified} header, 以便根据需要返回{@code 304}状态代码,
 * 从而避免已在客户端缓存的资源的不必要开销.
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
		implements HttpRequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

	// Servlet 3.1 setContentLengthLong(long) available?
	private static final boolean contentLengthLongAvailable =
			ClassUtils.hasMethod(ServletResponse.class, "setContentLengthLong", long.class);

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";


	private final List<String> locationValues = new ArrayList<String>(4);

	private final List<Resource> locations = new ArrayList<Resource>(4);

	private final Map<Resource, Charset> locationCharsets = new HashMap<Resource, Charset>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<ResourceResolver>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<ResourceTransformer>(4);

	private ResourceHttpMessageConverter resourceHttpMessageConverter;

	private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

	private ContentNegotiationManager contentNegotiationManager;

	private PathExtensionContentNegotiationStrategy contentNegotiationStrategy;

	private CorsConfiguration corsConfiguration;

	private UrlPathHelper urlPathHelper;

	private StringValueResolver embeddedValueResolver;


	public ResourceHttpRequestHandler() {
		super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}


	/**
	 * {@link #setLocations(List)}的替代方法, 它接受基于字符串的位置值列表,
	 * 支持{@link UrlResource} (e.g. 文件或HTTP URL) 带有特殊前缀以指示要使用的字符集, 附加相对路径时.
	 * 例如{@code "[charset=Windows-31J]http://example.org/path"}.
	 */
	public void setLocationValues(List<String> locationValues) {
		Assert.notNull(locationValues, "Location values list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locationValues);
	}

	/**
	 * 设置{@code Resource}位置的{@code List}以用作提供静态资源的源.
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locations.clear();
		this.locations.addAll(locations);
	}

	/**
	 * 返回{@code Resource}位置的配置的{@code List}.
	 * <p>请注意, 如果提供{@link #setLocationValues(List) locationValues}, 而不是加载基于资源的位置,
	 * 此方法将返回空, 直到通过{@link #afterPropertiesSet()}初始化后.
	 */
	public List<Resource> getLocations() {
		return this.locations;
	}

	/**
	 * 配置要使用的{@link ResourceResolver}列表.
	 * <p>默认配置{@link PathResourceResolver}.
	 * 如果使用此属性, 建议添加{@link PathResourceResolver}作为最后一个解析器.
	 */
	public void setResourceResolvers(List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * 返回配置的资源解析器列表.
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * 配置要使用的{@link ResourceTransformer}列表.
	 * <p>默认不配置转换器.
	 */
	public void setResourceTransformers(List<ResourceTransformer> resourceTransformers) {
		this.resourceTransformers.clear();
		if (resourceTransformers != null) {
			this.resourceTransformers.addAll(resourceTransformers);
		}
	}

	/**
	 * 返回配置的资源转换器列表.
	 */
	public List<ResourceTransformer> getResourceTransformers() {
		return this.resourceTransformers;
	}

	/**
	 * 配置要使用的{@link ResourceHttpMessageConverter}.
	 * <p>默认配置为{@link ResourceHttpMessageConverter}.
	 */
	public void setResourceHttpMessageConverter(ResourceHttpMessageConverter messageConverter) {
		this.resourceHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回配置的资源转换器.
	 */
	public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
		return this.resourceHttpMessageConverter;
	}

	/**
	 * 配置要使用的{@link ResourceRegionHttpMessageConverter}.
	 * <p>默认配置为{@link ResourceRegionHttpMessageConverter}.
	 */
	public void setResourceRegionHttpMessageConverter(ResourceRegionHttpMessageConverter messageConverter) {
		this.resourceRegionHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回配置的资源区域转换器.
	 */
	public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
		return this.resourceRegionHttpMessageConverter;
	}

	/**
	 * 配置{@code ContentNegotiationManager}以帮助确定所提供资源的媒体类型.
	 * 如果管理器包含路径扩展策略, 则将检查其是否已注册文件扩展名.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的内容协商管理器.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 为此处理器提供的资源指定CORS配置.
	 * <p>默认未设置, 允许跨源请求.
	 */
	public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	/**
	 * 返回指定的CORS配置.
	 */
	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		return this.corsConfiguration;
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
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (logger.isWarnEnabled() && CollectionUtils.isEmpty(this.locations)) {
			logger.warn("Locations list is empty. No resources will be served unless a " +
					"custom ResourceResolver is configured as an alternative to PathResourceResolver.");
		}

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		if (this.resourceHttpMessageConverter == null) {
			this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
		}
		if (this.resourceRegionHttpMessageConverter == null) {
			this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
		}

		this.contentNegotiationStrategy = initContentNegotiationStrategy();
	}

	private void resolveResourceLocations() {
		if (CollectionUtils.isEmpty(this.locationValues)) {
			return;
		}
		else if (!CollectionUtils.isEmpty(this.locations)) {
			throw new IllegalArgumentException("Please set either Resource-based \"locations\" or " +
					"String-based \"locationValues\", but not both.");
		}

		ApplicationContext applicationContext = getApplicationContext();
		for (String location : this.locationValues) {
			if (this.embeddedValueResolver != null) {
				String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
				if (resolvedLocation == null) {
					throw new IllegalArgumentException("Location resolved to null: " + location);
				}
				location = resolvedLocation;
			}
			Charset charset = null;
			location = location.trim();
			if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
				int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
				if (endIndex == -1) {
					throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
				}
				String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
				charset = Charset.forName(value);
				location = location.substring(endIndex + 1);
			}
			Resource resource = applicationContext.getResource(location);
			this.locations.add(resource);
			if (charset != null) {
				if (!(resource instanceof UrlResource)) {
					throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
				}
				this.locationCharsets.put(resource, charset);
			}
		}
	}

	/**
	 * 在配置的资源解析器中查找{@code PathResourceResolver}并设置其{@code allowedLocations}属性,
	 * 以匹配此类上配置的{@link #setLocations 位置}.
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(this.locations)) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
					pathResolver.setAllowedLocations(getLocations().toArray(new Resource[getLocations().size()]));
				}
				if (this.urlPathHelper != null) {
					pathResolver.setLocationCharsets(this.locationCharsets);
					pathResolver.setUrlPathHelper(this.urlPathHelper);
				}
				break;
			}
		}
	}

	/**
	 * 根据{@code ContentNegotiationManager}设置和{@code ServletContext}的可用性初始化内容协商策略.
	 */
	protected PathExtensionContentNegotiationStrategy initContentNegotiationStrategy() {
		Map<String, MediaType> mediaTypes = null;
		if (getContentNegotiationManager() != null) {
			PathExtensionContentNegotiationStrategy strategy =
					getContentNegotiationManager().getStrategy(PathExtensionContentNegotiationStrategy.class);
			if (strategy != null) {
				mediaTypes = new HashMap<String, MediaType>(strategy.getMediaTypes());
			}
		}
		return (getServletContext() != null ?
				new ServletPathExtensionContentNegotiationStrategy(getServletContext(), mediaTypes) :
				new PathExtensionContentNegotiationStrategy(mediaTypes));
	}


	/**
	 * 处理资源请求.
	 * <p>检查配置的位置列表中是否存在所请求的资源.
	 * 如果资源不存在, 则会向客户端返回{@code 404}响应.
	 * 如果资源存在, 将检查请求是否存在{@code Last-Modified} header,
	 * 并将其值与给定资源的last-modified时间戳进行比较, 如果{@code Last-Modified}值更大, 返回{@code 304}状态码.
	 * 如果资源比{@code Last-Modified}值更新, 或者 header不存在, 则资源的内容资源将写入响应, 缓存header设置为将来一年到期.
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// 对于非常一般的映射 (e.g. "/"), 需要先检查404
		Resource resource = getResource(request);
		if (resource == null) {
			logger.trace("No matching resource found - returning 404");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return;
		}

		// 支持的方法和所需的会话
		checkRequest(request);

		// Header phase
		if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.trace("Resource not modified - returning 304");
			return;
		}

		// 应用缓存设置
		prepareResponse(response);

		// 检查资源的媒体类型
		MediaType mediaType = getMediaType(request, resource);
		if (mediaType != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Determined media type '" + mediaType + "' for " + resource);
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("No media type found for " + resource + " - not sending a content-type header");
			}
		}

		// Content phase
		if (METHOD_HEAD.equals(request.getMethod())) {
			setHeaders(response, resource, mediaType);
			logger.trace("HEAD request - skipping content");
			return;
		}

		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		if (request.getHeader(HttpHeaders.RANGE) == null) {
			setHeaders(response, resource, mediaType);
			this.resourceHttpMessageConverter.write(resource, mediaType, outputMessage);
		}
		else {
			response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
			ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
			try {
				List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				if (httpRanges.size() == 1) {
					ResourceRegion resourceRegion = httpRanges.get(0).toResourceRegion(resource);
					this.resourceRegionHttpMessageConverter.write(resourceRegion, mediaType, outputMessage);
				}
				else {
					this.resourceRegionHttpMessageConverter.write(
							HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
				}
			}
			catch (IllegalArgumentException ex) {
				response.setHeader("Content-Range", "bytes */" + resource.contentLength());
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		}
	}

	protected Resource getResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring invalid resource path [" + path + "]");
			}
			return null;
		}
		if (isInvalidEncodedPath(path)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring invalid resource path with escape sequences [" + path + "]");
			}
			return null;
		}

		ResourceResolverChain resolveChain = new DefaultResourceResolverChain(getResourceResolvers());
		Resource resource = resolveChain.resolveResource(request, path, getLocations());
		if (resource == null || getResourceTransformers().isEmpty()) {
			return resource;
		}

		ResourceTransformerChain transformChain =
				new DefaultResourceTransformerChain(resolveChain, getResourceTransformers());
		resource = transformChain.transform(request, resource);
		return resource;
	}

	/**
	 * 处理给定的资源路径.
	 * <p>默认实现替换:
	 * <ul>
	 * <li>正斜杠的反斜杠.
	 * <li>单个斜杠的重复出现斜杠.
	 * <li>前导斜杠和控制字符 (00-1F 和 7F)与单个"/" 或 ""的任意组合. 例如 {@code "  / // foo/bar"}变为{@code "/foo/bar"}.
	 * </ul>
	 */
	protected String processPath(String path) {
		path = StringUtils.replace(path, "\\", "/");
		path = cleanDuplicateSlashes(path);
		return cleanLeadingSlash(path);
	}

	private String cleanDuplicateSlashes(String path) {
		StringBuilder sb = null;
		char prev = 0;
		for (int i = 0; i < path.length(); i++) {
			char curr = path.charAt(i);
			try {
				if ((curr == '/') && (prev == '/')) {
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					continue;
				}
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			}
			finally {
				prev = curr;
			}
		}
		return sb != null ? sb.toString() : path;
	}

	private String cleanLeadingSlash(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				path = (slash ? "/" + path.substring(i) : path.substring(i));
				if (logger.isTraceEnabled()) {
					logger.trace("Path after trimming leading '/' and control characters: [" + path + "]");
				}
				return path;
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * 检查给定路径是否包含无效的转义序列.
	 * 
	 * @param path 要验证的路径
	 * 
	 * @return {@code true} 如果路径无效, 否则{@code false}
	 */
	private boolean isInvalidEncodedPath(String path) {
		if (path.contains("%")) {
			try {
				// 使用URLDecoder (vs UriUtils) 来保留可能解码的UTF-8字符
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				if (isInvalidPath(decodedPath)) {
					return true;
				}
				decodedPath = processPath(decodedPath);
				if (isInvalidPath(decodedPath)) {
					return true;
				}
			}
			catch (IllegalArgumentException ex) {
				// Should never happen...
			}
			catch (UnsupportedEncodingException ex) {
				// Should never happen...
			}
		}
		return false;
	}

	/**
	 * 标识无效的资源路径. 默认情况下拒绝:
	 * <ul>
	 * <li>包含"WEB-INF" 或 "META-INF"的路径
	 * <li>调用{@link org.springframework.util.StringUtils#cleanPath}后包含"../"的路径.
	 * <li>表示{@link org.springframework.util.ResourceUtils#isUrl 有效URL}的路径, 或代表删除前导斜杠后的路径.
	 * </ul>
	 * <p><strong>Note:</strong> 这种方法假设已经去除了前导, 重复的 '/'或控制字符 (e.g. 空格),
	 * 以便路径以单个 '/'开头或者没有.
	 * 
	 * @param path 要验证的路径
	 * 
	 * @return {@code true} 如果路径无效, 否则{@code false}
	 */
	protected boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			logger.trace("Path contains \"WEB-INF\" or \"META-INF\".");
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				logger.trace("Path represents URL or has \"url:\" prefix.");
				return true;
			}
		}
		if (path.contains("..")) {
			path = StringUtils.cleanPath(path);
			if (path.contains("../")) {
				logger.trace("Path contains \"../\" after call to StringUtils#cleanPath.");
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定请求的媒体类型以及与之匹配的资源.
	 * 此实现尝试通过
	 * {@link ServletPathExtensionContentNegotiationStrategy#getMediaTypeForResource}
	 * 基于Resource的文件扩展名确定MediaType.
	 * 
	 * @param request 当前的请求
	 * @param resource 要检查的资源
	 * 
	 * @return 相应的媒体类型, 或{@code null}
	 */
	@SuppressWarnings("deprecation")
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		// 为了向后兼容
		MediaType mediaType = getMediaType(resource);
		if (mediaType != null) {
			return mediaType;
		}
		return this.contentNegotiationStrategy.getMediaTypeForResource(resource);
	}

	/**
	 * 确定给定资源的适当媒体类型.
	 * 
	 * @param resource 要检查的资源
	 * 
	 * @return 相应的媒体类型, 或{@code null}
	 * @deprecated as of 4.3 this method is deprecated; please override
	 * {@link #getMediaType(HttpServletRequest, Resource)} instead.
	 */
	@Deprecated
	protected MediaType getMediaType(Resource resource) {
		return null;
	}

	/**
	 * 在给定的servlet响应上设置header.
	 * 调用GET请求以及HEAD请求.
	 * 
	 * @param response 当前的servlet响应
	 * @param resource 已识别的资源 (never {@code null})
	 * @param mediaType 资源的媒体类型 (never {@code null})
	 * 
	 * @throws IOException 如果在设置header时出错
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, MediaType mediaType) throws IOException {
		long length = resource.contentLength();
		if (length > Integer.MAX_VALUE) {
			if (contentLengthLongAvailable) {
				response.setContentLengthLong(length);
			}
			else {
				response.setHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(length));
			}
		}
		else {
			response.setContentLength((int) length);
		}

		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
		if (resource instanceof EncodedResource) {
			response.setHeader(HttpHeaders.CONTENT_ENCODING, ((EncodedResource) resource).getContentEncoding());
		}
		if (resource instanceof VersionedResource) {
			response.setHeader(HttpHeaders.ETAG, "\"" + ((VersionedResource) resource).getVersion() + "\"");
		}
		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
	}


	@Override
	public String toString() {
		return "ResourceHttpRequestHandler [locations=" + getLocations() + ", resolvers=" + getResourceResolvers() + "]";
	}
}
