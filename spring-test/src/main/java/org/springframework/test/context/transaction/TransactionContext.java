package org.springframework.test.context.transaction;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * Transaction context for a specific {@link TestContext}.
 */
class TransactionContext {

	private static final Log logger = LogFactory.getLog(TransactionContext.class);

	private final TestContext testContext;

	private final TransactionDefinition transactionDefinition;

	private final PlatformTransactionManager transactionManager;

	private final boolean defaultRollback;

	private boolean flaggedForRollback;

	private TransactionStatus transactionStatus;

	private final AtomicInteger transactionsStarted = new AtomicInteger(0);


	TransactionContext(TestContext testContext, PlatformTransactionManager transactionManager,
			TransactionDefinition transactionDefinition, boolean defaultRollback) {

		this.testContext = testContext;
		this.transactionManager = transactionManager;
		this.transactionDefinition = transactionDefinition;
		this.defaultRollback = defaultRollback;
		this.flaggedForRollback = defaultRollback;
	}


	TransactionStatus getTransactionStatus() {
		return this.transactionStatus;
	}

	/**
	 * Has the current transaction been flagged for rollback?
	 * <p>In other words, should we roll back or commit the current transaction
	 * upon completion of the current test?
	 */
	boolean isFlaggedForRollback() {
		return this.flaggedForRollback;
	}

	void setFlaggedForRollback(boolean flaggedForRollback) {
		if (this.transactionStatus == null) {
			throw new IllegalStateException(
					"Failed to set rollback flag - transaction does not exist: " + this.testContext);
		}
		this.flaggedForRollback = flaggedForRollback;
	}

	/**
	 * Start a new transaction for the configured test context.
	 * <p>Only call this method if {@link #endTransaction} has been called or if no
	 * transaction has been previously started.
	 * @throws TransactionException if starting the transaction fails
	 */
	void startTransaction() {
		Assert.state(this.transactionStatus == null,
				"Cannot start a new transaction without ending the existing transaction first");

		this.flaggedForRollback = this.defaultRollback;
		this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		int transactionsStarted = this.transactionsStarted.incrementAndGet();

		if (logger.isInfoEnabled()) {
			logger.info(String.format(
					"Began transaction (%s) for test context %s; transaction manager [%s]; rollback [%s]",
					transactionsStarted, this.testContext, this.transactionManager, flaggedForRollback));
		}
	}

	/**
	 * Immediately force a <em>commit</em> or <em>rollback</em> of the transaction for the
	 * configured test context, according to the {@linkplain #isFlaggedForRollback rollback flag}.
	 */
	void endTransaction() {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format(
					"Ending transaction for test context %s; transaction status [%s]; rollback [%s]",
					this.testContext, this.transactionStatus, this.flaggedForRollback));
		}
		if (this.transactionStatus == null) {
			throw new IllegalStateException(
					"Failed to end transaction - transaction does not exist: " + this.testContext);
		}

		try {
			if (this.flaggedForRollback) {
				this.transactionManager.rollback(this.transactionStatus);
			}
			else {
				this.transactionManager.commit(this.transactionStatus);
			}
		}
		finally {
			this.transactionStatus = null;
		}

		if (logger.isInfoEnabled()) {
			logger.info((this.flaggedForRollback ? "Rolled back" : "Committed") +
					" transaction for test: " + this.testContext);
		}
	}

}