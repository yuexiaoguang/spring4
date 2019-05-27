package org.springframework.jms;

/**
 * 镜像JMS TransactionRolledBackException的运行时异常.
 */
@SuppressWarnings("serial")
public class TransactionRolledBackException extends JmsException {

	public TransactionRolledBackException(javax.jms.TransactionRolledBackException cause) {
		super(cause);
	}

}
