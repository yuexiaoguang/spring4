package org.springframework.web.client;

import java.io.IOException;

/**
 * 发生I/O错误时抛出的异常.
 */
public class ResourceAccessException extends RestClientException {

	private static final long serialVersionUID = -8513182514355844870L;


	public ResourceAccessException(String msg) {
		super(msg);
	}

	public ResourceAccessException(String msg, IOException ex) {
		super(msg, ex);
	}

}
