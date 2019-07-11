package org.springframework.transaction;

import org.springframework.core.NestedRuntimeException;

/**
 * 所有事务异常的超类.
 */
@SuppressWarnings("serial")
public abstract class TransactionException extends NestedRuntimeException {

	public TransactionException(String msg) {
		super(msg);
	}

	public TransactionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
