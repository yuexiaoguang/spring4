package org.springframework.transaction.support;

import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;

/**
 * {@link org.springframework.transaction.TransactionStatus}接口的默认实现,
 * 由{@link AbstractPlatformTransactionManager}使用.
 * 基于底层"事务对象"的概念.
 *
 * <p>保存{@link AbstractPlatformTransactionManager}内部需要的所有状态信息,
 * 包括由具体事务管理器实现确定的通用事务对象.
 *
 * <p>支持将与保存点相关的方法委托给实现{@link SavepointManager}接口的事务对象.
 *
 * <p><b>NOTE:</b> <i>不打算</i>与其他PlatformTransactionManager实现一起使用, 特别是不适用于测试环境中的模拟事务管理器.
 * 使用替代的{@link SimpleTransactionStatus}类或用于普通{@link org.springframework.transaction.TransactionStatus}接口的模拟.
 */
public class DefaultTransactionStatus extends AbstractTransactionStatus {

	private final Object transaction;

	private final boolean newTransaction;

	private final boolean newSynchronization;

	private final boolean readOnly;

	private final boolean debug;

	private final Object suspendedResources;


	/**
	 * @param transaction 可以为内部事务实现保存状态的底层事务对象
	 * @param newTransaction 如果事务是新的, 否则使用现有事务
	 * @param newSynchronization 如果已为给定事务打开新的事务同步
	 * @param readOnly 事务是否为只读
	 * @param debug 应该启用debug日志记录来处理此事务?
	 * 在此处缓存它可以防止重复调用询问日志系统是否应启用调试日志.
	 * @param suspendedResources 持有此事务暂停资源的保存器
	 */
	public DefaultTransactionStatus(
			Object transaction, boolean newTransaction, boolean newSynchronization,
			boolean readOnly, boolean debug, Object suspendedResources) {

		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.newSynchronization = newSynchronization;
		this.readOnly = readOnly;
		this.debug = debug;
		this.suspendedResources = suspendedResources;
	}


	/**
	 * 返回底层事务对象.
	 */
	public Object getTransaction() {
		return this.transaction;
	}

	/**
	 * 返回是否有实际的事务处于活动状态.
	 */
	public boolean hasTransaction() {
		return (this.transaction != null);
	}

	@Override
	public boolean isNewTransaction() {
		return (hasTransaction() && this.newTransaction);
	}

	/**
	 * 返回是否已为此事务打开新的事务同步.
	 */
	public boolean isNewSynchronization() {
		return this.newSynchronization;
	}

	/**
	 * 返回此事务是否被定义为只读事务.
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 返回是否调试此事务的进度.
	 * 这被AbstractPlatformTransactionManager用作优化, 以防止重复调用 logger.isDebug().
	 * 不是真正用于客户端代码.
	 */
	public boolean isDebug() {
		return this.debug;
	}

	/**
	 * 返回持有此事务暂停资源的保存器.
	 */
	public Object getSuspendedResources() {
		return this.suspendedResources;
	}


	//---------------------------------------------------------------------
	// Enable functionality through underlying transaction object
	//---------------------------------------------------------------------

	/**
	 * 通过检查事务对象来确定仅回滚标志, 前提是后者实现了{@link SmartTransactionObject}接口.
	 * <p>如果事务本身已被事务协调器标记为仅回滚, 则返回"true", 例如在超时的情况下.
	 */
	@Override
	public boolean isGlobalRollbackOnly() {
		return ((this.transaction instanceof SmartTransactionObject) &&
				((SmartTransactionObject) this.transaction).isRollbackOnly());
	}

	/**
	 * 将刷新委托给事务对象, 前提是后者实现了{@link SmartTransactionObject}接口.
	 */
	@Override
	public void flush() {
		if (this.transaction instanceof SmartTransactionObject) {
			((SmartTransactionObject) this.transaction).flush();
		}
	}

	/**
	 * 此实现公开了底层事务对象的SavepointManager接口.
	 */
	@Override
	protected SavepointManager getSavepointManager() {
		if (!isTransactionSavepointManager()) {
			throw new NestedTransactionNotSupportedException(
				"Transaction object [" + getTransaction() + "] does not support savepoints");
		}
		return (SavepointManager) getTransaction();
	}

	/**
	 * 返回底层事务是否实现了SavepointManager接口.
	 */
	public boolean isTransactionSavepointManager() {
		return (getTransaction() instanceof SavepointManager);
	}

}
