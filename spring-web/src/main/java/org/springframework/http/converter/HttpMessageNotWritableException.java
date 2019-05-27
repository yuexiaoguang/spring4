package org.springframework.http.converter;

/**
 * Thrown by {@link HttpMessageConverter} implementations when the
 * {@link HttpMessageConverter#write} method fails.
 */
@SuppressWarnings("serial")
public class HttpMessageNotWritableException extends HttpMessageConversionException {

	/**
	 * Create a new HttpMessageNotWritableException.
	 * @param msg the detail message
	 */
	public HttpMessageNotWritableException(String msg) {
		super(msg);
	}

	/**
	 * Create a new HttpMessageNotWritableException.
	 * @param msg the detail message
	 * @param cause the root cause (if any)
	 */
	public HttpMessageNotWritableException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
