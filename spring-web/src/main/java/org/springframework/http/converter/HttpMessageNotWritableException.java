package org.springframework.http.converter;

/**
 * 当{@link HttpMessageConverter#write}方法失败时, 由{@link HttpMessageConverter}实现抛出.
 */
@SuppressWarnings("serial")
public class HttpMessageNotWritableException extends HttpMessageConversionException {

	public HttpMessageNotWritableException(String msg) {
		super(msg);
	}

	public HttpMessageNotWritableException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
