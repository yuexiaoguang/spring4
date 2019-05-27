package org.springframework.jms.support.converter;

import org.springframework.jms.JmsException;

/**
 * 当对象和{@link javax.jms.Message}相互转换失败时, {@link MessageConverter}实现引发的异常.
 */
@SuppressWarnings("serial")
public class MessageConversionException extends JmsException {

	public MessageConversionException(String msg) {
		super(msg);
	}

	public MessageConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
