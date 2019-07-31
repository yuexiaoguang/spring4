package org.springframework.http;

import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * 表示HTTP请求或响应实体, 由header和正文组成.
 *
 * <p>通常与{@link org.springframework.web.client.RestTemplate}结合使用, 如此:
 * <pre class="code">
 * HttpHeaders headers = new HttpHeaders();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;String&gt;(helloWorld, headers);
 * URI location = template.postForLocation("http://example.com", entity);
 * </pre>
 * or
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * 也可以在Spring MVC中使用, 作为@Controller方法的返回值:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;String&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 */
public class HttpEntity<T> {

	/**
	 * 空{@code HttpEntity}, 没有正文或header.
	 */
	public static final HttpEntity<?> EMPTY = new HttpEntity<Object>();


	private final HttpHeaders headers;

	private final T body;


	protected HttpEntity() {
		this(null, null);
	}

	/**
	 * 使用给定的正文, 没有header.
	 * 
	 * @param body 实体正文
	 */
	public HttpEntity(T body) {
		this(body, null);
	}

	/**
	 * 使用给定的header, 没有正文.
	 * 
	 * @param headers 实体header
	 */
	public HttpEntity(MultiValueMap<String, String> headers) {
		this(null, headers);
	}

	/**
	 * @param body 实体正文
	 * @param headers 实体header
	 */
	public HttpEntity(T body, MultiValueMap<String, String> headers) {
		this.body = body;
		HttpHeaders tempHeaders = new HttpHeaders();
		if (headers != null) {
			tempHeaders.putAll(headers);
		}
		this.headers = HttpHeaders.readOnlyHttpHeaders(tempHeaders);
	}


	/**
	 * 返回此实体的header.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 返回此实体的正文.
	 */
	public T getBody() {
		return this.body;
	}

	/**
	 * 指示此实体是否具有正文.
	 */
	public boolean hasBody() {
		return (this.body != null);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		HttpEntity<?> otherEntity = (HttpEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.headers, otherEntity.headers) &&
				ObjectUtils.nullSafeEquals(this.body, otherEntity.body));
	}

	@Override
	public int hashCode() {
		return (ObjectUtils.nullSafeHashCode(this.headers) * 29 + ObjectUtils.nullSafeHashCode(this.body));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		if (this.body != null) {
			builder.append(this.body);
			if (this.headers != null) {
				builder.append(',');
			}
		}
		if (this.headers != null) {
			builder.append(this.headers);
		}
		builder.append('>');
		return builder.toString();
	}
}
