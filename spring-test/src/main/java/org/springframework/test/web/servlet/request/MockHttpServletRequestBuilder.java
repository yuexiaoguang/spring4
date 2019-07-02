package org.springframework.test.web.servlet.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;

import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link MockHttpServletRequest}的默认构建器, 需要作为在{@link MockMvc}中执行请求的输入.
 *
 * <p>应用程序测试通常会通过{@link MockMvcRequestBuilders}中的静态工厂方法访问此构建器.
 *
 * <p>此类不适用于扩展. 要将自定义初始化应用于创建的{@code MockHttpServletRequest}, 请使用{@link #with(RequestPostProcessor)}扩展点.
 */
public class MockHttpServletRequestBuilder
		implements ConfigurableSmartRequestBuilder<MockHttpServletRequestBuilder>, Mergeable {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();


	private final String method;

	private final URI url;

	private String contextPath = "";

	private String servletPath = "";

	private String pathInfo = "";

	private Boolean secure;

	private Principal principal;

	private MockHttpSession session;

	private String characterEncoding;

	private byte[] content;

	private String contentType;

	private final MultiValueMap<String, Object> headers = new LinkedMultiValueMap<String, Object>();

	private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();

	private final List<Cookie> cookies = new ArrayList<Cookie>();

	private final List<Locale> locales = new ArrayList<Locale>();

	private final Map<String, Object> requestAttributes = new LinkedHashMap<String, Object>();

	private final Map<String, Object> sessionAttributes = new LinkedHashMap<String, Object>();

	private final Map<String, Object> flashAttributes = new LinkedHashMap<String, Object>();

	private final List<RequestPostProcessor> postProcessors = new ArrayList<RequestPostProcessor>();


	/**
	 * 要获取实例, 请在{@link MockMvcRequestBuilders}中使用静态工厂方法.
	 * <p>虽然无法扩展此类, 但可以通过{@link #with(RequestPostProcessor)}插入初始化{@code MockHttpServletRequest}的其他方法.
	 * 
	 * @param httpMethod HTTP方法 (GET, POST, etc)
	 * @param url URL模板; 生成的URL将被编码
	 * @param vars 零个或多个URI变量
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, String url, Object... vars) {
		this(httpMethod.name(), UriComponentsBuilder.fromUriString(url).buildAndExpand(vars).encode().toUri());
	}

	/**
	 * 使用预构建的URI替代{@link #MockHttpServletRequestBuilder(HttpMethod, String, Object...)}.
	 * 
	 * @param httpMethod HTTP方法 (GET, POST, etc)
	 * @param url the URL
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, URI url) {
		this(httpMethod.name(), url);
	}

	/**
	 * 自定义HTTP方法的替代构造函数.
	 * 
	 * @param httpMethod HTTP方法 (GET, POST, etc)
	 * @param url the URL
	 */
	MockHttpServletRequestBuilder(String httpMethod, URI url) {
		Assert.notNull(httpMethod, "'httpMethod' is required");
		Assert.notNull(url, "'url' is required");
		this.method = httpMethod;
		this.url = url;
	}


	/**
	 * 指定requestURI的表示上下文路径的部分.
	 * 如果指定了上下文路径, 则必须与请求URI的开头匹配.
	 * <p>在大多数情况下, 可以通过省略requestURI的上下文路径来编写测试.
	 * 这是因为大多数应用程序实际上并不依赖于它们的部署名称.
	 * 如果在此处指定, 则上下文路径必须以"/"开头, 并且不得以 "/"结尾.
	 */
	public MockHttpServletRequestBuilder contextPath(String contextPath) {
		if (StringUtils.hasText(contextPath)) {
			Assert.isTrue(contextPath.startsWith("/"), "Context path must start with a '/'");
			Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with a '/'");
		}
		this.contextPath = (contextPath != null ? contextPath : "");
		return this;
	}

	/**
	 * 指定requestURI的表示Servlet映射到的路径的部分.
	 * 这通常是上下文路径之后的requestURI的一部分.
	 * <p>在大多数情况下, 可以通过省略requestURI中的servlet路径来编写测试.
	 * 这是因为大多数应用程序实际上并不依赖于servlet映射到的前缀.
	 * 例如, 如果Servlet映射到{@code "/main/*"}, 则可以使用requestURI {@code "/accounts/1"}编写测试, 而不是{@code "/main/accounts/1"}.
	 * 如果在此处指定, 则servletPath必须以"/"开头, 并且不得以 "/"结尾.
	 */
	public MockHttpServletRequestBuilder servletPath(String servletPath) {
		if (StringUtils.hasText(servletPath)) {
			Assert.isTrue(servletPath.startsWith("/"), "Servlet path must start with a '/'");
			Assert.isTrue(!servletPath.endsWith("/"), "Servlet path must not end with a '/'");
		}
		this.servletPath = (servletPath != null ? servletPath : "");
		return this;
	}

	/**
	 * 指定requestURI的表示pathInfo的部分.
	 * <p>如果未指定 (推荐), 将通过从requestURI中删除contextPath和servletPath并使用任何剩余部分来自动派生pathInfo.
	 * 如果在此处指定, 则pathInfo必须以 "/"开头.
	 * <p>如果指定, 则pathInfo将按原样使用.
	 */
	public MockHttpServletRequestBuilder pathInfo(String pathInfo) {
		if (StringUtils.hasText(pathInfo)) {
			Assert.isTrue(pathInfo.startsWith("/"), "Path info must start with a '/'");
		}
		this.pathInfo = pathInfo;
		return this;
	}

	/**
	 * 设置{@link ServletRequest}的安全属性, 指示使用安全通道, 例如HTTPS.
	 * 
	 * @param secure 请求是否使用安全通道
	 */
	public MockHttpServletRequestBuilder secure(boolean secure){
		this.secure = secure;
		return this;
	}

	/**
	 * 设置请求的字符编码.
	 * 
	 * @param encoding 字符编码
	 */
	public MockHttpServletRequestBuilder characterEncoding(String encoding) {
		this.characterEncoding = encoding;
		return this;
	}

	/**
	 * 设置请求正文.
	 * 
	 * @param content 正文内容
	 */
	public MockHttpServletRequestBuilder content(byte[] content) {
		this.content = content;
		return this;
	}

	/**
	 * 设置请求正文, 使用UTF-8字符串.
	 * 
	 * @param content 正文内容
	 */
	public MockHttpServletRequestBuilder content(String content) {
		this.content = content.getBytes(UTF8_CHARSET);
		return this;
	}

	/**
	 * 设置请求的'Content-Type' header.
	 * 
	 * @param contentType 内容类型
	 */
	public MockHttpServletRequestBuilder contentType(MediaType contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType.toString();
		return this;
	}

	/**
	 * 设置请求的'Content-Type' header.
	 * 
	 * @param contentType 内容类型
	 */
	public MockHttpServletRequestBuilder contentType(String contentType) {
		this.contentType = MediaType.parseMediaType(contentType).toString();
		return this;
	}

	/**
	 * 设置'Accept' header.
	 * 
	 * @param mediaTypes 媒体类型
	 */
	public MockHttpServletRequestBuilder accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		this.headers.set("Accept", MediaType.toString(Arrays.asList(mediaTypes)));
		return this;
	}

	/**
	 * 设置'Accept' header.
	 * 
	 * @param mediaTypes 媒体类型
	 */
	public MockHttpServletRequestBuilder accept(String... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		List<MediaType> result = new ArrayList<MediaType>(mediaTypes.length);
		for (String mediaType : mediaTypes) {
			result.add(MediaType.parseMediaType(mediaType));
		}
		this.headers.set("Accept", MediaType.toString(result));
		return this;
	}

	/**
	 * 为请求添加header. 始终添加值.
	 * 
	 * @param name header名称
	 * @param values 一个或多个header值
	 */
	public MockHttpServletRequestBuilder header(String name, Object... values) {
		addToMultiValueMap(this.headers, name, values);
		return this;
	}

	/**
	 * 将所有header添加到请求中. 始终添加值.
	 * 
	 * @param httpHeaders 要添加的header和值
	 */
	public MockHttpServletRequestBuilder headers(HttpHeaders httpHeaders) {
		for (String name : httpHeaders.keySet()) {
			Object[] values = ObjectUtils.toObjectArray(httpHeaders.get(name).toArray());
			addToMultiValueMap(this.headers, name, values);
		}
		return this;
	}

	/**
	 * 将请求参数添加到{@link MockHttpServletRequest}.
	 * <p>如果多次调用, 则会将新值添加到现有值.
	 * 
	 * @param name 参数名
	 * @param values 一个或多个值
	 */
	public MockHttpServletRequestBuilder param(String name, String... values) {
		addToMultiValueMap(this.parameters, name, values);
		return this;
	}

	/**
	 * 将请求参数的Map添加到{@link MockHttpServletRequest}, 例如在测试表单提交时.
	 * <p>如果多次调用, 则会将新值添加到现有值.
	 * 
	 * @param params 要添加的参数
	 */
	public MockHttpServletRequestBuilder params(MultiValueMap<String, String> params) {
		for (String name : params.keySet()) {
			for (String value : params.get(name)) {
				this.parameters.add(name, value);
			}
		}
		return this;
	}

	/**
	 * 将给定的cookie添加到请求中. 总是添加Cookie.
	 * 
	 * @param cookies 要添加的cookie
	 */
	public MockHttpServletRequestBuilder cookie(Cookie... cookies) {
		Assert.notEmpty(cookies, "'cookies' must not be empty");
		this.cookies.addAll(Arrays.asList(cookies));
		return this;
	}

	/**
	 * 将指定的语言环境添加为首选请求语言环境.
	 * 
	 * @param locales 要添加的语言环境
	 */
	public MockHttpServletRequestBuilder locale(Locale... locales) {
		Assert.notEmpty(locales, "'locales' must not be empty");
		this.locales.addAll(Arrays.asList(locales));
		return this;
	}

	/**
	 * 设置请求的语言环境, 覆盖以前的任何语言环境.
	 * 
	 * @param locale 语言环境, 或{@code null}重置它
	 */
	public MockHttpServletRequestBuilder locale(Locale locale) {
		this.locales.clear();
		if (locale != null) {
			this.locales.add(locale);
		}
		return this;
	}

	/**
	 * 设置请求属性.
	 * 
	 * @param name 属性名称
	 * @param value 属性值
	 */
	public MockHttpServletRequestBuilder requestAttr(String name, Object value) {
		addToMap(this.requestAttributes, name, value);
		return this;
	}

	/**
	 * 设置会话属性.
	 * 
	 * @param name 属性名称
	 * @param value 属性值
	 */
	public MockHttpServletRequestBuilder sessionAttr(String name, Object value) {
		addToMap(this.sessionAttributes, name, value);
		return this;
	}

	/**
	 * 设置会话属性.
	 * 
	 * @param sessionAttributes 会话属性
	 */
	public MockHttpServletRequestBuilder sessionAttrs(Map<String, Object> sessionAttributes) {
		Assert.notEmpty(sessionAttributes, "'sessionAttributes' must not be empty");
		for (String name : sessionAttributes.keySet()) {
			sessionAttr(name, sessionAttributes.get(name));
		}
		return this;
	}

	/**
	 * 设置"input" flash属性.
	 * 
	 * @param name 属性名称
	 * @param value 属性值
	 */
	public MockHttpServletRequestBuilder flashAttr(String name, Object value) {
		addToMap(this.flashAttributes, name, value);
		return this;
	}

	/**
	 * 设置flash属性.
	 * 
	 * @param flashAttributes 属性
	 */
	public MockHttpServletRequestBuilder flashAttrs(Map<String, Object> flashAttributes) {
		Assert.notEmpty(flashAttributes, "'flashAttributes' must not be empty");
		for (String name : flashAttributes.keySet()) {
			flashAttr(name, flashAttributes.get(name));
		}
		return this;
	}

	/**
	 * 设置要使用的HTTP会话, 可能在请求之间重复使用.
	 * <p>通过{@link #sessionAttr(String, Object)}提供的各个属性会覆盖此处提供的会话内容.
	 * 
	 * @param session HTTP会话
	 */
	public MockHttpServletRequestBuilder session(MockHttpSession session) {
		Assert.notNull(session, "'session' must not be null");
		this.session = session;
		return this;
	}

	/**
	 * 设置请求的主体.
	 * 
	 * @param principal 主体
	 */
	public MockHttpServletRequestBuilder principal(Principal principal) {
		Assert.notNull(principal, "'principal' must not be null");
		this.principal = principal;
		return this;
	}

	/**
	 * 以未直接构建到{@code MockHttpServletRequestBuilder}的方式进一步初始化{@link MockHttpServletRequest}的扩展点.
	 * 此接口的实现本身可以具有构建器样式的方法, 并且可以通过静态工厂方法访问.
	 * 
	 * @param postProcessor 要添加的后处理器
	 */
	@Override
	public MockHttpServletRequestBuilder with(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "postProcessor is required");
		this.postProcessors.add(postProcessor);
		return this;
	}


	/**
	 * {@inheritDoc}
	 * @return 总是返回{@code true}.
	 */
	@Override
	public boolean isMergeEnabled() {
		return true;
	}

	/**
	 * 合并"父级" RequestBuilder的属性, 仅当尚未在"this"实例中设置时才接受值.
	 * 
	 * @param parent 从中继承属性的父级{@code RequestBuilder}
	 * 
	 * @return 合并后的结果
	 */
	@Override
	public Object merge(Object parent) {
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof MockHttpServletRequestBuilder)) {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		MockHttpServletRequestBuilder parentBuilder = (MockHttpServletRequestBuilder) parent;

		if (!StringUtils.hasText(this.contextPath)) {
			this.contextPath = parentBuilder.contextPath;
		}
		if (!StringUtils.hasText(this.servletPath)) {
			this.servletPath = parentBuilder.servletPath;
		}
		if ("".equals(this.pathInfo)) {
			this.pathInfo = parentBuilder.pathInfo;
		}

		if (this.secure == null) {
			this.secure = parentBuilder.secure;
		}
		if (this.principal == null) {
			this.principal = parentBuilder.principal;
		}
		if (this.session == null) {
			this.session = parentBuilder.session;
		}

		if (this.characterEncoding == null) {
			this.characterEncoding = parentBuilder.characterEncoding;
		}
		if (this.content == null) {
			this.content = parentBuilder.content;
		}
		if (this.contentType == null) {
			this.contentType = parentBuilder.contentType;
		}

		for (String headerName : parentBuilder.headers.keySet()) {
			if (!this.headers.containsKey(headerName)) {
				this.headers.put(headerName, parentBuilder.headers.get(headerName));
			}
		}
		for (String paramName : parentBuilder.parameters.keySet()) {
			if (!this.parameters.containsKey(paramName)) {
				this.parameters.put(paramName, parentBuilder.parameters.get(paramName));
			}
		}
		for (Cookie cookie : parentBuilder.cookies) {
			if (!containsCookie(cookie)) {
				this.cookies.add(cookie);
			}
		}
		for (Locale locale : parentBuilder.locales) {
			if (!this.locales.contains(locale)) {
				this.locales.add(locale);
			}
		}

		for (String attributeName : parentBuilder.requestAttributes.keySet()) {
			if (!this.requestAttributes.containsKey(attributeName)) {
				this.requestAttributes.put(attributeName, parentBuilder.requestAttributes.get(attributeName));
			}
		}
		for (String attributeName : parentBuilder.sessionAttributes.keySet()) {
			if (!this.sessionAttributes.containsKey(attributeName)) {
				this.sessionAttributes.put(attributeName, parentBuilder.sessionAttributes.get(attributeName));
			}
		}
		for (String attributeName : parentBuilder.flashAttributes.keySet()) {
			if (!this.flashAttributes.containsKey(attributeName)) {
				this.flashAttributes.put(attributeName, parentBuilder.flashAttributes.get(attributeName));
			}
		}

		this.postProcessors.addAll(0, parentBuilder.postProcessors);

		return this;
	}

	private boolean containsCookie(Cookie cookie) {
		for (Cookie cookieToCheck : this.cookies) {
			if (ObjectUtils.nullSafeEquals(cookieToCheck.getName(), cookie.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 构建{@link MockHttpServletRequest}.
	 */
	@Override
	public final MockHttpServletRequest buildRequest(ServletContext servletContext) {
		MockHttpServletRequest request = createServletRequest(servletContext);

		request.setAsyncSupported(true);
		request.setMethod(this.method);

		String requestUri = this.url.getRawPath();
		request.setRequestURI(requestUri);

		if (this.url.getScheme() != null) {
			request.setScheme(this.url.getScheme());
		}
		if (this.url.getHost() != null) {
			request.setServerName(this.url.getHost());
		}
		if (this.url.getPort() != -1) {
			request.setServerPort(this.url.getPort());
		}

		updatePathRequestProperties(request, requestUri);

		if (this.secure != null) {
			request.setSecure(this.secure);
		}
		if (this.principal != null) {
			request.setUserPrincipal(this.principal);
		}
		if (this.session != null) {
			request.setSession(this.session);
		}

		request.setCharacterEncoding(this.characterEncoding);
		request.setContent(this.content);
		request.setContentType(this.contentType);

		for (String name : this.headers.keySet()) {
			for (Object value : this.headers.get(name)) {
				request.addHeader(name, value);
			}
		}

		if (this.url.getRawQuery() != null) {
			request.setQueryString(this.url.getRawQuery());
		}
		addRequestParams(request, UriComponentsBuilder.fromUri(this.url).build().getQueryParams());

		for (String name : this.parameters.keySet()) {
			for (String value : this.parameters.get(name)) {
				request.addParameter(name, value);
			}
		}

		if (this.content != null && this.content.length > 0) {
			String requestContentType = request.getContentType();
			if (requestContentType != null) {
				MediaType mediaType = MediaType.parseMediaType(requestContentType);
				if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
					addRequestParams(request, parseFormData(mediaType));
				}
			}
		}

		if (!ObjectUtils.isEmpty(this.cookies)) {
			request.setCookies(this.cookies.toArray(new Cookie[this.cookies.size()]));
		}
		if (!ObjectUtils.isEmpty(this.locales)) {
			request.setPreferredLocales(this.locales);
		}

		for (String name : this.requestAttributes.keySet()) {
			request.setAttribute(name, this.requestAttributes.get(name));
		}
		for (String name : this.sessionAttributes.keySet()) {
			request.getSession().setAttribute(name, this.sessionAttributes.get(name));
		}

		FlashMap flashMap = new FlashMap();
		flashMap.putAll(this.flashAttributes);
		FlashMapManager flashMapManager = getFlashMapManager(request);
		flashMapManager.saveOutputFlashMap(flashMap, request, new MockHttpServletResponse());

		return request;
	}

	/**
	 * 根据提供的{@code ServletContext}创建一个新的{@link MockHttpServletRequest}.
	 * <p>可以在子类中重写.
	 */
	protected MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		return new MockHttpServletRequest(servletContext);
	}

	/**
	 * 更新请求的contextPath, servletPath和pathInfo.
	 */
	private void updatePathRequestProperties(MockHttpServletRequest request, String requestUri) {
		if (!requestUri.startsWith(this.contextPath)) {
			throw new IllegalArgumentException(
					"Request URI [" + requestUri + "] does not start with context path [" + this.contextPath + "]");
		}
		request.setContextPath(this.contextPath);
		request.setServletPath(this.servletPath);

		if ("".equals(this.pathInfo)) {
			if (!requestUri.startsWith(this.contextPath + this.servletPath)) {
				throw new IllegalArgumentException(
						"Invalid servlet path [" + this.servletPath + "] for request URI [" + requestUri + "]");
			}
			String extraPath = requestUri.substring(this.contextPath.length() + this.servletPath.length());
			this.pathInfo = (StringUtils.hasText(extraPath) ?
					urlPathHelper.decodeRequestString(request, extraPath) : null);
		}
		request.setPathInfo(this.pathInfo);
	}

	private void addRequestParams(MockHttpServletRequest request, MultiValueMap<String, String> map) {
		try {
			for (Entry<String, List<String>> entry : map.entrySet()) {
				for (String value : entry.getValue()) {
					value = (value != null) ? UriUtils.decode(value, "UTF-8") : null;
					request.addParameter(UriUtils.decode(entry.getKey(), "UTF-8"), value);
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			// shouldn't happen
		}
	}

	private MultiValueMap<String, String> parseFormData(final MediaType mediaType) {
		HttpInputMessage message = new HttpInputMessage() {
			@Override
			public InputStream getBody() throws IOException {
				return new ByteArrayInputStream(content);
			}
			@Override
			public HttpHeaders getHeaders() {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(mediaType);
				return headers;
			}
		};

		try {
			return new FormHttpMessageConverter().read(null, message);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to parse form data in request body", ex);
		}
	}

	private FlashMapManager getFlashMapManager(MockHttpServletRequest request) {
		FlashMapManager flashMapManager = null;
		try {
			ServletContext servletContext = request.getServletContext();
			WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			flashMapManager = wac.getBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
		}
		catch (IllegalStateException ex) {
			// ignore
		}
		catch (NoSuchBeanDefinitionException ex) {
			// ignore
		}
		return (flashMapManager != null ? flashMapManager : new SessionFlashMapManager());
	}

	@Override
	public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
		for (RequestPostProcessor postProcessor : this.postProcessors) {
			request = postProcessor.postProcessRequest(request);
			if (request == null) {
				throw new IllegalStateException(
						"Post-processor [" + postProcessor.getClass().getName() + "] returned null");
			}
		}
		return request;
	}


	private static void addToMap(Map<String, Object> map, String name, Object value) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(value, "'value' must not be null");
		map.put(name, value);
	}

	private static <T> void addToMultiValueMap(MultiValueMap<String, T> map, String name, T[] values) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notEmpty(values, "'values' must not be empty");
		for (T value : values) {
			map.add(name, value);
		}
	}

}
