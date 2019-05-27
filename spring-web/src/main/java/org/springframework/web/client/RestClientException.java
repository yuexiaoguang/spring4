package org.springframework.web.client;

import org.springframework.core.NestedRuntimeException;

/**
 * Base class for exceptions thrown by {@link RestTemplate} whenever it encounters
 * client-side HTTP errors.
 */
public class RestClientException extends NestedRuntimeException {

	private static final long serialVersionUID = -4084444984163796577L;


	/**
	 * Construct a new instance of {@code HttpClientException} with the given message.
	 * @param msg the message
	 */
	public RestClientException(String msg) {
		super(msg);
	}

	/**
	 * Construct a new instance of {@code HttpClientException} with the given message and
	 * exception.
	 * @param msg the message
	 * @param ex the exception
	 */
	public RestClientException(String msg, Throwable ex) {
		super(msg, ex);
	}

}
