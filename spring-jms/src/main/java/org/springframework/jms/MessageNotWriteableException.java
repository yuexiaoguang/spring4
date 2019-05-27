package org.springframework.jms;

/**
 * 镜像JMS MessageNotWriteableException的运行时异常.
 */
@SuppressWarnings("serial")
public class MessageNotWriteableException extends JmsException {

	public MessageNotWriteableException(javax.jms.MessageNotWriteableException cause) {
		super(cause);
	}

}
