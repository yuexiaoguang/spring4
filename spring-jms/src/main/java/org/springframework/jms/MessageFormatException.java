package org.springframework.jms;

/**
 * 镜像JMS MessageFormatException的运行时异常.
 */
@SuppressWarnings("serial")
public class MessageFormatException extends JmsException {

	public MessageFormatException(javax.jms.MessageFormatException cause) {
		super(cause);
	}

}
