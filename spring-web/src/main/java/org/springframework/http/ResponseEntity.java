package org.springframework.http;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link HttpEntity}的扩展, 添加{@link HttpStatus}状态码.
 * 用于{@code RestTemplate}以及{@code @Controller}方法.
 *
 * <p>在{@code RestTemplate}中, 此类由
 * {@link org.springframework.web.client.RestTemplate#getForEntity getForEntity()}
 * 和{@link org.springframework.web.client.RestTemplate#exchange exchange()}返回:
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 *
 * <p>也可以在Spring MVC中使用, 作为@Controller方法的返回值:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * 或者, 通过使用可通过静态方法访问的构建器:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final Object status;


	public ResponseEntity(HttpStatus status) {
		this(null, null, status);
	}

	public ResponseEntity(T body, HttpStatus status) {
		this(body, null, status);
	}

	public ResponseEntity(MultiValueMap<String, String> headers, HttpStatus status) {
		this(null, headers, status);
	}

	public ResponseEntity(T body, MultiValueMap<String, String> headers, HttpStatus status) {
		super(body, headers);
		Assert.notNull(status, "HttpStatus must not be null");
		this.status = status;
	}

	/**
	 * 刚刚在嵌套构建器API后面使用.
	 * 
	 * @param body 实体正文
	 * @param headers 实体header
	 * @param status 状态码 (为{@code HttpStatus}或{@code Integer}值)
	 */
	private ResponseEntity(T body, MultiValueMap<String, String> headers, Object status) {
		super(body, headers);
		this.status = status;
	}


	/**
	 * 返回响应的HTTP状态码.
	 * 
	 * @return HTTP状态
	 */
	public HttpStatus getStatusCode() {
		if (this.status instanceof HttpStatus) {
			return (HttpStatus) this.status;
		}
		else {
			return HttpStatus.valueOf((Integer) this.status);
		}
	}

	/**
	 * 返回响应的HTTP状态码.
	 * 
	 * @return HTTP状态
	 */
	public int getStatusCodeValue() {
		if (this.status instanceof HttpStatus) {
			return ((HttpStatus) this.status).value();
		}
		else {
			return (Integer) this.status;
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
	}

	@Override
	public int hashCode() {
		return (super.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.status.toString());
		if (this.status instanceof HttpStatus) {
			builder.append(' ');
			builder.append(((HttpStatus) this.status).getReasonPhrase());
		}
		builder.append(',');
		T body = getBody();
		HttpHeaders headers = getHeaders();
		if (body != null) {
			builder.append(body);
			if (headers != null) {
				builder.append(',');
			}
		}
		if (headers != null) {
			builder.append(headers);
		}
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * 创建具有给定状态的构建器.
	 * 
	 * @param status 响应状态
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder status(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		return new DefaultBuilder(status);
	}

	/**
	 * 创建具有给定状态的构建器.
	 * 
	 * @param status 响应状态
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder status(int status) {
		return new DefaultBuilder(status);
	}

	/**
	 * 创建{@linkplain HttpStatus#OK OK}状态的构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * 使用给定正文和{@linkplain HttpStatus#OK OK}状态创建{@code ResponseEntity}的快捷方式.
	 * 
	 * @return 创建的{@code ResponseEntity}
	 */
	public static <T> ResponseEntity<T> ok(T body) {
		BodyBuilder builder = ok();
		return builder.body(body);
	}

	/**
	 * 使用{@linkplain HttpStatus#CREATED CREATED}状态和设置为给定URI的location header创建构建器.
	 * 
	 * @param location 位置 URI
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * 使用{@linkplain HttpStatus#ACCEPTED ACCEPTED}状态创建构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * 使用{@linkplain HttpStatus#NO_CONTENT NO_CONTENT}状态创建构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * 使用{@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST}状态创建构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 使用{@linkplain HttpStatus#NOT_FOUND NOT_FOUND}状态创建构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * 使用{@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY}状态创建构建器.
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}


	/**
	 * 定义将header添加到响应实体的构建器.
	 * 
	 * @param <B> 构建器子类
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加单个header值.
		 * 
		 * @param headerName 名称
		 * @param headerValues 值
		 * 
		 * @return 此构建器
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 将给定header复制到实体的header 映射中.
		 * 
		 * @param headers 要复制的现有HttpHeaders
		 * 
		 * @return 此构建器
		 */
		B headers(HttpHeaders headers);

		/**
		 * 设置允许的{@link HttpMethod HTTP方法}的集合, 由{@code Allow} header指定.
		 * 
		 * @param allowedMethods 允许的方法
		 * 
		 * @return 此构建器
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * 设置正文的实体标签, 由{@code ETag} header指定.
		 * 
		 * @param etag 新的实体标签
		 * 
		 * @return 此构建器
		 */
		B eTag(String etag);

		/**
		 * 设置资源上次更改的时间, 由{@code Last-Modified} header指定.
		 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
		 * 
		 * @param lastModified 最后修改的日期
		 * 
		 * @return 此构建器
		 */
		B lastModified(long lastModified);

		/**
		 * 设置资源的位置, 由{@code Location} header指定.
		 * 
		 * @param location 位置
		 * 
		 * @return 此构建器
		 */
		B location(URI location);

		/**
		 * 设置资源的缓存指令, 由HTTP 1.1 {@code Cache-Control} header指定.
		 * <p>可以像
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}一样构建{@code CacheControl}实例.
		 * 
		 * @param cacheControl 与缓存相关的HTTP响应header的构建器
		 * 
		 * @return 此构建器
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求 header名称 (e.g. "Accept-Language") 以添加到"Vary"响应header,
		 * 通知客户端响应受内容协商和基于给定请求header的值的差异的影响.
		 * 仅当响应"Vary" header中尚未存在时, 才会添加配置的请求header名称.
		 * 
		 * @param requestHeaders 请求header名称
		 */
		B varyBy(String... requestHeaders);

		/**
		 * 构建没有正文的响应实体.
		 * 
		 * @return 响应实体
		 */
		<T> ResponseEntity<T> build();
	}


	/**
	 * 定义将主体添加到响应实体的构建器.
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置正文的长度, 以字节为单位, 由{@code Content-Length} header指定.
		 * 
		 * @param contentLength 内容长度
		 * 
		 * @return 此构建器
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置正文的{@linkplain MediaType 媒体类型}, 由{@code Content-Type} header指定.
		 * 
		 * @param contentType 内容类型
		 * 
		 * @return 此构建器
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置响应实体的正文并返回它.
		 * 
		 * @param <T> 正文的类型
		 * @param body 响应实体的主体
		 * 
		 * @return 构建的响应实体
		 */
		<T> ResponseEntity<T> body(T body);
	}


	private static class DefaultBuilder implements BodyBuilder {

		private final Object statusCode;

		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBuilder(Object statusCode) {
			this.statusCode = statusCode;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(HttpHeaders headers) {
			if (headers != null) {
				this.headers.putAll(headers);
			}
			return this;
		}

		@Override
		public BodyBuilder allow(HttpMethod... allowedMethods) {
			this.headers.setAllow(new LinkedHashSet<HttpMethod>(Arrays.asList(allowedMethods)));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder eTag(String etag) {
			if (etag != null) {
				if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
					etag = "\"" + etag;
				}
				if (!etag.endsWith("\"")) {
					etag = etag + "\"";
				}
			}
			this.headers.setETag(etag);
			return this;
		}

		@Override
		public BodyBuilder lastModified(long date) {
			this.headers.setLastModified(date);
			return this;
		}

		@Override
		public BodyBuilder location(URI location) {
			this.headers.setLocation(location);
			return this;
		}

		@Override
		public BodyBuilder cacheControl(CacheControl cacheControl) {
			String ccValue = cacheControl.getHeaderValue();
			if (ccValue != null) {
				this.headers.setCacheControl(cacheControl.getHeaderValue());
			}
			return this;
		}

		@Override
		public BodyBuilder varyBy(String... requestHeaders) {
			this.headers.setVary(Arrays.asList(requestHeaders));
			return this;
		}

		@Override
		public <T> ResponseEntity<T> build() {
			return body(null);
		}

		@Override
		public <T> ResponseEntity<T> body(T body) {
			return new ResponseEntity<T>(body, this.headers, this.statusCode);
		}
	}

}
