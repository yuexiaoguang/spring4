package org.springframework.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

/**
 * 从"Forwarded"和"X-Forwarded-*" header中提取值, 以便从请求和响应中包装和覆盖以下内容:
 * {@link HttpServletRequest#getServerName() getServerName()},
 * {@link HttpServletRequest#getServerPort() getServerPort()},
 * {@link HttpServletRequest#getScheme() getScheme()},
 * {@link HttpServletRequest#isSecure() isSecure()},
 * {@link HttpServletResponse#sendRedirect(String) sendRedirect(String)}.
 * 实际上, 包装的请求和响应反映了客户端发起的协议和地址.
 *
 * <p><strong>Note:</strong> 此过滤器也可用于{@link #setRemoveOnly removeOnly}模式,
 * 其中"Forwarded"和"X-Forwarded-*" header仅在不使用的情况下被删除.
 */
public class ForwardedHeaderFilter extends OncePerRequestFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<Boolean>(5, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
	}


	private final UrlPathHelper pathHelper;

	private boolean removeOnly;

	private boolean relativeRedirects;


	public ForwardedHeaderFilter() {
		this.pathHelper = new UrlPathHelper();
		this.pathHelper.setUrlDecode(false);
		this.pathHelper.setRemoveSemicolonContent(false);
	}


	/**
	 * 启用仅删除"Forwarded" 或 "X-Forwarded-*" header并忽略其中信息的模式.
	 * 
	 * @param removeOnly 是否丢弃并忽略转发的header
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * 使用此属性启用相对重定向, 如{@link RelativeRedirectFilter}中所述,
	 * 并使用与该过滤器相同的响应包装器, 或者如果两者都配置, 则只有一个将包装.
	 * <p>默认, 如果此属性设置为false, 则在这种情况下, 将覆盖对{@link HttpServletResponse#sendRedirect(String)}的调用,
	 * 以便将相对转换为绝对URL, 同时还会考虑转发的header.
	 * 
	 * @param relativeRedirects 是否使用相对重定向
	 */
	public void setRelativeRedirects(boolean relativeRedirects) {
		this.relativeRedirects = relativeRedirects;
	}


	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (FORWARDED_HEADER_NAMES.contains(name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (this.removeOnly) {
			ForwardedHeaderRemovingRequest theRequest = new ForwardedHeaderRemovingRequest(request);
			filterChain.doFilter(theRequest, response);
		}
		else {
			HttpServletRequest theRequest = new ForwardedHeaderExtractingRequest(request, this.pathHelper);
			HttpServletResponse theResponse = (this.relativeRedirects ?
					RelativeRedirectResponseWrapper.wrapIfNecessary(response, HttpStatus.SEE_OTHER) :
					new ForwardedHeaderExtractingResponse(response, theRequest));
			filterChain.doFilter(theRequest, theResponse);
		}
	}


	/**
	 * 隐藏"Forwarded"或"X-Forwarded-*" header.
	 */
	private static class ForwardedHeaderRemovingRequest extends HttpServletRequestWrapper {

		private final Map<String, List<String>> headers;

		public ForwardedHeaderRemovingRequest(HttpServletRequest request) {
			super(request);
			this.headers = initHeaders(request);
		}

		private static Map<String, List<String>> initHeaders(HttpServletRequest request) {
			Map<String, List<String>> headers = new LinkedCaseInsensitiveMap<List<String>>(Locale.ENGLISH);
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (!FORWARDED_HEADER_NAMES.contains(name)) {
					headers.put(name, Collections.list(request.getHeaders(name)));
				}
			}
			return headers;
		}

		// 覆盖header访问器以不公开转发的header

		@Override
		public String getHeader(String name) {
			List<String> value = this.headers.get(name);
			return (CollectionUtils.isEmpty(value) ? null : value.get(0));
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			List<String> value = this.headers.get(name);
			return (Collections.enumeration(value != null ? value : Collections.<String>emptySet()));
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(this.headers.keySet());
		}
	}


	/**
	 * 提取并使用"Forwarded"或"X-Forwarded-*" header.
	 */
	private static class ForwardedHeaderExtractingRequest extends ForwardedHeaderRemovingRequest {

		private final String scheme;

		private final boolean secure;

		private final String host;

		private final int port;

		private final String contextPath;

		private final String requestUri;

		private final String requestUrl;

		public ForwardedHeaderExtractingRequest(HttpServletRequest request, UrlPathHelper pathHelper) {
			super(request);

			HttpRequest httpRequest = new ServletServerHttpRequest(request);
			UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();
			int port = uriComponents.getPort();

			this.scheme = uriComponents.getScheme();
			this.secure = "https".equals(scheme);
			this.host = uriComponents.getHost();
			this.port = (port == -1 ? (this.secure ? 443 : 80) : port);

			String prefix = getForwardedPrefix(request);
			this.contextPath = (prefix != null ? prefix : request.getContextPath());
			this.requestUri = this.contextPath + pathHelper.getPathWithinApplication(request);
			this.requestUrl = this.scheme + "://" + this.host + (port == -1 ? "" : ":" + port) + this.requestUri;
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

		@Override
		public String getScheme() {
			return this.scheme;
		}

		@Override
		public String getServerName() {
			return this.host;
		}

		@Override
		public int getServerPort() {
			return this.port;
		}

		@Override
		public boolean isSecure() {
			return this.secure;
		}

		@Override
		public String getContextPath() {
			return this.contextPath;
		}

		@Override
		public String getRequestURI() {
			return this.requestUri;
		}

		@Override
		public StringBuffer getRequestURL() {
			return new StringBuffer(this.requestUrl);
		}
	}


	private static class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {

		private static final String FOLDER_SEPARATOR = "/";

		private final HttpServletRequest request;

		public ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public void sendRedirect(String location) throws IOException {

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);
			UriComponents uriComponents = builder.build();

			// Absolute location
			if (uriComponents.getScheme() != null) {
				super.sendRedirect(location);
				return;
			}

			// Network-path reference
			if (location.startsWith("//")) {
				String scheme = this.request.getScheme();
				super.sendRedirect(builder.scheme(scheme).toUriString());
				return;
			}

			String path = uriComponents.getPath();
			if (path != null) {
				// 相对于Servlet容器根或当前请求
				path = (path.startsWith(FOLDER_SEPARATOR) ? path :
						StringUtils.applyRelativePath(this.request.getRequestURI(), path));
			}

			String result = UriComponentsBuilder
					.fromHttpRequest(new ServletServerHttpRequest(this.request))
					.replacePath(path)
					.replaceQuery(uriComponents.getQuery())
					.fragment(uriComponents.getFragment())
					.build().normalize().toUriString();

			super.sendRedirect(result);
		}
	}

}
