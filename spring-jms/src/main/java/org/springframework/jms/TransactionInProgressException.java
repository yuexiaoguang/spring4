package org.springframework.jms;

/**
 * 镜像JMS TransactionInProgressException的运行时异常.
 */
@SuppressWarnings("serial")
public class TransactionInProgressException extends JmsException {

	public TransactionInProgressException(javax.jms.TransactionInProgressException cause) {
		super(cause);
	}

}
