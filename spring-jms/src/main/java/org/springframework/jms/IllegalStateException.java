package org.springframework.jms;

/**
 * 镜像JMS IllegalStateException的运行时异常.
 */
@SuppressWarnings("serial")
public class IllegalStateException extends JmsException {

	public IllegalStateException(javax.jms.IllegalStateException cause) {
		super(cause);
	}

}
