package org.springframework.jms.listener.adapter;

import org.springframework.jms.JmsException;

/**
 * 无法发送消息回复时抛出异常.
 */
@SuppressWarnings("serial")
public class ReplyFailureException extends JmsException {

	public ReplyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
