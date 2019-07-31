package org.springframework.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * 收到HTTP 4xx时抛出的异常.
 */
public class HttpClientErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = 5177019431887513952L;


	/**
	 * @param statusCode 状态码
	 */
	public HttpClientErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseBody 响应主体内容 (may be {@code null})
	 * @param responseCharset 响应主体字符集 (may be {@code null})
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
			byte[] responseBody, Charset responseCharset) {

		super(statusCode, statusText, responseBody, responseCharset);
	}

	/**
	 * @param statusCode 状态码
	 * @param statusText 状态文本
	 * @param responseHeaders 响应header (may be {@code null})
	 * @param responseBody 响应主体内容 (may be {@code null})
	 * @param responseCharset 响应主体字符集 (may be {@code null})
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
			HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {

		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

}
