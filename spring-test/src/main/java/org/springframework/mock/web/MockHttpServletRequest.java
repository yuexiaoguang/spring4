package org.springframework.mock.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link javax.servlet.http.HttpServletRequest}接口的模拟实现.
 *
 * <p>此请求模拟的<em>服务器</em>的默认首选{@link Locale}是{@link Locale#ENGLISH}.
 * 可以通过{@link #addPreferredLocale}或{@link #setPreferredLocales}更改此值.
 *
 * <p>从Spring Framework 4.0开始, 这组模拟是在Servlet 3.0基线上设计的.
 */
public class MockHttpServletRequest implements HttpServletRequest {

	private static final String HTTP = "http";

	private static final String HTTPS = "https";

	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	private static final String HOST_HEADER = "Host";

	private static final String CHARSET_PREFIX = "charset=";

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	private static final ServletInputStream EMPTY_SERVLET_INPUT_STREAM =
			new DelegatingServletInputStream(StreamUtils.emptyInput());

	private static final BufferedReader EMPTY_BUFFERED_READER =
			new BufferedReader(new StringReader(""));

	/**
	 * HTTP RFC中指定的日期格式
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};


	// ---------------------------------------------------------------------
	// Public constants
	// ---------------------------------------------------------------------

	/**
	 * 默认协议: 'HTTP/1.1'.
	 */
	public static final String DEFAULT_PROTOCOL = "HTTP/1.1";

	/**
	 * 默认模式: 'http'.
	 */
	public static final String DEFAULT_SCHEME = HTTP;

	/**
	 * 默认服务器地址: '127.0.0.1'.
	 */
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

	/**
	 * 默认服务器名称: 'localhost'.
	 */
	public static final String DEFAULT_SERVER_NAME = "localhost";

	/**
	 * 默认服务器端口: '80'.
	 */
	public static final int DEFAULT_SERVER_PORT = 80;

	/**
	 * 默认远程地址: '127.0.0.1'.
	 */
	public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";

	/**
	 * 默认远程主机: 'localhost'.
	 */
	public static final String DEFAULT_REMOTE_HOST = "localhost";


	// ---------------------------------------------------------------------
	// Lifecycle properties
	// ---------------------------------------------------------------------

	private final ServletContext servletContext;

	private boolean active = true;


	// ---------------------------------------------------------------------
	// ServletRequest properties
	// ---------------------------------------------------------------------

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private String characterEncoding;

	private byte[] content;

	private String contentType;

	private final Map<String, String[]> parameters = new LinkedHashMap<String, String[]>();

	private String protocol = DEFAULT_PROTOCOL;

	private String scheme = DEFAULT_SCHEME;

	private String serverName = DEFAULT_SERVER_NAME;

	private int serverPort = DEFAULT_SERVER_PORT;

	private String remoteAddr = DEFAULT_REMOTE_ADDR;

	private String remoteHost = DEFAULT_REMOTE_HOST;

	/** 按降序排列的区域设置列表 */
	private final List<Locale> locales = new LinkedList<Locale>();

	private boolean secure = false;

	private int remotePort = DEFAULT_SERVER_PORT;

	private String localName = DEFAULT_SERVER_NAME;

	private String localAddr = DEFAULT_SERVER_ADDR;

	private int localPort = DEFAULT_SERVER_PORT;

	private boolean asyncStarted = false;

	private boolean asyncSupported = false;

	private MockAsyncContext asyncContext;

	private DispatcherType dispatcherType = DispatcherType.REQUEST;


	// ---------------------------------------------------------------------
	// HttpServletRequest properties
	// ---------------------------------------------------------------------

	private String authType;

	private Cookie[] cookies;

	private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<HeaderValueHolder>();

	private String method;

	private String pathInfo;

	private String contextPath = "";

	private String queryString;

	private String remoteUser;

	private final Set<String> userRoles = new HashSet<String>();

	private Principal userPrincipal;

	private String requestedSessionId;

	private String requestURI;

	private String servletPath = "";

	private HttpSession session;

	private boolean requestedSessionIdValid = true;

	private boolean requestedSessionIdFromCookie = true;

	private boolean requestedSessionIdFromURL = false;

	private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<String, Part>();


	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------

	public MockHttpServletRequest() {
		this(null, "", "");
	}

	/**
	 * @param method 请求方法 (may be {@code null})
	 * @param requestURI 请求URI (may be {@code null})
	 */
	public MockHttpServletRequest(String method, String requestURI) {
		this(null, method, requestURI);
	}

	/**
	 * @param servletContext 运行请求的ServletContext (可能是{@code null}以使用默认的{@link MockServletContext})
	 */
	public MockHttpServletRequest(ServletContext servletContext) {
		this(servletContext, "", "");
	}

	/**
	 * <p>首选区域设置将设置为{@link Locale#ENGLISH}.
	 * 
	 * @param servletContext 运行请求的ServletContext (可能是{@code null}以使用默认的{@link MockServletContext})
	 * @param method 请求方法 (may be {@code null})
	 * @param requestURI 请求URI (may be {@code null})
	 */
	public MockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.method = method;
		this.requestURI = requestURI;
		this.locales.add(Locale.ENGLISH);
	}


	// ---------------------------------------------------------------------
	// Lifecycle methods
	// ---------------------------------------------------------------------

	/**
	 * 返回与此请求关联的ServletContext.
	 * (由于某种原因, 在标准HttpServletRequest接口中不可用.)
	 */
	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * 返回此请求是否仍处于活动状态 (即尚未完成).
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * 将此请求标记为已完成, 并保持其状态.
	 */
	public void close() {
		this.active = false;
	}

	/**
	 * 使此请求无效, 清除其状态.
	 */
	public void invalidate() {
		close();
		clearAttributes();
	}

	/**
	 * 检查此请求是否仍处于活动状态 (即尚未完成), 如果不再处于活动状态, 则抛出IllegalStateException.
	 */
	protected void checkActive() throws IllegalStateException {
		if (!this.active) {
			throw new IllegalStateException("Request is not active anymore");
		}
	}


	// ---------------------------------------------------------------------
	// ServletRequest interface
	// ---------------------------------------------------------------------

	@Override
	public Object getAttribute(String name) {
		checkActive();
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		checkActive();
		return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
	}

	@Override
	public String getCharacterEncoding() {
		return this.characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
		updateContentTypeHeader();
	}

	private void updateContentTypeHeader() {
		if (StringUtils.hasLength(this.contentType)) {
			StringBuilder sb = new StringBuilder(this.contentType);
			if (!this.contentType.toLowerCase().contains(CHARSET_PREFIX) &&
					StringUtils.hasLength(this.characterEncoding)) {
				sb.append(";").append(CHARSET_PREFIX).append(this.characterEncoding);
			}
			doAddHeaderValue(CONTENT_TYPE_HEADER, sb.toString(), true);
		}
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public int getContentLength() {
		return (this.content != null ? this.content.length : -1);
	}

	public long getContentLengthLong() {
		return getContentLength();
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				if (mediaType.getCharset() != null) {
					this.characterEncoding = mediaType.getCharset().name();
				}
			}
			catch (Exception ex) {
				// 尽量获得charset值
				int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
				if (charsetIndex != -1) {
					this.characterEncoding = contentType.substring(charsetIndex + CHARSET_PREFIX.length());
				}
			}
			updateContentTypeHeader();
		}
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public ServletInputStream getInputStream() {
		if (this.content != null) {
			return new DelegatingServletInputStream(new ByteArrayInputStream(this.content));
		}
		else {
			return EMPTY_SERVLET_INPUT_STREAM;
		}
	}

	/**
	 * 为指定的HTTP参数设置单个值.
	 * <p>如果已经为给定的参数名称注册了一个或多个值, 则将替换它们.
	 */
	public void setParameter(String name, String value) {
		setParameter(name, new String[] {value});
	}

	/**
	 * 为指定的HTTP参数设置值数组.
	 * <p>如果已经为给定的参数名称注册了一个或多个值, 则将替换它们.
	 */
	public void setParameter(String name, String... values) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.put(name, values);
	}

	/**
	 * 设置所有提供的参数, <strong>替换</strong>所提供参数名称的任何现有值.
	 * 要在不替换现有值的情况下添加, 使用{@link #addParameters(java.util.Map)}.
	 */
	public void setParameters(Map<String, ?> params) {
		Assert.notNull(params, "Parameter map must not be null");
		for (String key : params.keySet()) {
			Object value = params.get(key);
			if (value instanceof String) {
				setParameter(key, (String) value);
			}
			else if (value instanceof String[]) {
				setParameter(key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException(
						"Parameter map value must be single value " + " or array of type [" + String.class.getName() + "]");
			}
		}
	}

	/**
	 * 为指定的HTTP参数添加单个值.
	 * <p>如果已经为给定的参数名称注册了一个或多个值, 则给定的值将添加到列表的末尾.
	 */
	public void addParameter(String name, String value) {
		addParameter(name, new String[] {value});
	}

	/**
	 * 为指定的HTTP参数添加值数组.
	 * <p>如果已经为给定的参数名称注册了一个或多个值, 则给定的值将添加到列表的末尾.
	 */
	public void addParameter(String name, String... values) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] oldArr = this.parameters.get(name);
		if (oldArr != null) {
			String[] newArr = new String[oldArr.length + values.length];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			System.arraycopy(values, 0, newArr, oldArr.length, values.length);
			this.parameters.put(name, newArr);
		}
		else {
			this.parameters.put(name, values);
		}
	}

	/**
	 * 添加所有提供的参数, <strong>不</strong>替换任何现有值.
	 * 要替换现有值, 使用{@link #setParameters(java.util.Map)}.
	 */
	public void addParameters(Map<String, ?> params) {
		Assert.notNull(params, "Parameter map must not be null");
		for (String key : params.keySet()) {
			Object value = params.get(key);
			if (value instanceof String) {
				addParameter(key, (String) value);
			}
			else if (value instanceof String[]) {
				addParameter(key, (String[]) value);
			}
			else {
				throw new IllegalArgumentException("Parameter map value must be single value " +
						" or array of type [" + String.class.getName() + "]");
			}
		}
	}

	/**
	 * 删除已注册的指定HTTP参数值.
	 */
	public void removeParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.remove(name);
	}

	/**
	 * 删除所有现有参数.
	 */
	public void removeAllParameters() {
		this.parameters.clear();
	}

	@Override
	public String getParameter(String name) {
		String[] arr = (name != null ? this.parameters.get(name) : null);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(this.parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		return (name != null ? this.parameters.get(name) : null);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public String getServerName() {
		String host = getHeader(HOST_HEADER);
		if (host != null) {
			host = host.trim();
			if (host.startsWith("[")) {
				host = host.substring(1, host.indexOf(']'));
			}
			else if (host.contains(":")) {
				host = host.substring(0, host.indexOf(':'));
			}
			return host;
		}

		// else
		return this.serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	@Override
	public int getServerPort() {
		String host = getHeader(HOST_HEADER);
		if (host != null) {
			host = host.trim();
			int idx;
			if (host.startsWith("[")) {
				idx = host.indexOf(':', host.indexOf(']'));
			}
			else {
				idx = host.indexOf(':');
			}
			if (idx != -1) {
				return Integer.parseInt(host.substring(idx + 1));
			}
		}

		// else
		return this.serverPort;
	}

	@Override
	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.content != null) {
			InputStream sourceStream = new ByteArrayInputStream(this.content);
			Reader sourceReader = (this.characterEncoding != null) ?
					new InputStreamReader(sourceStream, this.characterEncoding) :
					new InputStreamReader(sourceStream);
			return new BufferedReader(sourceReader);
		}
		else {
			return EMPTY_BUFFERED_READER;
		}
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	@Override
	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	@Override
	public String getRemoteHost() {
		return this.remoteHost;
	}

	@Override
	public void setAttribute(String name, Object value) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		checkActive();
		Assert.notNull(name, "Attribute name must not be null");
		this.attributes.remove(name);
	}

	/**
	 * 清除所有此请求的属性.
	 */
	public void clearAttributes() {
		this.attributes.clear();
	}

	/**
	 * 在任何现有区域设置之前, 添加新的首选区域设置.
	 */
	public void addPreferredLocale(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		this.locales.add(0, locale);
	}

	/**
	 * 按降序设置首选区域设置列表, 有效替换任何现有区域设置.
	 */
	public void setPreferredLocales(List<Locale> locales) {
		Assert.notEmpty(locales, "Locale list must not be empty");
		this.locales.clear();
		this.locales.addAll(locales);
	}

	/**
	 * 返回在此模拟请求中配置的第一个首选{@linkplain Locale locale}.
	 * <p>如果未显式配置区域设置, 则此请求模拟的<em>服务器</em>的默认首选{@link Locale}为{@link Locale#ENGLISH}.
	 * <p>与Servlet规范相反, 此模拟实现<strong>不</strong>考虑通过{@code Accept-Language} header指定的任何区域设置.
	 */
	@Override
	public Locale getLocale() {
		return this.locales.get(0);
	}

	/**
	 * 返回此模拟请求中配置的首选{@linkplain Locale locales}的{@linkplain Enumeration枚举}.
	 * <p>如果未显式配置区域设置, 则此请求模拟的<em>服务器</em>的默认首选{@link Locale}为{@link Locale#ENGLISH}.
	 * <p>与Servlet规范相反, 此模拟实现<strong>不</strong>考虑通过{@code Accept-Language} header指定的任何区域设置.
	 */
	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(this.locales);
	}

	/**
	 * 设置boolean {@code secure}标志, 指示模拟请求是否是使用安全通道, 例如HTTPS.
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/**
	 * 如果{@link #setSecure secure}标志已设置为{@code true}或者{@link #getScheme scheme}为{@code https}, 则返回{@code true}.
	 */
	@Override
	public boolean isSecure() {
		return (this.secure || HTTPS.equalsIgnoreCase(this.scheme));
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return new MockRequestDispatcher(path);
	}

	@Override
	@Deprecated
	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	@Override
	public int getRemotePort() {
		return this.remotePort;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	@Override
	public String getLocalName() {
		return this.localName;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	@Override
	public String getLocalAddr() {
		return this.localAddr;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	@Override
	public int getLocalPort() {
		return this.localPort;
	}

	@Override
	public AsyncContext startAsync() {
		return startAsync(this, null);
	}

	@Override
	public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
		if (!this.asyncSupported) {
			throw new IllegalStateException("Async not supported");
		}
		this.asyncStarted = true;
		this.asyncContext = new MockAsyncContext(request, response);
		return this.asyncContext;
	}

	public void setAsyncStarted(boolean asyncStarted) {
		this.asyncStarted = asyncStarted;
	}

	@Override
	public boolean isAsyncStarted() {
		return this.asyncStarted;
	}

	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	@Override
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	public void setAsyncContext(MockAsyncContext asyncContext) {
		this.asyncContext = asyncContext;
	}

	@Override
	public AsyncContext getAsyncContext() {
		return this.asyncContext;
	}

	public void setDispatcherType(DispatcherType dispatcherType) {
		this.dispatcherType = dispatcherType;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return this.dispatcherType;
	}


	// ---------------------------------------------------------------------
	// HttpServletRequest interface
	// ---------------------------------------------------------------------

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	@Override
	public String getAuthType() {
		return this.authType;
	}

	public void setCookies(Cookie... cookies) {
		this.cookies = cookies;
	}

	@Override
	public Cookie[] getCookies() {
		return this.cookies;
	}

	/**
	 * 为给定名称添加header条目.
	 * <p>虽然此方法可以将任何{@code Object}作为参数, 但建议使用以下类型:
	 * <ul>
	 * <li>使用{@code toString()}转换的String或任何Object; see {@link #getHeader}.</li>
	 * <li>日期header的String, Number, 或Date; see {@link #getDateHeader}.</li>
	 * <li>整型header的String 或 Number; see {@link #getIntHeader}.</li>
	 * <li>multiple值的{@code String[]} 或{@code Collection<String>}; see {@link #getHeaders}.</li>
	 * </ul>
	 */
	public void addHeader(String name, Object value) {
		if (CONTENT_TYPE_HEADER.equalsIgnoreCase(name) && !this.headers.containsKey(CONTENT_TYPE_HEADER)) {
			setContentType(value.toString());
		}
		else {
			doAddHeaderValue(name, value, false);
		}
	}

	private void doAddHeaderValue(String name, Object value, boolean replace) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Assert.notNull(value, "Header value must not be null");
		if (header == null || replace) {
			header = new HeaderValueHolder();
			this.headers.put(name, header);
		}
		if (value instanceof Collection) {
			header.addValues((Collection<?>) value);
		}
		else if (value.getClass().isArray()) {
			header.addValueArray(value);
		}
		else {
			header.addValue(value);
		}
	}

	/**
	 * 返回给定的{@code name}的日期header的long时间戳.
	 * <p>如果内部值表示是String, 则此方法将尝试使用支持的日期格式将其解析为日期:
	 * <ul>
	 * <li>"EEE, dd MMM yyyy HH:mm:ss zzz"</li>
	 * <li>"EEE, dd-MMM-yy HH:mm:ss zzz"</li>
	 * <li>"EEE MMM dd HH:mm:ss yyyy"</li>
	 * </ul>
	 * 
	 * @param name header名称
	 */
	@Override
	public long getDateHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Date) {
			return ((Date) value).getTime();
		}
		else if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		else if (value instanceof String) {
			return parseDateHeader(name, (String) value);
		}
		else if (value != null) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is not a Date, Number, or String: " + value);
		}
		else {
			return -1L;
		}
	}

	private long parseDateHeader(String name, String value) {
		for (String dateFormat : DATE_FORMATS) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
			simpleDateFormat.setTimeZone(GMT);
			try {
				return simpleDateFormat.parse(value).getTime();
			}
			catch (ParseException ex) {
				// ignore
			}
		}
		throw new IllegalArgumentException("Cannot parse date value '" + value + "' for '" + name + "' header");
	}

	@Override
	public String getHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return (header != null ? header.getStringValue() : null);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		return Collections.enumeration(header != null ? header.getStringValues() : new LinkedList<String>());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(this.headers.keySet());
	}

	@Override
	public int getIntHeader(String name) {
		HeaderValueHolder header = HeaderValueHolder.getByName(this.headers, name);
		Object value = (header != null ? header.getValue() : null);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String) value);
		}
		else if (value != null) {
			throw new NumberFormatException("Value for header '" + name + "' is not a Number: " + value);
		}
		else {
			return -1;
		}
	}

	public void setMethod(String method) {
		this.method = method;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	@Override
	public String getPathInfo() {
		return this.pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return (this.pathInfo != null ? getRealPath(this.pathInfo) : null);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public String getQueryString() {
		return this.queryString;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	@Override
	public String getRemoteUser() {
		return this.remoteUser;
	}

	public void addUserRole(String role) {
		this.userRoles.add(role);
	}

	@Override
	public boolean isUserInRole(String role) {
		return (this.userRoles.contains(role) || (this.servletContext instanceof MockServletContext &&
				((MockServletContext) this.servletContext).getDeclaredRoles().contains(role)));
	}

	public void setUserPrincipal(Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	@Override
	public Principal getUserPrincipal() {
		return this.userPrincipal;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	@Override
	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	@Override
	public String getRequestURI() {
		return this.requestURI;
	}

	@Override
	public StringBuffer getRequestURL() {
		String scheme = getScheme();
		String server = getServerName();
		int port = getServerPort();
		String uri = getRequestURI();

		StringBuffer url = new StringBuffer(scheme).append("://").append(server);
		if (port > 0 && ((HTTP.equalsIgnoreCase(scheme) && port != 80) ||
				(HTTPS.equalsIgnoreCase(scheme) && port != 443))) {
			url.append(':').append(port);
		}
		if (StringUtils.hasText(uri)) {
			url.append(uri);
		}
		return url;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	@Override
	public String getServletPath() {
		return this.servletPath;
	}

	public void setSession(HttpSession session) {
		this.session = session;
		if (session instanceof MockHttpSession) {
			MockHttpSession mockSession = ((MockHttpSession) session);
			mockSession.access();
		}
	}

	@Override
	public HttpSession getSession(boolean create) {
		checkActive();
		// 如果无效, 则重置会话.
		if (this.session instanceof MockHttpSession && ((MockHttpSession) this.session).isInvalid()) {
			this.session = null;
		}
		// Create new session if necessary.
		if (this.session == null && create) {
			this.session = new MockHttpSession(this.servletContext);
		}
		return this.session;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	/**
	 * 如果会话是模拟会话, 则此(Servlet 3.1+)方法的实现会调用{@link MockHttpSession#changeSessionId()}.
	 * 否则它只返回当前的会话ID.
	 */
	public String changeSessionId() {
		Assert.isTrue(this.session != null, "The request does not have a session");
		if (this.session instanceof MockHttpSession) {
			return ((MockHttpSession) session).changeSessionId();
		}
		return this.session.getId();
	}

	public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
		this.requestedSessionIdValid = requestedSessionIdValid;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return this.requestedSessionIdValid;
	}

	public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
		this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return this.requestedSessionIdFromCookie;
	}

	public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
		this.requestedSessionIdFromURL = requestedSessionIdFromURL;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return this.requestedSessionIdFromURL;
	}

	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout() throws ServletException {
		this.userPrincipal = null;
		this.remoteUser = null;
		this.authType = null;
	}

	public void addPart(Part part) {
		this.parts.add(part.getName(), part);
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		return this.parts.getFirst(name);
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		List<Part> result = new LinkedList<Part>();
		for (List<Part> list : this.parts.values()) {
			result.addAll(list);
		}
		return result;
	}

}
