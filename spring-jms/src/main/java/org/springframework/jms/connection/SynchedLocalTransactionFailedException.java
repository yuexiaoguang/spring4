package org.springframework.jms.connection;

import javax.jms.JMSException;

import org.springframework.jms.JmsException;

/**
 * 同步本地事务未能完成时抛出异常 (在主事务已完成之后).
 */
@SuppressWarnings("serial")
public class SynchedLocalTransactionFailedException extends JmsException {

	public SynchedLocalTransactionFailedException(String msg, JMSException cause) {
		super(msg, cause);
	}

}
