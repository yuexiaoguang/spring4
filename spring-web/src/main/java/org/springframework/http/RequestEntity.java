package org.springframework.http;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link HttpEntity}的扩展, 添加{@linkplain HttpMethod 方法}和{@linkplain URI uri}.
 * 用于{@code RestTemplate}和{@code @Controller}方法.
 *
 * <p>在{@code RestTemplate}中, 此类在
 * {@link org.springframework.web.client.RestTemplate#exchange(RequestEntity, Class) exchange()}中用作参数:
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity.post(new URI(&quot;http://example.com/bar&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 *
 * <p>如果想提供包含变量的URI模板, 考虑使用{@link org.springframework.web.util.UriTemplate}:
 * <pre class="code">
 * URI uri = new UriTemplate(&quot;http://example.com/{foo}&quot;).expand(&quot;bar&quot;);
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity.post(uri).accept(MediaType.APPLICATION_JSON).body(body);
 * </pre>
 *
 * <p>也可以在Spring MVC中使用, 作为@Controller方法中的参数:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public void handle(RequestEntity&lt;String&gt; request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * </pre>
 */
public class RequestEntity<T> extends HttpEntity<T> {

	private final HttpMethod method;

	private final URI url;

	private final Type type;


	public RequestEntity(HttpMethod method, URI url) {
		this(null, null, method, url);
	}

	public RequestEntity(T body, HttpMethod method, URI url) {
		this(body, null, method, url, null);
	}

	/**
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 * @param type 用于泛型类型解析的类型
	 * @since 4.3
	 */
	public RequestEntity(T body, HttpMethod method, URI url, Type type) {
		this(body, null, method, url, type);
	}

	public RequestEntity(MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null);
	}

	public RequestEntity(T body, MultiValueMap<String, String> headers, HttpMethod method, URI url) {
		this(body, headers, method, url, null);
	}

	/**
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @param type 用于泛型类型解析的类型
	 */
	public RequestEntity(T body, MultiValueMap<String, String> headers, HttpMethod method, URI url, Type type) {
		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
	}


	/**
	 * 返回请求的HTTP方法.
	 * 
	 * @return HTTP方法作为{@code HttpMethod}枚举值
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * 返回请求的URL.
	 * 
	 * @return URL
	 */
	public URI getUrl() {
		return this.url;
	}

	/**
	 * 返回请求正文的类型.
	 * 
	 * @return 请求正文的类型, 或{@code null}
	 */
	public Type getType() {
		if (this.type == null) {
			T body = getBody();
			if (body != null) {
				return body.getClass();
			}
		}
		return this.type;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		RequestEntity<?> otherEntity = (RequestEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(getMethod(), otherEntity.getMethod()) &&
				ObjectUtils.nullSafeEquals(getUrl(), otherEntity.getUrl()));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.method);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.url);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(getMethod());
		builder.append(' ');
		builder.append(getUrl());
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
	 * 使用给定的方法和URL创建构建器.
	 * 
	 * @param method HTTP方法 (GET, POST, etc)
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		return new DefaultBodyBuilder(method, url);
	}

	/**
	 * 使用给定的URL创建HTTP GET构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> get(URI url) {
		return method(HttpMethod.GET, url);
	}

	/**
	 * 使用给定的URL创建HTTP HEAD构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> head(URI url) {
		return method(HttpMethod.HEAD, url);
	}

	/**
	 * 使用给定的URL创建HTTP POST构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder post(URI url) {
		return method(HttpMethod.POST, url);
	}

	/**
	 * 使用给定的URL创建HTTP PUT构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder put(URI url) {
		return method(HttpMethod.PUT, url);
	}

	/**
	 * 使用给定的URL创建HTTP PATCH构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static BodyBuilder patch(URI url) {
		return method(HttpMethod.PATCH, url);
	}

	/**
	 * 使用给定的URL创建HTTP DELETE构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> delete(URI url) {
		return method(HttpMethod.DELETE, url);
	}

	/**
	 * 使用给定的URL创建HTTP OPTIONS构建器.
	 * 
	 * @param url the URL
	 * 
	 * @return 创建的构建器
	 */
	public static HeadersBuilder<?> options(URI url) {
		return method(HttpMethod.OPTIONS, url);
	}


	/**
	 * 定义将header添加到请求实体的构建器.
	 * 
	 * @param <B> 构建器子类
	 */
	public interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * 在给定名称下添加给定的单个header值.
		 * 
		 * @param headerName  名称
		 * @param headerValues 值
		 * 
		 * @return 此构建器
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 设置{@code Accept} header指定的可接受{@linkplain MediaType 媒体类型}列表.
		 * 
		 * @param acceptableMediaTypes 可接受的媒体类型
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置{@code Accept-Charset} header指定的可接受{@linkplain Charset 字符集}列表.
		 * 
		 * @param acceptableCharsets 可接受的字符集
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * 设置{@code If-Modified-Since} header的值.
		 * <p>日期应指定为格林威治标准时间1970年1月1日以来的毫秒数.
		 * 
		 * @param ifModifiedSince header的新值
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * 设置{@code If-None-Match} header的值.
		 * 
		 * @param ifNoneMatches header的新值
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * 构建没有正文的请求实体.
		 * 
		 * @return 请求实体
		 */
		RequestEntity<Void> build();
	}


	/**
	 * 定义将主体添加到响应实体的构建器.
	 */
	public interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * 设置正文的长度(以字节为单位), 由{@code Content-Length} header指定.
		 * 
		 * @param contentLength 正文的长度
		 * 
		 * @return 此构建器
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 根据{@code Content-Type} header的指定, 设置正文的{@linkplain MediaType 媒体类型}.
		 * 
		 * @param contentType 内容类型
		 * 
		 * @return 此构建器
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置请求实体的主体并构建RequestEntity.
		 * 
		 * @param <T> 正文的类型
		 * @param body 请求实体的正文
		 * 
		 * @return 构建的请求实体
		 */
		<T> RequestEntity<T> body(T body);

		/**
		 * 设置请求实体的正文和类型, 并构建RequestEntity.
		 * 
		 * @param <T> 正文的类型
		 * @param body 请求实体的正文
		 * @param type 正文的类型, 对泛型类型解析很有用
		 * 
		 * @return 构建的请求实体
		 */
		<T> RequestEntity<T> body(T body, Type type);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final HttpMethod method;

		private final URI url;

		private final HttpHeaders headers = new HttpHeaders();

		public DefaultBodyBuilder(HttpMethod method, URI url) {
			this.method = method;
			this.url = url;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
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
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestEntity<Void> build() {
			return new RequestEntity<Void>(this.headers, this.method, this.url);
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return new RequestEntity<T>(body, this.headers, this.method, this.url);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return new RequestEntity<T>(body, this.headers, this.method, this.url, type);
		}
	}
}
