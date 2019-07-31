package org.springframework.web.multipart;

import org.springframework.core.NestedRuntimeException;

/**
 * multipart解析失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class MultipartException extends NestedRuntimeException {

	public MultipartException(String msg) {
		super(msg);
	}

	public MultipartException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
