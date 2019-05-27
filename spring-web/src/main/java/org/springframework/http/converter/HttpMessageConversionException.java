package org.springframework.http.converter;

import org.springframework.core.NestedRuntimeException;

/**
 * Thrown by {@link HttpMessageConverter} implementations when a conversion attempt fails.
 */
@SuppressWarnings("serial")
public class HttpMessageConversionException extends NestedRuntimeException {

	/**
	 * Create a new HttpMessageConversionException.
	 * @param msg the detail message
	 */
	public HttpMessageConversionException(String msg) {
		super(msg);
	}

	/**
	 * Create a new HttpMessageConversionException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public HttpMessageConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
