package org.springframework.jms;

/**
 * 镜像JMS InvalidClientIDException的运行时异常.
 */
@SuppressWarnings("serial")
public class InvalidClientIDException extends JmsException {

	public InvalidClientIDException(javax.jms.InvalidClientIDException cause) {
		super(cause);
	}

}
