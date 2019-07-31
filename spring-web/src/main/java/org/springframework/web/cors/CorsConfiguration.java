package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于CORS配置的容器, 以及检查给定请求的实际来源, HTTP方法和header的方法.
 *
 * <p>默认情况下, 新创建的{@code CorsConfiguration}不允许任何跨源请求, 必须明确配置以指示应允许的内容.
 *
 * <p>使用{@link #applyPermitDefaultValues()}将初始化模型翻转, 以允许所有跨GET, HEAD和POST请求的跨源请求.
 */
public class CorsConfiguration {

	/**
	 * 表示<em>所有</em> 来源, 方法, 或header的通配符.
	 */
	public static final String ALL = "*";

	private static final List<HttpMethod> DEFAULT_METHODS;

	static {
		List<HttpMethod> rawMethods = new ArrayList<HttpMethod>(2);
		rawMethods.add(HttpMethod.GET);
		rawMethods.add(HttpMethod.HEAD);
		DEFAULT_METHODS = Collections.unmodifiableList(rawMethods);
	}


	private List<String> allowedOrigins;

	private List<String> allowedMethods;

	private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;

	private List<String> allowedHeaders;

	private List<String> exposedHeaders;

	private Boolean allowCredentials;

	private Long maxAge;


	/**
	 * 构造一个新的{@code CorsConfiguration}实例, 默认情况下不允许任何源的跨源请求.
	 */
	public CorsConfiguration() {
	}

	/**
	 * 通过复制提供的{@code CorsConfiguration}中的所有值来构造新的{@code CorsConfiguration}实例.
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedMethods = other.allowedMethods;
		this.resolvedMethods = other.resolvedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.maxAge = other.maxAge;
	}


	/**
	 * 设置允许的来源, e.g. {@code "http://domain1.com"}.
	 * <p>{@code "*"}允许所有域名.
	 * <p>默认不设置.
	 * <p><strong>Note:</strong> CORS检查"Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto" header中的值,
	 * 以反映客户端发起的地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此类header.
	 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
	 */
	public void setAllowedOrigins(List<String> allowedOrigins) {
		this.allowedOrigins = (allowedOrigins != null ? new ArrayList<String>(allowedOrigins) : null);
	}

	/**
	 * 返回允许的来源, 或{@code null}.
	 */
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * 添加允许的来源.
	 */
	public void addAllowedOrigin(String origin) {
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<String>(4);
		}
		this.allowedOrigins.add(origin);
	}

	/**
	 * 设置允许的HTTP方法, e.g. {@code "GET"}, {@code "POST"}, {@code "PUT"}, etc.
	 * <p>{@code "*"}允许所有方法.
	 * <p>如果未设置, 则仅允许{@code "GET"}和{@code "HEAD"}.
	 * <p>默认不设置.
	 */
	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = (allowedMethods != null ? new ArrayList<String>(allowedMethods) : null);
		if (!CollectionUtils.isEmpty(allowedMethods)) {
			this.resolvedMethods = new ArrayList<HttpMethod>(allowedMethods.size());
			for (String method : allowedMethods) {
				if (ALL.equals(method)) {
					this.resolvedMethods = null;
					break;
				}
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		}
		else {
			this.resolvedMethods = DEFAULT_METHODS;
		}
	}

	/**
	 * 返回允许的HTTP方法, 或{@code null}, 只允许{@code "GET"}和{@code "HEAD"}.
	 */
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * 添加允许的HTTP方法.
	 */
	public void addAllowedMethod(HttpMethod method) {
		if (method != null) {
			addAllowedMethod(method.name());
		}
	}

	/**
	 * 添加允许的HTTP方法.
	 */
	public void addAllowedMethod(String method) {
		if (StringUtils.hasText(method)) {
			if (this.allowedMethods == null) {
				this.allowedMethods = new ArrayList<String>(4);
				this.resolvedMethods = new ArrayList<HttpMethod>(4);
			}
			this.allowedMethods.add(method);
			if (ALL.equals(method)) {
				this.resolvedMethods = null;
			}
			else if (this.resolvedMethods != null) {
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		}
	}

	/**
	 * 设置在实际请求期间允许使用的pre-flight请求的header列表.
	 * <p>{@code "*"}允许实际请求发送任何header.
	 * <p>如果header名称是以下其中之一, 则不需要列出header名称:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires}, {@code Last-Modified}, 或{@code Pragma}.
	 * <p>默认未设置.
	 */
	public void setAllowedHeaders(List<String> allowedHeaders) {
		this.allowedHeaders = (allowedHeaders != null ? new ArrayList<String>(allowedHeaders) : null);
	}

	/**
	 * 返回允许的实际请求header, 或{@code null}.
	 */
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * 添加允许的实际请求header.
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<String>(4);
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * 设置除简单header之外的响应header列表
	 * (i.e.  {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
	 * {@code Expires}, {@code Last-Modified}, 或{@code Pragma}), 实际的反应可能会暴露出来.
	 * <p>{@code "*"}不是有效的公开header值.
	 * <p>默认不设置.
	 */
	public void setExposedHeaders(List<String> exposedHeaders) {
		if (exposedHeaders != null && exposedHeaders.contains(ALL)) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		this.exposedHeaders = (exposedHeaders != null ? new ArrayList<String>(exposedHeaders) : null);
	}

	/**
	 * 返回配置的要公开的响应header, 或{@code null}.
	 */
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * 添加要公开的响应header.
	 * <p>{@code "*"}不是有效的公开header值.
	 */
	public void addExposedHeader(String exposedHeader) {
		if (ALL.equals(exposedHeader)) {
			throw new IllegalArgumentException("'*' is not a valid exposed header value");
		}
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<String>(4);
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * 是否支持用户凭据.
	 * <p>默认不设置 (i.e. 不支持用户凭据).
	 */
	public void setAllowCredentials(Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * 返回配置的{@code allowCredentials}标志, 或{@code null}.
	 */
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * 配置客户端缓存来自pre-flight请求的响应的时间长度, 以秒为单位.
	 * <p>By default this is not set.
	 */
	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * 返回配置的{@code maxAge}值, 或{@code null}.
	 */
	public Long getMaxAge() {
		return this.maxAge;
	}

	/**
	 * 默认情况下, 新创建的{@code CorsConfiguration}不允许任何跨源请求, 必须明确配置以指示应允许的内容.
	 *
	 * <p>使用此方法以允许对GET, HEAD和POST请求的所有跨源请求.
	 * 但请注意, 此方法不会覆盖已设置的任何现有值.
	 *
	 * <p>如果尚未设置, 则应用以下默认值:
	 * <ul>
	 *     <li>允许所有来源, i.e. {@code "*"}.</li>
	 *     <li>允许"simple"方法 {@code GET}, {@code HEAD}和{@code POST}.</li>
	 *     <li>允许所有header.</li>
	 *     <li>允许凭据.</li>
	 *     <li>设置最大时长为1800 秒 (30 minutes).</li>
	 * </ul>
	 */
	public CorsConfiguration applyPermitDefaultValues() {
		if (this.allowedOrigins == null) {
			this.addAllowedOrigin(ALL);
		}
		if (this.allowedMethods == null) {
			this.setAllowedMethods(Arrays.asList(
					HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name()));
		}
		if (this.allowedHeaders == null) {
			this.addAllowedHeader(ALL);
		}
		if (this.allowCredentials == null) {
			this.setAllowCredentials(true);
		}
		if (this.maxAge == null) {
			this.setMaxAge(1800L);
		}
		return this;
	}

	/**
	 * 将提供的{@code CorsConfiguration}与此相结合.
	 * <p>此配置的属性将被提供的属性的任何非null属性覆盖.
	 * 
	 * @return 如果提供的配置为{@code null}, 则组合{@code CorsConfiguration}或{@code this}配置
	 */
	public CorsConfiguration combine(CorsConfiguration other) {
		if (other == null) {
			return this;
		}
		CorsConfiguration config = new CorsConfiguration(this);
		config.setAllowedOrigins(combine(getAllowedOrigins(), other.getAllowedOrigins()));
		config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
		config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
		config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		return config;
	}

	private List<String> combine(List<String> source, List<String> other) {
		if (other == null || other.contains(ALL)) {
			return source;
		}
		if (source == null || source.contains(ALL)) {
			return other;
		}
		Set<String> combined = new LinkedHashSet<String>(source);
		combined.addAll(other);
		return new ArrayList<String>(combined);
	}

	/**
	 * 根据配置的允许来源检查请求的来源.
	 * 
	 * @param requestOrigin 要检查的来源
	 * 
	 * @return 用于响应的来源, 或{@code null}意味着不允许请求来源
	 */
	public String checkOrigin(String requestOrigin) {
		if (!StringUtils.hasText(requestOrigin)) {
			return null;
		}
		if (ObjectUtils.isEmpty(this.allowedOrigins)) {
			return null;
		}

		if (this.allowedOrigins.contains(ALL)) {
			if (this.allowCredentials != Boolean.TRUE) {
				return ALL;
			}
			else {
				return requestOrigin;
			}
		}
		for (String allowedOrigin : this.allowedOrigins) {
			if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
				return requestOrigin;
			}
		}

		return null;
	}

	/**
	 * 针对配置的允许方法检查HTTP请求方法 (或来自pre-flight请求的{@code Access-Control-Request-Method} header中的方法).
	 * 
	 * @param requestMethod 要检查的HTTP请求方法
	 * 
	 * @return 要在pre-flight请求的响应中列出的HTTP方法列表, 或{@code null} 如果不允许提供的{@code requestMethod}
	 */
	public List<HttpMethod> checkHttpMethod(HttpMethod requestMethod) {
		if (requestMethod == null) {
			return null;
		}
		if (this.resolvedMethods == null) {
			return Collections.singletonList(requestMethod);
		}
		return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
	}

	/**
	 * 针对配置的允许header检查提供的请求header (或pre-flight请求的{@code Access-Control-Request-Headers}中列出的header).
	 * 
	 * @param requestHeaders 要检查的请求header
	 * 
	 * @return 要在pre-flight请求的响应中列出的允许header列表, 或{@code null} 如果不允许提供的请求header
	 */
	public List<String> checkHeaders(List<String> requestHeaders) {
		if (requestHeaders == null) {
			return null;
		}
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		if (ObjectUtils.isEmpty(this.allowedHeaders)) {
			return null;
		}

		boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
		List<String> result = new ArrayList<String>(requestHeaders.size());
		for (String requestHeader : requestHeaders) {
			if (StringUtils.hasText(requestHeader)) {
				requestHeader = requestHeader.trim();
				if (allowAnyHeader) {
					result.add(requestHeader);
				}
				else {
					for (String allowedHeader : this.allowedHeaders) {
						if (requestHeader.equalsIgnoreCase(allowedHeader)) {
							result.add(requestHeader);
							break;
						}
					}
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

}
