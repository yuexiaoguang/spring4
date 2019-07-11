package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 实现JTA {@link javax.transaction.Synchronization}接口的适配器, 委托给底层Spring
 * {@link org.springframework.transaction.support.TransactionSynchronization}.
 *
 * <p>尽管为Spring事务同步构建了原始代码, 但对于将Spring资源管理代码与纯JTA/EJB CMT事务同步非常有用.
 */
public class SpringJtaSynchronizationAdapter implements Synchronization {

	protected static final Log logger = LogFactory.getLog(SpringJtaSynchronizationAdapter.class);

	private final TransactionSynchronization springSynchronization;

	private UserTransaction jtaTransaction;

	private boolean beforeCompletionCalled = false;


	/**
	 * @param springSynchronization 要委托给的Spring TransactionSynchronization
	 */
	public SpringJtaSynchronizationAdapter(TransactionSynchronization springSynchronization) {
		Assert.notNull(springSynchronization, "TransactionSynchronization must not be null");
		this.springSynchronization = springSynchronization;
	}

	/**
	 * <p>请注意, 此适配器永远不会在WebLogic上执行只回滚调用,
	 * 因为已知WebLogic Server会在{@code beforeCompletion}异常的情况下自动将事务标记为仅回滚.
	 * 因此, 在WLS上, 此构造函数等效于单arg构造函数.
	 * 
	 * @param springSynchronization 要委托给的Spring TransactionSynchronization
	 * @param jtaUserTransaction 在{@code beforeCompletion}中抛出异常的情况下, JTA UserTransaction用于仅回滚设置
	 * (如果JTA提供者本身在这种情况下标记事务仅回滚, 则可以省略, 这是JTA规范从JTA 1.1开始所要求的).
	 */
	public SpringJtaSynchronizationAdapter(
			TransactionSynchronization springSynchronization, UserTransaction jtaUserTransaction) {

		this(springSynchronization);
		if (jtaUserTransaction != null && !jtaUserTransaction.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = jtaUserTransaction;
		}
	}

	/**
	 * <p>请注意, 此适配器永远不会在WebLogic上执行仅回滚调用,
	 * 因为已知WebLogic Server会在{@code beforeCompletion}异常的情况下自动将事务标记为仅回滚.
	 * 因此, 在WLS上, 此构造函数等效于单arg构造函数.
	 * 
	 * @param springSynchronization 要委托给的Spring TransactionSynchronization
	 * @param jtaTransactionManager JTA TransactionManager, 用于在{@code beforeCompletion}中抛出异常的情况下用于仅回滚设置
	 * (如果JTA提供者本身在这种情况下标记事务仅回滚, 则可以省略, 这是JTA规范从JTA 1.1开始所要求的)
	 */
	public SpringJtaSynchronizationAdapter(
			TransactionSynchronization springSynchronization, TransactionManager jtaTransactionManager) {

		this(springSynchronization);
		if (jtaTransactionManager != null && !jtaTransactionManager.getClass().getName().startsWith("weblogic.")) {
			this.jtaTransaction = new UserTransactionAdapter(jtaTransactionManager);
		}
	}


	/**
	 * JTA {@code beforeCompletion}回调: 在提交之前调用.
	 * <p>如果发生异常, JTA事务将标记为仅回滚.
	 */
	@Override
	public void beforeCompletion() {
		try {
			boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
			this.springSynchronization.beforeCommit(readOnly);
		}
		catch (RuntimeException ex) {
			setRollbackOnlyIfPossible();
			throw ex;
		}
		catch (Error err) {
			setRollbackOnlyIfPossible();
			throw err;
		}
		finally {
			// 尽早处理Spring的beforeCompletion, 以避免在事务完成后执行JDBC操作时发出警告的严格JTA实现的问题 (e.g. Connection.getWarnings).
			this.beforeCompletionCalled = true;
			this.springSynchronization.beforeCompletion();
		}
	}

	/**
	 * 将底层JTA事务设置为仅回滚.
	 */
	private void setRollbackOnlyIfPossible() {
		if (this.jtaTransaction != null) {
			try {
				this.jtaTransaction.setRollbackOnly();
			}
			catch (UnsupportedOperationException ex) {
				// 可能是Hibernate的WebSphereExtendedJTATransactionLookup伪JTA内容...
				logger.debug("JTA transaction handle does not support setRollbackOnly method - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion", ex);
			}
			catch (Throwable ex) {
				logger.error("Could not set JTA transaction rollback-only", ex);
			}
		}
		else {
			logger.debug("No JTA transaction handle available and/or running on WebLogic - " +
						"relying on JTA provider to mark the transaction as rollback-only based on " +
						"the exception thrown from beforeCompletion");
			}
	}

	/**
	 * JTA {@code afterCompletion}回调: 在提交/回滚后调用.
	 * <p>在回滚的情况下, 需要在此后期阶段调用Spring同步的{@code beforeCompletion}, 因为JTA没有相应的回调.
	 */
	@Override
	public void afterCompletion(int status) {
		if (!this.beforeCompletionCalled) {
			// beforeCompletion 之前没有调用过 (可能是因为JTA回滚).
			// 在这里执行清理.
			this.springSynchronization.beforeCompletion();
		}
		// 使用适当的状态指示调用afterCompletion.
		switch (status) {
			case Status.STATUS_COMMITTED:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
				break;
			case Status.STATUS_ROLLEDBACK:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				this.springSynchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		}
	}

}
