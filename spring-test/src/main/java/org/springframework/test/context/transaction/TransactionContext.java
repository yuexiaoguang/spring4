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
 * 特定{@link TestContext}的事务上下文.
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
	 * 当前事务是否已标记为回滚?
	 * <p>换句话说, 应该在完成当前测试后回滚或提交当前事务?
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
	 * 为配置的测试上下文启动新事务.
	 * <p>如果已调用{@link #endTransaction}或之前未启动任何事务, 则仅调用此方法.
	 * 
	 * @throws TransactionException 如果启动事务失败
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
	 * 根据{@linkplain #isFlaggedForRollback 回滚标志},
	 * 立即为配置的测试上下文强制执行事务的<em>提交</em>或<em>回滚</em>.
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