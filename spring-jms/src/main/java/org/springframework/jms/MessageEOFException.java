package org.springframework.jms;

/**
 * 镜像JMS MessageEOFException的运行时异常.
 */
@SuppressWarnings("serial")
public class MessageEOFException extends JmsException {

	public MessageEOFException(javax.jms.MessageEOFException cause) {
		super(cause);
	}

}
