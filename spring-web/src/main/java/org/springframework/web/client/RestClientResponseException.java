package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;

/**
 * 包含实际HTTP响应数据的异常的公共基类.
 */
public class RestClientResponseException extends RestClientException {

	private static final long serialVersionUID = -8803556342728481792L;

	private static final String DEFAULT_CHARSET = "ISO-8859-1";


	private final int rawStatusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final HttpHeaders responseHeaders;

	private final String responseCharset;


	/**
	 * @param statusCode 原始状态码值
	 * @param statusText 状态文本
	 * @param responseHeaders 响应header (may be {@code null})
	 * @param responseBody 响应主体内容 (may be {@code null})
	 * @param responseCharset 响应主体字符集 (may be {@code null})
	 */
	public RestClientResponseException(String message, int statusCode, String statusText,
			HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {

		super(message);
		this.rawStatusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (responseCharset != null ? responseCharset.name() : DEFAULT_CHARSET);
	}


	/**
	 * 返回原始HTTP状态码值.
	 */
	public int getRawStatusCode() {
		return this.rawStatusCode;
	}

	/**
	 * 返回HTTP状态文本.
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * 返回HTTP响应 header.
	 */
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * 返回响应主体.
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * 返回响应主体.
	 */
	public String getResponseBodyAsString() {
		try {
			return new String(this.responseBody, this.responseCharset);
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new IllegalStateException(ex);
		}
	}

}
