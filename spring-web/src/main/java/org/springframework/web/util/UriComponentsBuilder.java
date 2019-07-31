package org.springframework.web.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;

/**
 * {@link UriComponents}的构建器.
 *
 * <p>典型用法涉及:
 * <ol>
 * <li>使用其中一种静态工厂方法(例如{@link #fromPath(String)}或{@link #fromUri(URI)})
 * 创建{@code UriComponentsBuilder}</li>
 * <li>通过各自的方法设置各种URI组件 ({@link #scheme(String)},
 * {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 * {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and {@link #fragment(String)}.</li>
 * <li>使用{@link #build()}方法构建{@link UriComponents}实例.</li>
 * </ol>
 */
public class UriComponentsBuilder implements Cloneable {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String HTTP_PATTERN = "(?i)(http|https):";

	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}\\:\\.]*[%\\p{Alnum}]*\\]";

	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String LAST_PATTERN = "(.*)";

	// 匹配URI的正则表达式模式. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
					PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");


	private String scheme;

	private String ssp;

	private String userInfo;

	private String host;

	private String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();

	private String fragment;


	/**
	 * 防止直接实例化.
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * 深度克隆给定的UriComponentsBuilder.
	 * 
	 * @param other 要复制的其他构建器
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.queryParams.putAll(other.queryParams);
		this.fragment = other.fragment;
	}


	// Factory methods

	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * 创建使用给定路径初始化的构建器.
	 * 
	 * @param path 初始化的路径
	 * 
	 * @return 新的{@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * 创建使用给定的{@code URI}初始化的构建器.
	 * 
	 * @param uri 要初始化的URI
	 * 
	 * @return 新的{@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * 创建使用给定URI字符串初始化的构建器.
	 * <p><strong>Note:</strong> 保留字符的存在可能会阻止正确解析URI字符串.
	 * 例如, 如果查询参数包含{@code '='}或{@code '&'}字符, 则无法明确地解析查询字符串.
	 * 此类值应替换为URI变量以启用正确的解析:
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * 
	 * @param uri 要初始化的URI字符串
	 * 
	 * @return 新的{@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.notNull(uri, "URI must not be null");
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String userInfo = matcher.group(5);
			String host = matcher.group(6);
			String port = matcher.group(8);
			String path = matcher.group(9);
			String query = matcher.group(11);
			String fragment = matcher.group(13);
			boolean opaque = false;
			if (StringUtils.hasLength(scheme)) {
				String rest = uri.substring(scheme.length());
				if (!rest.startsWith(":/")) {
					opaque = true;
				}
			}
			builder.scheme(scheme);
			if (opaque) {
				String ssp = uri.substring(scheme.length()).substring(1);
				if (StringUtils.hasLength(fragment)) {
					ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
				}
				builder.schemeSpecificPart(ssp);
			}
			else {
				builder.userInfo(userInfo);
				builder.host(host);
				if (StringUtils.hasLength(port)) {
					builder.port(port);
				}
				builder.path(path);
				builder.query(query);
			}
			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * 从给定的HTTP URL String创建URI组件构建器.
	 * <p><strong>Note:</strong> 保留字符的存在可能会阻止正确解析URI字符串.
	 * 例如, 如果查询参数包含{@code '='}或{@code '&'}字符, 则无法明确地解析查询字符串.
	 * 此类值应替换为URI变量以启用正确的解析:
	 * <pre class="code">
	 * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * 
	 * @param httpUrl 源URI
	 * 
	 * @return URI的URI组件
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "HTTP URL must not be null");
		Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(1);
			builder.scheme(scheme != null ? scheme.toLowerCase() : null);
			builder.userInfo(matcher.group(4));
			String host = matcher.group(5);
			if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
				throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
			}
			builder.host(host);
			String port = matcher.group(7);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			builder.path(matcher.group(8));
			builder.query(matcher.group(10));
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * 从与给定HttpRequest关联的URI创建新的{@code UriComponents}对象,
	 * 同时还覆盖header "Forwarded"中的值 (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * 如果未找到"Forwarded", 则覆盖"X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto"中的值.
	 * <p><strong>Note:</strong> 此方法使用转发header中的值, 以反映客户端发起的协议和地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此header.
	 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
	 * 
	 * @param request 源请求
	 * 
	 * @return URI的URI组件
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
		return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
	}

	/**
	 * 通过解析HTTP请求的"Origin" header来创建实例.
	 */
	public static UriComponentsBuilder fromOriginHeader(String origin) {
		Matcher matcher = URI_PATTERN.matcher(origin);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String host = matcher.group(6);
			String port = matcher.group(8);
			if (StringUtils.hasLength(scheme)) {
				builder.scheme(scheme);
			}
			builder.host(host);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
		}
	}


	// build methods

	/**
	 * 从此构建器中包含的各种组件构建{@code UriComponents}实例.
	 * 
	 * @return URI组件
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * 从此构建器中包含的各种组件构建{@code UriComponents}实例.
	 * 
	 * @param encoded 是否对此构建器中设置的所有组件进行编码({@code true}) 或不进行编码 ({@code false})
	 * 
	 * @return URI组件
	 */
	public UriComponents build(boolean encoded) {
		if (this.ssp != null) {
			return new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		}
		else {
			return new HierarchicalUriComponents(this.scheme, this.userInfo, this.host, this.port,
					this.pathBuilder.build(), this.queryParams, this.fragment, encoded, true);
		}
	}

	/**
	 * 构建一个{@code UriComponents}实例, 并用Map中的值替换URI模板变量.
	 * 这是一种快捷方法, 它结合了对{@link #build()}和{@link UriComponents#expand(Map)}的调用.
	 * 
	 * @param uriVariables URI变量的Map
	 * 
	 * @return 具有扩展值的URI组件
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build(false).expand(uriVariables);
	}

	/**
	 * 构建一个{@code UriComponents}实例, 并使用数组中的值替换URI模板变量.
	 * 这是一种快捷方法, 它结合了对{@link #build()}和{@link UriComponents#expand(Object...)}的调用.
	 * 
	 * @param uriVariableValues URI变量值
	 * 
	 * @return 具有扩展值的URI组件
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build(false).expand(uriVariableValues);
	}

	/**
	 * 构建URI字符串.
	 * 这是一种快捷方法, 它结合了对{@link #build()}, {@link UriComponents#encode()}和{@link UriComponents#toUriString()}的调用.
	 */
	public String toUriString() {
		return build(false).encode().toUriString();
	}


	// Instance methods

	/**
	 * 从给定URI的组件初始化此构建器的组件.
	 * 
	 * @param uri the URI
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.scheme = uri.getScheme();
		if (uri.isOpaque()) {
			this.ssp = uri.getRawSchemeSpecificPart();
			resetHierarchicalComponents();
		}
		else {
			if (uri.getRawUserInfo() != null) {
				this.userInfo = uri.getRawUserInfo();
			}
			if (uri.getHost() != null) {
				this.host = uri.getHost();
			}
			if (uri.getPort() != -1) {
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.hasLength(uri.getRawPath())) {
				this.pathBuilder = new CompositePathComponentBuilder(uri.getRawPath());
			}
			if (StringUtils.hasLength(uri.getRawQuery())) {
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	/**
	 * 根据给定{@link UriComponents}实例的值设置或附加此构建器的各个URI组件.
	 * <p>对于每个组件的语义 (i.e. set vs append), 请检查此类的构建器方法.
	 * 例如{@link #host(String)}在{@link #path(String)}附加时设置.
	 * 
	 * @param uriComponents 要复制的UriComponents
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
		Assert.notNull(uriComponents, "UriComponents must not be null");
		uriComponents.copyToUriComponentsBuilder(this);
		return this;
	}

	/**
	 * 设置URI scheme.
	 * 给定的scheme可能包含URI模板变量, 也可能是{@code null}以清除此构建器的scheme.
	 * 
	 * @param scheme the URI scheme
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * 设置URI scheme-specific-part.
	 * 调用时, 此方法重写
	 * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
	 * {@linkplain #port(int) port}, {@linkplain #path(String) path}, 和{@link #query(String) query}.
	 * 
	 * @param ssp URI scheme-specific-part, 可以包含URI模板参数
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	/**
	 * 设置URI用户信息.
	 * 给定的用户信息可能包含URI模板变量, 也可能是{@code null}以清除此构建器的用户信息.
	 * 
	 * @param userInfo URI用户信息
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder userInfo(String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置URI主机.
	 * 给定的主机可能包含URI模板变量, 也可能是{@code null}以清除此构建器的主机.
	 * 
	 * @param host URI主机
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder host(String host) {
		this.host = host;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置URI端口.
	 * 传递{@code -1}将清除此构建器的端口.
	 * 
	 * @param port URI端口
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置URI端口.
	 * 仅当端口需要使用URI变量进行参数化时才使用此方法. 否则使用{@link #port(int)}.
	 * 传递{@code null}将清除此构建器的端口.
	 * 
	 * @param port URI端口
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(String port) {
		this.port = port;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 将给定路径附加到此构建器的现有路径.
	 * 给定路径可能包含URI模板变量.
	 * 
	 * @param path URI路径
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 将路径分段附加到现有路径.
	 * 每个路径分段可能包含URI模板变量, 不应包含任何斜杠.
	 * 随后使用{@code path("/")}确保尾部斜杠.
	 * 
	 * @param pathSegments URI路径分段
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置此构建器的路径, 覆盖所有现有路径和路径分段值.
	 * 
	 * @param path URI路径 ({@code null}值导致空路径)
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replacePath(String path) {
		this.pathBuilder = new CompositePathComponentBuilder(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 将给定查询附加到此构建器的现有查询.
	 * 给定的查询可能包含URI模板变量.
	 * <p><strong>Note:</strong> 保留字符的存在可能会阻止正确解析URI字符串.
	 * 例如, 如果查询参数包含{@code '='}或{@code '&'}字符, 则无法明确地解析查询字符串.
	 * 此类值应替换为URI变量以启用正确的解析:
	 * <pre class="code">
	 * UriComponentsBuilder.fromUriString(&quot;/hotels/42&quot;)
	 * 	.query(&quot;filter={value}&quot;)
	 * 	.buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * 
	 * @param query 查询字符串
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder query(String query) {
		if (query != null) {
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
			}
		}
		else {
			this.queryParams.clear();
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置此构建器的查询将覆盖所有现有查询参数.
	 * 
	 * @param query 查询字符串; {@code null}值将删除所有查询参数.
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQuery(String query) {
		this.queryParams.clear();
		query(query);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 将给定的查询参数附加到现有查询参数.
	 * 给定名称或任何值可能包含URI模板变量.
	 * 如果没有给出值, 则生成的URI将仅包含查询参数名称 (i.e. {@code ?foo} 而不是 {@code ?foo=bar}).
	 * 
	 * @param name 查询参数名称
	 * @param values 查询参数值
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = (value != null ? value.toString() : null);
				this.queryParams.add(name, valueAsString);
			}
		}
		else {
			this.queryParams.add(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 添加给定的查询参数.
	 * 
	 * @param params 参数
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder queryParams(MultiValueMap<String, String> params) {
		if (params != null) {
			this.queryParams.putAll(params);
		}
		return this;
	}

	/**
	 * 设置查询参数值, 覆盖同一参数的所有现有查询值.
	 * 如果未给出值, 则删除查询参数.
	 * 
	 * @param name 查询参数名称
	 * @param values 查询参数值
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		this.queryParams.remove(name);
		if (!ObjectUtils.isEmpty(values)) {
			queryParam(name, values);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * 设置查询参数值覆盖所有现有查询值.
	 * 
	 * @param params 查询参数名称
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replaceQueryParams(MultiValueMap<String, String> params) {
		this.queryParams.clear();
		if (params != null) {
			this.queryParams.putAll(params);
		}
		return this;
	}

	/**
	 * 设置URI片段.
	 * 给定的片段可能包含URI模板变量, 也可能是{@code null}以清除此构建器的片段.
	 * 
	 * @param fragment URI片段
	 * 
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder fragment(String fragment) {
		if (fragment != null) {
			Assert.hasLength(fragment, "Fragment must not be empty");
			this.fragment = fragment;
		}
		else {
			this.fragment = null;
		}
		return this;
	}

	/**
	 * 从给定的header中调整此构建器的scheme+host+port, 特别是"Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>,
	 * 如果"Forwarded"不存在, 则为"X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto".
	 * 
	 * @param headers 要考虑的HTTP header
	 * 
	 * @return this UriComponentsBuilder
	 */
	UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		try {
			String forwardedHeader = headers.getFirst("Forwarded");
			if (StringUtils.hasText(forwardedHeader)) {
				String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
				Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
				if (matcher.find()) {
					scheme(matcher.group(1).trim());
					port(null);
				}
				matcher = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
				if (matcher.find()) {
					adaptForwardedHost(matcher.group(1).trim());
				}
			}
			else {
				String protocolHeader = headers.getFirst("X-Forwarded-Proto");
				if (StringUtils.hasText(protocolHeader)) {
					scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
					port(null);
				}

				String hostHeader = headers.getFirst("X-Forwarded-Host");
				if (StringUtils.hasText(hostHeader)) {
					adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
				}

				String portHeader = headers.getFirst("X-Forwarded-Port");
				if (StringUtils.hasText(portHeader)) {
					port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
				}
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " +
					"If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
					"with the removeOnly=true. Request headers: " + headers);
		}

		if (this.scheme != null && ((this.scheme.equals("http") && "80".equals(this.port)) ||
				(this.scheme.equals("https") && "443".equals(this.port)))) {
			port(null);
		}

		return this;
	}

	private void adaptForwardedHost(String hostToUse) {
		int portSeparatorIdx = hostToUse.lastIndexOf(':');
		if (portSeparatorIdx > hostToUse.lastIndexOf(']')) {
			host(hostToUse.substring(0, portSeparatorIdx));
			port(Integer.parseInt(hostToUse.substring(portSeparatorIdx + 1)));
		}
		else {
			host(hostToUse);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


	/**
	 * 委托给{@link #cloneBuilder()}.
	 */
	@Override
	public Object clone() {
		return cloneBuilder();
	}

	/**
	 * 克隆此{@code UriComponentsBuilder}.
	 * 
	 * @return 克隆的{@code UriComponentsBuilder}对象
	 */
	public UriComponentsBuilder cloneBuilder() {
		return new UriComponentsBuilder(this);
	}


	private interface PathComponentBuilder {

		PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private final LinkedList<PathComponentBuilder> builders = new LinkedList<PathComponentBuilder>();

		public CompositePathComponentBuilder() {
		}

		public CompositePathComponentBuilder(String path) {
			addPath(path);
		}

		public void addPathSegments(String... pathSegments) {
			if (!ObjectUtils.isEmpty(pathSegments)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder == null) {
					psBuilder = new PathSegmentComponentBuilder();
					this.builders.add(psBuilder);
					if (fpBuilder != null) {
						fpBuilder.removeTrailingSlash();
					}
				}
				psBuilder.append(pathSegments);
			}
		}

		public void addPath(String path) {
			if (StringUtils.hasText(path)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder != null) {
					path = path.startsWith("/") ? path : "/" + path;
				}
				if (fpBuilder == null) {
					fpBuilder = new FullPathComponentBuilder();
					this.builders.add(fpBuilder);
				}
				fpBuilder.append(path);
			}
		}

		@SuppressWarnings("unchecked")
		private <T> T getLastBuilder(Class<T> builderClass) {
			if (!this.builders.isEmpty()) {
				PathComponentBuilder last = this.builders.getLast();
				if (builderClass.isInstance(last)) {
					return (T) last;
				}
			}
			return null;
		}

		@Override
		public PathComponent build() {
			int size = this.builders.size();
			List<PathComponent> components = new ArrayList<PathComponent>(size);
			for (PathComponentBuilder componentBuilder : this.builders) {
				PathComponent pathComponent = componentBuilder.build();
				if (pathComponent != null) {
					components.add(pathComponent);
				}
			}
			if (components.isEmpty()) {
				return HierarchicalUriComponents.NULL_PATH_COMPONENT;
			}
			if (components.size() == 1) {
				return components.get(0);
			}
			return new HierarchicalUriComponents.PathComponentComposite(components);
		}

		@Override
		public CompositePathComponentBuilder cloneBuilder() {
			CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
			for (PathComponentBuilder builder : this.builders) {
				compositeBuilder.builders.add(builder.cloneBuilder());
			}
			return compositeBuilder;
		}
	}


	private static class FullPathComponentBuilder implements PathComponentBuilder {

		private final StringBuilder path = new StringBuilder();

		public void append(String path) {
			this.path.append(path);
		}

		@Override
		public PathComponent build() {
			if (this.path.length() == 0) {
				return null;
			}
			String path = this.path.toString();
			while (true) {
				int index = path.indexOf("//");
				if (index == -1) {
					break;
				}
				path = path.substring(0, index) + path.substring(index + 1);
			}
			return new HierarchicalUriComponents.FullPathComponent(path);
		}

		public void removeTrailingSlash() {
			int index = this.path.length() - 1;
			if (this.path.charAt(index) == '/') {
				this.path.deleteCharAt(index);
			}
		}

		@Override
		public FullPathComponentBuilder cloneBuilder() {
			FullPathComponentBuilder builder = new FullPathComponentBuilder();
			builder.append(this.path.toString());
			return builder;
		}
	}


	private static class PathSegmentComponentBuilder implements PathComponentBuilder {

		private final List<String> pathSegments = new LinkedList<String>();

		public void append(String... pathSegments) {
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasText(pathSegment)) {
					this.pathSegments.add(pathSegment);
				}
			}
		}

		@Override
		public PathComponent build() {
			return (this.pathSegments.isEmpty() ? null :
					new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
		}

		@Override
		public PathSegmentComponentBuilder cloneBuilder() {
			PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
			builder.pathSegments.addAll(this.pathSegments);
			return builder;
		}
	}

}
