package org.springframework.http.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 基于{@link HttpServletResponse}的{@link ServerHttpResponse}实现.
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	/** 检查Servlet 3.0+ HttpServletResponse.getHeader(String) */
	private static final boolean servlet3Present =
			ClassUtils.hasMethod(HttpServletResponse.class, "getHeader", String.class);


	private final HttpServletResponse servletResponse;

	private final HttpHeaders headers;

	private boolean headersWritten = false;

	private boolean bodyUsed = false;


	/**
	 * @param servletResponse servlet响应
	 */
	public ServletServerHttpResponse(HttpServletResponse servletResponse) {
		Assert.notNull(servletResponse, "HttpServletResponse must not be null");
		this.servletResponse = servletResponse;
		this.headers = (servlet3Present ? new ServletResponseHttpHeaders() : new HttpHeaders());
	}


	/**
	 * 返回此对象所基于的{@code HttpServletResponse}.
	 */
	public HttpServletResponse getServletResponse() {
		return this.servletResponse;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status, "HttpStatus must not be null");
		this.servletResponse.setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public OutputStream getBody() throws IOException {
		this.bodyUsed = true;
		writeHeaders();
		return this.servletResponse.getOutputStream();
	}

	@Override
	public void flush() throws IOException {
		writeHeaders();
		if (this.bodyUsed) {
			this.servletResponse.flushBuffer();
		}
	}

	@Override
	public void close() {
		writeHeaders();
	}

	private void writeHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					this.servletResponse.addHeader(headerName, headerValue);
				}
			}
			// HttpServletResponse公开了一些header作为属性: 应该包括那些尚未存在的header
			if (this.servletResponse.getContentType() == null && this.headers.getContentType() != null) {
				this.servletResponse.setContentType(this.headers.getContentType().toString());
			}
			if (this.servletResponse.getCharacterEncoding() == null && this.headers.getContentType() != null &&
					this.headers.getContentType().getCharset() != null) {
				this.servletResponse.setCharacterEncoding(this.headers.getContentType().getCharset().name());
			}
			this.headersWritten = true;
		}
	}


	/**
	 * 扩展HttpHeaders, 能够查找底层HttpServletResponse中已存在的header.
	 *
	 * <p>目的仅仅是公开通过HttpServletResponse可用的内容, i.e. 通过名称查找特定header值的功能.
	 * 所有其他与map相关的操作 (e.g. 迭代, 删除等) 仅适用于通过HttpHeaders方法直接添加的值.
	 */
	private class ServletResponseHttpHeaders extends HttpHeaders {

		private static final long serialVersionUID = 3410708522401046302L;

		@Override
		public boolean containsKey(Object key) {
			return (super.containsKey(key) || (get(key) != null));
		}

		@Override
		public String getFirst(String headerName) {
			String value = servletResponse.getHeader(headerName);
			if (value != null) {
				return value;
			}
			else {
				return super.getFirst(headerName);
			}
		}

		@Override
		public List<String> get(Object key) {
			Assert.isInstanceOf(String.class, key, "Key must be a String-based header name");

			Collection<String> values1 = servletResponse.getHeaders((String) key);
			boolean isEmpty1 = CollectionUtils.isEmpty(values1);

			List<String> values2 = super.get(key);
			boolean isEmpty2 = CollectionUtils.isEmpty(values2);

			if (isEmpty1 && isEmpty2) {
				return null;
			}

			List<String> values = new ArrayList<String>();
			if (!isEmpty1) {
				values.addAll(values1);
			}
			if (!isEmpty2) {
				values.addAll(values2);
			}
			return values;
		}
	}

}
