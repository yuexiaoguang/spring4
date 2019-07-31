package org.springframework.http.converter;

import org.springframework.core.NestedRuntimeException;

/**
 * 当转换尝试失败时, 由{@link HttpMessageConverter}实现抛出.
 */
@SuppressWarnings("serial")
public class HttpMessageConversionException extends NestedRuntimeException {

	public HttpMessageConversionException(String msg) {
		super(msg);
	}

	public HttpMessageConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
