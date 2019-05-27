package org.springframework.http.converter;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#read} method fails.
 */
@SuppressWarnings("serial")
public class HttpMessageNotReadableException extends HttpMessageConversionException {

	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 */
	public HttpMessageNotReadableException(String msg) {
		super(msg);
	}

	/**
	 * Create a new HttpMessageNotReadableException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public HttpMessageNotReadableException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
