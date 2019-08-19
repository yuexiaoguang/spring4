package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;

import org.springframework.web.cors.CorsConfiguration;

/**
 * 协助创建映射到路径模式的{@link CorsConfiguration}实例.
 * 默认情况下, 允许{@code GET}, {@code HEAD}, 和{@code POST}请求的所有来源, header和凭据,
 * 而最长期限设置为30分钟.
 */
public class CorsRegistration {

	private final String pathPattern;

	private final CorsConfiguration config;


	/**
	 * 创建一个新的{@link CorsRegistration}, 允许指定路径的{@code GET}, {@code HEAD},
	 * 和{@code POST}请求的所有来源, header和凭据, 最长期限设置为 1800 秒 (30 分钟).
	 * 
	 * @param pathPattern CORS配置应该应用的路径;
	 * 支持精确路径映射URI (例如{@code "/admin"}) 以及Ant样式路径模式 (例如{@code "/admin/**"}).
	 */
	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// 默认 @CrossOrigin 注解 + 允许简单方法
		this.config = new CorsConfiguration().applyPermitDefaultValues();
	}


	/**
	 * 设置允许的来源, e.g. {@code "http://domain1.com"}.
	 * <p>特殊值 {@code "*"}允许所有域.
	 * <p>默认允许所有来源.
	 * <p><strong>Note:</strong> CORS检查使用"Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" header中的值,
	 * 以反映客户端发起的地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此类header.
	 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(Arrays.asList(origins));
		return this;
	}


	/**
	 * 设置允许的HTTP方法, e.g. {@code "GET"}, {@code "POST"}, etc.
	 * <p>特殊值{@code "*"}允许所有方法.
	 * <p>默认允许"简单"方法 {@code GET}, {@code HEAD}, 和{@code POST}.
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(Arrays.asList(methods));
		return this;
	}

	/**
	 * 设置在实际请求期间pre-flight请求列出的允许使用的header列表.
	 * <p>特殊值{@code "*"} 可用于允许所有header.
	 * <p>如果header名称是其中之一, 则不需要列出header名称:
	 * 根据CORS规范, {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, 或{@code Pragma}.
	 * <p>默认允许所有header.
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 设置"简单" header以外的响应header列表, i.e.
	 * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
	 * {@code Expires}, {@code Last-Modified}, 或{@code Pragma}, 实际的反应可能会暴露出来.
	 * <p>请注意, 此属性不支持{@code "*"}.
	 * <p>默认未设置.
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 配置客户端缓存来自pre-flight请求的响应的时间长度, 以秒为单位.
	 * <p>默认为 1800 秒 (30 分钟).
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * 是否支持用户凭据.
	 * <p>默认为{@code true}, 支持用户凭据.
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	protected String getPathPattern() {
		return this.pathPattern;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}
}
