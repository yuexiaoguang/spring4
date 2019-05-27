package org.springframework.jms;

/**
 * 镜像JMS MessageNotReadableException的运行时异常.
 */
@SuppressWarnings("serial")
public class MessageNotReadableException extends JmsException {

	public MessageNotReadableException(javax.jms.MessageNotReadableException cause) {
		super(cause);
	}

}
