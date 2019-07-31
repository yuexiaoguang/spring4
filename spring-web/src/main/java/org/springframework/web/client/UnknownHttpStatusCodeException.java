package org.springframework.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * 收到未知(或自定义) HTTP状态码时抛出的异常.
 */
public class UnknownHttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 7103980251635005491L;


	/**
	 * @param rawStatusCode 原始状态码值
	 * @param statusText 状态文本
	 * @param responseHeaders 响应headers (may be {@code null})
	 * @param responseBody 响应主体内容 (may be {@code null})
	 * @param responseCharset 响应主体字符集 (may be {@code null})
	 */
	public UnknownHttpStatusCodeException(int rawStatusCode, String statusText,
			HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {

		super("Unknown status code [" + rawStatusCode + "]" + " " + statusText,
				rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

}
