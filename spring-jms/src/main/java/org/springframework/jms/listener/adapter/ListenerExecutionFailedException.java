package org.springframework.jms.listener.adapter;

import org.springframework.jms.JmsException;

/**
 * 执行监听器方法失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class ListenerExecutionFailedException extends JmsException {

	public ListenerExecutionFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
