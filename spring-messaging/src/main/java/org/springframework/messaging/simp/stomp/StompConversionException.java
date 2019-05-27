package org.springframework.messaging.simp.stomp;

import org.springframework.core.NestedRuntimeException;

/**
 * 在编码或解码STOMP消息失败后引发.
 */
@SuppressWarnings("serial")
public class StompConversionException extends NestedRuntimeException {


	public StompConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public StompConversionException(String msg) {
		super(msg);
	}

}
