package org.springframework.test.web.servlet.htmlunit;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FormEncodingType;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import org.springframework.beans.Mergeable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.SmartRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 用于使用Spring MVC Test的 {@link RequestBuilder}将{@link WebRequest}转换为{@link MockHttpServletRequest}的内部类.
 *
 * <p>默认情况下, URL的第一个路径段用作上下文路径.
 * 要覆盖此默认值, 请参阅{@link #setContextPath(String)}.
 */
final class HtmlUnitRequestBuilder implements RequestBuilder, Mergeable {

	private static final Pattern LOCALE_PATTERN = Pattern.compile("^\\s*(\\w{2})(?:-(\\w{2}))?(?:;q=(\\d+\\.\\d+))?$");

	private static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");


	private static final Method getCharsetMethod = ClassUtils.getMethodIfAvailable(WebRequest.class, "getCharset");

	private final Map<String, MockHttpSession> sessions;

	private final WebClient webClient;

	private final WebRequest webRequest;

	private String contextPath;

	private RequestBuilder parentBuilder;

	private SmartRequestBuilder parentPostProcessor;

	private RequestPostProcessor forwardPostProcessor;


	/**
	 * @param sessions 从会话{@linkplain HttpSession#getId() IDs}到当前管理的{@link HttpSession}对象的{@link Map}; never {@code null}
	 * @param webClient 用于检索cookie的WebClient
	 * @param webRequest 要转换为{@link MockHttpServletRequest}的{@link WebRequest}; never {@code null}
	 */
	public HtmlUnitRequestBuilder(Map<String, MockHttpSession> sessions, WebClient webClient, WebRequest webRequest) {
		Assert.notNull(sessions, "Sessions Map must not be null");
		Assert.notNull(webClient, "WebClient must not be null");
		Assert.notNull(webRequest, "WebRequest must not be null");
		this.sessions = sessions;
		this.webClient = webClient;
		this.webRequest = webRequest;
	}


	public MockHttpServletRequest buildRequest(ServletContext servletContext) {
		Charset charset = getCharset();
		String httpMethod = this.webRequest.getHttpMethod().name();
		UriComponents uriComponents = uriComponents();

		MockHttpServletRequest request = new HtmlUnitMockHttpServletRequest(
				servletContext, httpMethod, uriComponents.getPath());
		parent(request, this.parentBuilder);
		request.setServerName(uriComponents.getHost());  // 需要首先获得额外的header
		authType(request);
		request.setCharacterEncoding(charset.name());
		content(request, charset);
		contextPath(request, uriComponents);
		contentType(request);
		cookies(request);
		headers(request);
		locales(request);
		servletPath(uriComponents, request);
		params(request, uriComponents);
		ports(uriComponents, request);
		request.setProtocol("HTTP/1.1");
		request.setQueryString(uriComponents.getQuery());
		request.setScheme(uriComponents.getScheme());
		request.setPathInfo(null);

		return postProcess(request);
	}

	private Charset getCharset() {
		if (getCharsetMethod != null) {
			Object value = ReflectionUtils.invokeMethod(getCharsetMethod, this.webRequest);
			if (value instanceof Charset) {
				// HtmlUnit 2.25: a Charset
				return (Charset) value;
			}
			else if (value != null) {
				// HtmlUnit up until 2.24: a String
				return Charset.forName(value.toString());
			}
		}
		return DEFAULT_CHARSET;
	}

	private MockHttpServletRequest postProcess(MockHttpServletRequest request) {
		if (this.parentPostProcessor != null) {
			request = this.parentPostProcessor.postProcessRequest(request);
		}
		if (this.forwardPostProcessor != null) {
			request = this.forwardPostProcessor.postProcessRequest(request);
		}
		return request;
	}

	private void parent(MockHttpServletRequest request, RequestBuilder parent) {
		if (parent == null) {
			return;
		}

		MockHttpServletRequest parentRequest = parent.buildRequest(request.getServletContext());

		// session
		HttpSession parentSession = parentRequest.getSession(false);
		if (parentSession != null) {
			Enumeration<String> attrNames = parentSession.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = parentSession.getAttribute(attrName);
				request.getSession().setAttribute(attrName, attrValue);
			}
		}

		// header
		Enumeration<String> headerNames = parentRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String attrName = headerNames.nextElement();
			Enumeration<String> attrValues = parentRequest.getHeaders(attrName);
			while (attrValues.hasMoreElements()) {
				String attrValue = attrValues.nextElement();
				request.addHeader(attrName, attrValue);
			}
		}

		// parameter
		Map<String, String[]> parentParams = parentRequest.getParameterMap();
		for (Map.Entry<String, String[]> parentParam : parentParams.entrySet()) {
			String paramName = parentParam.getKey();
			String[] paramValues = parentParam.getValue();
			request.addParameter(paramName, paramValues);
		}

		// cookie
		Cookie[] parentCookies = parentRequest.getCookies();
		if (!ObjectUtils.isEmpty(parentCookies)) {
			request.setCookies(parentCookies);
		}

		// request attribute
		Enumeration<String> parentAttrNames = parentRequest.getAttributeNames();
		while (parentAttrNames.hasMoreElements()) {
			String parentAttrName = parentAttrNames.nextElement();
			request.setAttribute(parentAttrName, parentRequest.getAttribute(parentAttrName));
		}
	}

	/**
	 * 设置要使用的contextPath.
	 * <p>该值可以为null, 在这种情况下, URL的第一个路径段将转换为contextPath.
	 * 否则它必须符合{@link HttpServletRequest#getContextPath()},
	 * 它声明它可以是一个空字符串, 或者它必须以 "/" 开头而不是以 "/"结尾.
	 * 
	 * @param contextPath 有效的contextPath
	 * 
	 * @throws IllegalArgumentException 如果contextPath不是有效的{@link HttpServletRequest#getContextPath()}
	 */
	public void setContextPath(String contextPath) {
		MockMvcWebConnection.validateContextPath(contextPath);
		this.contextPath = contextPath;
	}

	public void setForwardPostProcessor(RequestPostProcessor forwardPostProcessor) {
		this.forwardPostProcessor = forwardPostProcessor;
	}

	private void authType(MockHttpServletRequest request) {
		String authorization = header("Authorization");
		String[] authSplit = StringUtils.split(authorization, ": ");
		if (authSplit != null) {
			request.setAuthType(authSplit[0]);
		}
	}

	private void content(MockHttpServletRequest request, Charset charset) {
		String requestBody = this.webRequest.getRequestBody();
		if (requestBody == null) {
			return;
		}
		request.setContent(requestBody.getBytes(charset));
	}

	private void contentType(MockHttpServletRequest request) {
		String contentType = header("Content-Type");
		if (contentType == null) {
			FormEncodingType encodingType = this.webRequest.getEncodingType();
			if (encodingType != null) {
				contentType = encodingType.getName();
			}
		}
		request.setContentType(contentType != null ? contentType : MediaType.ALL_VALUE);
	}

	private void contextPath(MockHttpServletRequest request, UriComponents uriComponents) {
		if (this.contextPath == null) {
			List<String> pathSegments = uriComponents.getPathSegments();
			if (pathSegments.isEmpty()) {
				request.setContextPath("");
			}
			else {
				request.setContextPath("/" + pathSegments.get(0));
			}
		}
		else {
			if (!uriComponents.getPath().startsWith(this.contextPath)) {
				throw new IllegalArgumentException("\"" + uriComponents.getPath() +
						"\" should start with context path \"" + this.contextPath + "\"");
			}
			request.setContextPath(this.contextPath);
		}
	}

	private void cookies(MockHttpServletRequest request) {
		List<Cookie> cookies = new ArrayList<Cookie>();

		String cookieHeaderValue = header("Cookie");
		if (cookieHeaderValue != null) {
			StringTokenizer tokens = new StringTokenizer(cookieHeaderValue, "=;");
			while (tokens.hasMoreTokens()) {
				String cookieName = tokens.nextToken().trim();
				if (!tokens.hasMoreTokens()) {
					throw new IllegalArgumentException("Expected value for cookie name '" + cookieName +
							"': full cookie header was [" + cookieHeaderValue + "]");
				}
				String cookieValue = tokens.nextToken().trim();
				processCookie(request, cookies, new Cookie(cookieName, cookieValue));
			}
		}

		Set<com.gargoylesoftware.htmlunit.util.Cookie> managedCookies = this.webClient.getCookies(this.webRequest.getUrl());
		for (com.gargoylesoftware.htmlunit.util.Cookie cookie : managedCookies) {
			processCookie(request, cookies, new Cookie(cookie.getName(), cookie.getValue()));
		}

		Cookie[] parentCookies = request.getCookies();
		if (parentCookies != null) {
			for (Cookie cookie : parentCookies) {
				cookies.add(cookie);
			}
		}

		if (!ObjectUtils.isEmpty(cookies)) {
			request.setCookies(cookies.toArray(new Cookie[cookies.size()]));
		}
	}

	private void processCookie(MockHttpServletRequest request, List<Cookie> cookies, Cookie cookie) {
		cookies.add(cookie);
		if ("JSESSIONID".equals(cookie.getName())) {
			request.setRequestedSessionId(cookie.getValue());
			request.setSession(httpSession(request, cookie.getValue()));
		}
	}

	private String header(String headerName) {
		return this.webRequest.getAdditionalHeaders().get(headerName);
	}

	private void headers(MockHttpServletRequest request) {
		for (Entry<String, String> header : this.webRequest.getAdditionalHeaders().entrySet()) {
			request.addHeader(header.getKey(), header.getValue());
		}
	}

	private MockHttpSession httpSession(MockHttpServletRequest request, final String sessionid) {
		MockHttpSession session;
		synchronized (this.sessions) {
			session = this.sessions.get(sessionid);
			if (session == null) {
				session = new HtmlUnitMockHttpSession(request, sessionid);
				session.setNew(true);
				synchronized (this.sessions) {
					this.sessions.put(sessionid, session);
				}
				addSessionCookie(request, sessionid);
			}
			else {
				session.setNew(false);
			}
		}
		return session;
	}

	private void addSessionCookie(MockHttpServletRequest request, String sessionid) {
		getCookieManager().addCookie(createCookie(request, sessionid));
	}

	private void removeSessionCookie(MockHttpServletRequest request, String sessionid) {
		getCookieManager().removeCookie(createCookie(request, sessionid));
	}

	private com.gargoylesoftware.htmlunit.util.Cookie createCookie(MockHttpServletRequest request, String sessionid) {
		return new com.gargoylesoftware.htmlunit.util.Cookie(request.getServerName(), "JSESSIONID", sessionid,
				request.getContextPath() + "/", null, request.isSecure(), true);
	}

	private void locales(MockHttpServletRequest request) {
		String locale = header("Accept-Language");
		if (locale == null) {
			request.addPreferredLocale(Locale.getDefault());
		}
		else {
			String[] tokens = StringUtils.tokenizeToStringArray(locale, ",");
			for (int i = tokens.length - 1; i >= 0; i--) {
				request.addPreferredLocale(parseLocale(tokens[i]));
			}
		}
	}

	private void params(MockHttpServletRequest request, UriComponents uriComponents) {
		for (Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
			String name = entry.getKey();
			String urlDecodedName = urlDecode(name);
			for (String value : entry.getValue()) {
				value = (value != null ? urlDecode(value) : "");
				request.addParameter(urlDecodedName, value);
			}
		}
		for (NameValuePair param : this.webRequest.getRequestParameters()) {
			request.addParameter(param.getName(), param.getValue());
		}
	}

	private String urlDecode(String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Locale parseLocale(String locale) {
		Matcher matcher = LOCALE_PATTERN.matcher(locale);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid locale value [" + locale + "]");
		}
		String language = matcher.group(1);
		String country = matcher.group(2);
		if (country == null) {
			country = "";
		}
		String qualifier = matcher.group(3);
		if (qualifier == null) {
			qualifier = "";
		}
		return new Locale(language, country, qualifier);
	}

	private void servletPath(MockHttpServletRequest request, String requestPath) {
		String servletPath = requestPath.substring(request.getContextPath().length());
		if ("".equals(servletPath)) {
			servletPath = null;
		}
		request.setServletPath(servletPath);
	}

	private void servletPath(UriComponents uriComponents, MockHttpServletRequest request) {
		if ("".equals(request.getPathInfo())) {
			request.setPathInfo(null);
		}
		servletPath(request, uriComponents.getPath());
	}

	private void ports(UriComponents uriComponents, MockHttpServletRequest request) {
		int serverPort = uriComponents.getPort();
		request.setServerPort(serverPort);
		if (serverPort == -1) {
			int portConnection = this.webRequest.getUrl().getDefaultPort();
			request.setLocalPort(serverPort);
			request.setRemotePort(portConnection);
		}
		else {
			request.setRemotePort(serverPort);
		}
	}

	private UriComponents uriComponents() {
		URL url = this.webRequest.getUrl();
		return UriComponentsBuilder.fromUriString(url.toExternalForm()).build();
	}

	@Override
	public boolean isMergeEnabled() {
		return true;
	}

	@Override
	public Object merge(Object parent) {
		if (parent instanceof RequestBuilder) {
			if (parent instanceof MockHttpServletRequestBuilder) {
				MockHttpServletRequestBuilder copiedParent = MockMvcRequestBuilders.get("/");
				copiedParent.merge(parent);
				this.parentBuilder = copiedParent;
			}
			else {
				this.parentBuilder = (RequestBuilder) parent;
			}
			if (parent instanceof SmartRequestBuilder) {
				this.parentPostProcessor = (SmartRequestBuilder) parent;
			}
		}
		return this;
	}

	private CookieManager getCookieManager() {
		return this.webClient.getCookieManager();
	}


	/**
	 * {@link MockHttpServletRequest}的扩展, 确保在创建新的{@link HttpSession}时, 将其添加到管理的会话.
	 */
	private final class HtmlUnitMockHttpServletRequest extends MockHttpServletRequest {

		public HtmlUnitMockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
			super(servletContext, method, requestURI);
		}

		@Override
		public HttpSession getSession(boolean create) {
			HttpSession session = super.getSession(false);
			if (session == null && create) {
				HtmlUnitMockHttpSession newSession = new HtmlUnitMockHttpSession(this);
				setSession(newSession);
				newSession.setNew(true);
				String sessionid = newSession.getId();
				synchronized (HtmlUnitRequestBuilder.this.sessions) {
					HtmlUnitRequestBuilder.this.sessions.put(sessionid, newSession);
				}
				addSessionCookie(this, sessionid);
				session = newSession;
			}
			return session;
		}
	}


	/**
	 * {@link MockHttpSession}的扩展, 确保在调用{@link #invalidate()}时, 从管理的会话中删除{@link HttpSession}.
	 */
	private final class HtmlUnitMockHttpSession extends MockHttpSession {

		private final MockHttpServletRequest request;

		public HtmlUnitMockHttpSession(MockHttpServletRequest request) {
			super(request.getServletContext());
			this.request = request;
		}

		private HtmlUnitMockHttpSession(MockHttpServletRequest request, String id) {
			super(request.getServletContext(), id);
			this.request = request;
		}

		@Override
		public void invalidate() {
			super.invalidate();
			synchronized (HtmlUnitRequestBuilder.this.sessions) {
				HtmlUnitRequestBuilder.this.sessions.remove(getId());
			}
			removeSessionCookie(request, getId());
		}
	}
}
