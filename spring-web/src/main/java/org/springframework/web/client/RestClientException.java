package org.springframework.web.client;

import org.springframework.core.NestedRuntimeException;

/**
 * 每当遇到客户端HTTP错误时{@link RestTemplate}抛出的异常的基类.
 */
public class RestClientException extends NestedRuntimeException {

	private static final long serialVersionUID = -4084444984163796577L;


	public RestClientException(String msg) {
		super(msg);
	}

	public RestClientException(String msg, Throwable ex) {
		super(msg, ex);
	}
}
