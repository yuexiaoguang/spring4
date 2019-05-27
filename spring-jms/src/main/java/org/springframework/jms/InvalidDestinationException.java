package org.springframework.jms;

/**
 * 镜像JMS InvalidDestinationException的运行时异常.
 */
@SuppressWarnings("serial")
public class InvalidDestinationException extends JmsException {

	public InvalidDestinationException(javax.jms.InvalidDestinationException cause) {
		super(cause);
	}

}
