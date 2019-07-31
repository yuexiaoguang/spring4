package org.springframework.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * 基于{@link HttpStatus}的异常的抽象基类.
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 5696801857651587810L;


	private final HttpStatus statusCode;


	/**
	 * @param statusCode 状态码
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		this(statusCode, statusCode.name(), null, null, null);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		this(statusCode, statusText, null, null, null);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseBody 响应主体内容, may be {@code null}
	 * @param responseCharset 响应主体字符集, may be {@code null}
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			byte[] responseBody, Charset responseCharset) {

		this(statusCode, statusText, null, responseBody, responseCharset);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseHeaders 响应 header, may be {@code null}
	 * @param responseBody 响应主体内容, may be {@code null}
	 * @param responseCharset 响应主体字符集, may be {@code null}
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {

		super(statusCode.value() + " " + statusText, statusCode.value(), statusText,
				responseHeaders, responseBody, responseCharset);
		this.statusCode = statusCode;
	}


	/**
	 * 返回HTTP状态码.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

}
