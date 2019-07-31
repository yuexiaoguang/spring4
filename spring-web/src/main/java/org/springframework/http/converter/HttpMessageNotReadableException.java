package org.springframework.http.converter;

/**
 * 当{@link HttpMessageConverter#read}方法失败时, 由{@link HttpMessageConverter}实现抛出.
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

	public HttpMessageNotReadableException(String msg) {
		super(msg);
	}

	public HttpMessageNotReadableException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
