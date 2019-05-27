package org.springframework.core.convert;

import org.springframework.core.NestedRuntimeException;

/**
 * 转换系统抛出的异常的基类.
 */
@SuppressWarnings("serial")
public abstract class ConversionException extends NestedRuntimeException {

	public ConversionException(String message) {
		super(message);
	}

	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}
