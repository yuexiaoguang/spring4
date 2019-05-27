package org.springframework.jms;

/**
 * 镜像JMS JMSSecurityException的运行时异常.
 */
@SuppressWarnings("serial")
public class JmsSecurityException extends JmsException {

	public JmsSecurityException(javax.jms.JMSSecurityException cause) {
		super(cause);
	}

}
