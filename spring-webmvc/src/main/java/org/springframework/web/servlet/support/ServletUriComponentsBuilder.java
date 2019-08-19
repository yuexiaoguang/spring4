package org.springframework.web.servlet.support;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 具有额外的静态工厂方法的UriComponentsBuilder, 可以根据当前的HttpServletRequest创建链接.
 *
 * <p><strong>Note:</strong> 此类使用"Forwarded"
 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
 * "X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto" header中的值,
 * 以反映客户端发起的协议和地址.
 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此类header.
 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

	private String originalPath;


	protected ServletUriComponentsBuilder() {
	}

	/**
	 * 创建给定ServletUriComponentsBuilder的深层副本.
	 * 
	 * @param other 要复制的其他构建器
	 */
	protected ServletUriComponentsBuilder(ServletUriComponentsBuilder other) {
		super(other);
		this.originalPath = other.originalPath;
	}


	// Factory methods based on a HttpServletRequest

	/**
	 * 从给定HttpServletRequest的主机, 端口, scheme和上下文路径准备构建器.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		String forwardedPrefix = getForwardedPrefix(request);
		builder.replacePath(forwardedPrefix != null ? forwardedPrefix : request.getContextPath());
		return builder;
	}

	/**
	 * 从给定HttpServletRequest的主机, 端口, scheme, 上下文路径和servlet映射准备构建器.
	 * <p>如果servlet按名称映射, e.g. {@code "/main/*"}, 路径将以"/main"结尾.
	 * 如果以其他方式映射servlet, e.g. {@code "/"}或{@code "*.do"},
	 * 结果与调用{@link #fromContextPath(HttpServletRequest)}的结果相同.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = fromContextPath(request);
		if (StringUtils.hasText(new UrlPathHelper().getPathWithinServletMapping(request))) {
			builder.path(request.getServletPath());
		}
		return builder;
	}

	/**
	 * 从HttpServletRequest的主机, 端口, scheme和路径 (but not the query)准备构建器.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(getRequestUriWithForwardedPrefix(request));
		return builder;
	}

	/**
	 * 通过复制HttpServletRequest的scheme, 主机, 端口, 路径和查询字符串来准备构建器.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = initFromRequest(request);
		builder.initPath(getRequestUriWithForwardedPrefix(request));
		builder.query(request.getQueryString());
		return builder;
	}

	/**
	 * 使用scheme, 主机和端口初始化构建器 (但不是路径和查询).
	 */
	private static ServletUriComponentsBuilder initFromRequest(HttpServletRequest request) {
		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();
		String scheme = uriComponents.getScheme();
		String host = uriComponents.getHost();
		int port = uriComponents.getPort();

		ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
		builder.scheme(scheme);
		builder.host(host);
		if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
			builder.port(port);
		}
		return builder;
	}

	private static String getForwardedPrefix(HttpServletRequest request) {
		String prefix = null;
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
				prefix = request.getHeader(name);
			}
		}
		if (prefix != null) {
			while (prefix.endsWith("/")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
		}
		return prefix;
	}

	private static String getRequestUriWithForwardedPrefix(HttpServletRequest request) {
		String path = request.getRequestURI();
		String forwardedPrefix = getForwardedPrefix(request);
		if (forwardedPrefix != null) {
			String contextPath = request.getContextPath();
			if (!StringUtils.isEmpty(contextPath) && !contextPath.equals("/") && path.startsWith(contextPath)) {
				path = path.substring(contextPath.length());
			}
			path = forwardedPrefix + path;
		}
		return path;
	}


	// Alternative methods relying on RequestContextHolder to find the request

	/**
	 * 与{@link #fromContextPath(HttpServletRequest)}相同, 除了请求是通过{@link RequestContextHolder}获得的.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * 与{@link #fromServletMapping(HttpServletRequest)}相同, 除了请求是通过{@link RequestContextHolder}获得的.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * 与{@link #fromRequestUri(HttpServletRequest)}相同, 除了请求是通过{@link RequestContextHolder}获得的.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequestUri() {
		return fromRequestUri(getCurrentRequest());
	}

	/**
	 * 与{@link #fromRequest(HttpServletRequest)}相同, 除了请求是通过{@link RequestContextHolder}获得的.
	 *
	 * <p><strong>Note:</strong> 如果找到, 此方法从"Forwarded"和"X-Forwarded-*" header中提取值. 请参阅类级文档.
	 *
	 * <p>从4.3.15开始, 此方法将contextPath替换为"X-Forwarded-Prefix", 而不是加前缀, 从而与{@code ForwardedHeaderFiller}对齐.
	 */
	public static ServletUriComponentsBuilder fromCurrentRequest() {
		return fromRequest(getCurrentRequest());
	}

	/**
	 * 通过{@link RequestContextHolder}获取当前请求.
	 */
	protected static HttpServletRequest getCurrentRequest() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		return ((ServletRequestAttributes) attrs).getRequest();
	}


	private void initPath(String path) {
		this.originalPath = path;
		replacePath(path);
	}

	/**
	 * 从{@link HttpServletRequest#getRequestURI() requestURI}中删除任何路径扩展名.
	 * 必须在调用{@link #path(String)}或{@link #pathSegment(String...)}之前调用此方法.
	 * <pre>
	 * GET http://foo.com/rest/books/6.json
	 *
	 * ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
	 * String ext = builder.removePathExtension();
	 * String uri = builder.path("/pages/1.{ext}").buildAndExpand(ext).toUriString();
	 * assertEquals("http://foo.com/rest/books/6/pages/1.json", result);
	 * </pre>
	 * 
	 * @return 可能重复使用的删除的路径扩展名, 或{@code null}
	 */
	public String removePathExtension() {
		String extension = null;
		if (this.originalPath != null) {
			extension = UriUtils.extractFileExtension(this.originalPath);
			if (!StringUtils.isEmpty(extension)) {
				int end = this.originalPath.length() - (extension.length() + 1);
				replacePath(this.originalPath.substring(0, end));
			}
			this.originalPath = null;
		}
		return extension;
	}

	@Override
	public ServletUriComponentsBuilder cloneBuilder() {
		return new ServletUriComponentsBuilder(this);
	}

}
