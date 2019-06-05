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
 * Hibernate 5的JtaPlatform SPI的实现, 公开传入的{@link TransactionManager}, {@link UserTransaction}
 * 和{@link TransactionSynchronizationRegistry}引用.
 */
@SuppressWarnings("serial")
class ConfigurableJtaPlatform implements JtaPlatform {

	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;

	private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * @param tm JTA TransactionManager引用 (必须)
	 * @param ut JTA UserTransaction引用 (可选)
	 * @param tsr JTA 1.1 TransactionSynchronizationRegistry (可选)
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
