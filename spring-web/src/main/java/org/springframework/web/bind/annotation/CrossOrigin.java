package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.cors.CorsConfiguration;

/**
 * 将带注解的方法或类型标记为允许跨源请求.
 *
 * <p>默认情况下, 允许所有origin和header, 允许凭据, 并将最大时限设置为 1800秒 (30分钟).
 * 如果没有在{@code @CrossOrigin}上明确设置, 则将HTTP方法列表设置为{@code @RequestMapping}上的方法.
 *
 * <p><b>NOTE:</b> 如果配置了适当的{@code HandlerMapping}-{@code HandlerAdapter}对,
 * 例如{@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter}对,
 * 它们是MVC Java配置和MVC命名空间中的默认值, 则会处理{@code @CrossOrigin}.
 * 特别是{@code @CrossOrigin}不支持已弃用的
 * {@code DefaultAnnotationHandlerMapping}-{@code AnnotationMethodHandlerAdapter}对.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

	/**
	 * @deprecated as of Spring 4.3.4, in favor of using {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	String[] DEFAULT_ORIGINS = { "*" };

	/**
	 * @deprecated as of Spring 4.3.4, in favor of using {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	String[] DEFAULT_ALLOWED_HEADERS = { "*" };

	/**
	 * @deprecated as of Spring 4.3.4, in favor of using {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	boolean DEFAULT_ALLOW_CREDENTIALS = true;

	/**
	 * @deprecated as of Spring 4.3.4, in favor of using {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	long DEFAULT_MAX_AGE = 1800;


	/**
	 * Alias for {@link #origins}.
	 */
	@AliasFor("origins")
	String[] value() default {};

	/**
	 * 允许的来源列表, e.g. {@code "http://domain1.com"}.
	 * <p>这些值放在pre-flight响应和实际响应的{@code Access-Control-Allow-Origin} header中.
	 * {@code "*"}表示允许所有来源.
	 * <p>如果未定义, 则允许所有来源.
	 * <p><strong>Note:</strong> CORS检查使用"Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto" header中的值,
	 * 以反映客户端发起的地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此header.
	 * 有关此过滤器的更多信息, 参阅Spring Framework引用.
	 */
	@AliasFor("value")
	String[] origins() default {};

	/**
	 * 可在实际请求期间使用的请求header列表.
	 * <p>此属性控制 pre-flight响应的{@code Access-Control-Allow-Headers} header的值.
	 * {@code "*"}表示允许客户端请求的所有header.
	 * <p>如果未定义, 则允许所有请求的 header.
	 */
	String[] allowedHeaders() default {};

	/**
	 * 用户代理允许客户端访问的响应header列表.
	 * <p>此属性控制实际响应的{@code Access-Control-Expose-Headers} header的值.
	 * <p>如果未定义, 则使用空的公开header列表.
	 */
	String[] exposedHeaders() default {};

	/**
	 * 支持的HTTP请求方法列表, e.g. {@code "{RequestMethod.GET, RequestMethod.POST}"}.
	 * <p>此处指定的方法将覆盖通过{@code RequestMapping}指定的方法.
	 * <p>如果未定义, 则使用{@link RequestMapping}注解定义的方法.
	 */
	RequestMethod[] methods() default {};

	/**
	 * 浏览器是否应包含与被注解的请求的域相关联的任何cookie.
	 * <p>如果不包含此类cookie, 设置为{@code "false"}. 空字符串({@code ""})表示<em>未定义</em>.
	 * {@code "true"}表示pre-flight响应将包含 header {@code Access-Control-Allow-Credentials=true}.
	 * <p>如果未定义, 则允许凭据.
	 */
	String allowCredentials() default "";

	/**
	 * pre-flight响应的缓存的过期时间(以秒为单位).
	 * <p>此属性控制pre-flight响应中{@code Access-Control-Max-Age} header的值.
	 * <p>将其设置为合理的值可以减少浏览器所需的pre-flight请求/响应交互的数量.
	 * 负值表示<em>undefined</em>.
	 * <p>如果未定义, 最大过期时间是{@code 1800}秒 (i.e., 30 分钟).
	 */
	long maxAge() default -1;

}
