package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * JTA transaction object, representing a {@link javax.transaction.UserTransaction}.
 * Used as transaction object by Spring's {@link JtaTransactionManager}.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 */
public class JtaTransactionObject implements SmartTransactionObject {

	private final UserTransaction userTransaction;

	boolean resetTransactionTimeout = false;


	/**
	 * Create a new JtaTransactionObject for the given JTA UserTransaction.
	 * @param userTransaction the JTA UserTransaction for the current transaction
	 * (either a shared object or retrieved through a fresh per-transaction lookuip)
	 */
	public JtaTransactionObject(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * Return the JTA UserTransaction object for the current transaction.
	 */
	public final UserTransaction getUserTransaction() {
		return this.userTransaction;
	}


	/**
	 * This implementation checks the UserTransaction's rollback-only flag.
	 */
	@Override
	public boolean isRollbackOnly() {
		if (this.userTransaction == null) {
			return false;
		}
		try {
			int jtaStatus = this.userTransaction.getStatus();
			return (jtaStatus == Status.STATUS_MARKED_ROLLBACK || jtaStatus == Status.STATUS_ROLLEDBACK);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * This implementation triggers flush callbacks,
	 * assuming that they will flush all affected ORM sessions.
	 * @see org.springframework.transaction.support.TransactionSynchronization#flush()
	 */
	@Override
	public void flush() {
		TransactionSynchronizationUtils.triggerFlush();
	}

}
