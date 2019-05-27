package org.springframework.jms;

/**
 * 镜像JMS InvalidSelectorException的运行时异常.
 */
@SuppressWarnings("serial")
public class InvalidSelectorException extends JmsException {

	public InvalidSelectorException(javax.jms.InvalidSelectorException cause) {
		super(cause);
	}

}
