package org.springframework.orm.hibernate5;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate 5's JtaPlatform SPI, exposing passed-in {@link TransactionManager},
 * {@link UserTransaction} and {@link TransactionSynchronizationRegistry} references.
 */
@SuppressWarnings("serial")
class ConfigurableJtaPlatform implements JtaPlatform {

	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;

	private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * Create a new ConfigurableJtaPlatform instance with the given
	 * JTA TransactionManager and optionally a given UserTransaction.
	 * @param tm the JTA TransactionManager reference (required)
	 * @param ut the JTA UserTransaction reference (optional)
	 * @param tsr the JTA 1.1 TransactionSynchronizationRegistry (optional)
	 */
	public ConfigurableJtaPlatform(TransactionManager tm, UserTransaction ut, TransactionSynchronizationRegistry tsr) {
		Assert.notNull(tm, "TransactionManager reference must not be null");
		this.transactionManager = tm;
		this.userTransaction = (ut != null ? ut : new UserTransactionAdapter(tm));
		this.transactionSynchronizationRegistry = tsr;
	}


	@Override
	public TransactionManager retrieveTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		return this.userTransaction;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

	@Override
	public boolean canRegisterSynchronization() {
		try {
			return (this.transactionManager.getStatus() == Status.STATUS_ACTIVE);
		}
		catch (SystemException ex) {
			throw new TransactionException("Could not determine JTA transaction status", ex);
		}
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if (this.transactionSynchronizationRegistry != null) {
			this.transactionSynchronizationRegistry.registerInterposedSynchronization(synchronization);
		}
		else {
			try {
				this.transactionManager.getTransaction().registerSynchronization(synchronization);
			}
			catch (Exception ex) {
				throw new TransactionException("Could not access JTA Transaction to register synchronization", ex);
			}
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

}
