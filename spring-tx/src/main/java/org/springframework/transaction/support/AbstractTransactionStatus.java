package org.springframework.transaction.support;

import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionUsageException;

/**
 * {@link org.springframework.transaction.TransactionStatus}接口的抽象基础实现.
 *
 * <p>预先实现本地回滚和已完成标志的处理, 并委托给底层{@link org.springframework.transaction.SavepointManager}.
 * 还提供在事务中保存保存点的选项.
 *
 * <p>不承担任何特定的内部事务处理, 例如底层事务对象, 也没有事务同步机制.
 */
public abstract class AbstractTransactionStatus implements TransactionStatus {

	private boolean rollbackOnly = false;

	private boolean completed = false;

	private Object savepoint;


	//---------------------------------------------------------------------
	// Handling of current transaction state
	//---------------------------------------------------------------------

	@Override
	public void setRollbackOnly() {
		this.rollbackOnly = true;
	}

	/**
	 * 通过检查此TransactionStatus的本地回滚标志和基础事务的全局回滚标志, 来确定仅回滚标志.
	 */
	@Override
	public boolean isRollbackOnly() {
		return (isLocalRollbackOnly() || isGlobalRollbackOnly());
	}

	/**
	 * 通过检查此TransactionStatus确定仅回滚标志.
	 * <p>如果应用程序在此TransactionStatus对象上调用{@code setRollbackOnly}, 则仅返回"true".
	 */
	public boolean isLocalRollbackOnly() {
		return this.rollbackOnly;
	}

	/**
	 * 用于确定底层事务的全局回滚标志的模板方法.
	 * <p>此实现始终返回{@code false}.
	 */
	public boolean isGlobalRollbackOnly() {
		return false;
	}

	/**
	 * 这种实现是空的, 将flush视为无操作.
	 */
	@Override
	public void flush() {
	}

	/**
	 * 将此事务标记为已完成, 即已提交或已回滚.
	 */
	public void setCompleted() {
		this.completed = true;
	}

	@Override
	public boolean isCompleted() {
		return this.completed;
	}


	//---------------------------------------------------------------------
	// Handling of current savepoint state
	//---------------------------------------------------------------------

	/**
	 * 为此事务设置保存点. 适用于PROPAGATION_NESTED.
	 */
	protected void setSavepoint(Object savepoint) {
		this.savepoint = savepoint;
	}

	/**
	 * 获取此事务的保存点.
	 */
	protected Object getSavepoint() {
		return this.savepoint;
	}

	@Override
	public boolean hasSavepoint() {
		return (this.savepoint != null);
	}

	/**
	 * 创建一个保存点并为事务保留它.
	 * 
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 */
	public void createAndHoldSavepoint() throws TransactionException {
		setSavepoint(getSavepointManager().createSavepoint());
	}

	/**
	 * 回滚到为事务保留的保存点, 然后立即释放保存点.
	 */
	public void rollbackToHeldSavepoint() throws TransactionException {
		if (!hasSavepoint()) {
			throw new TransactionUsageException(
					"Cannot roll back to savepoint - no savepoint associated with current transaction");
		}
		getSavepointManager().rollbackToSavepoint(getSavepoint());
		getSavepointManager().releaseSavepoint(getSavepoint());
		setSavepoint(null);
	}

	/**
	 * 释放为事务保留的保存点.
	 */
	public void releaseHeldSavepoint() throws TransactionException {
		if (!hasSavepoint()) {
			throw new TransactionUsageException(
					"Cannot release savepoint - no savepoint associated with current transaction");
		}
		getSavepointManager().releaseSavepoint(getSavepoint());
		setSavepoint(null);
	}


	//---------------------------------------------------------------------
	// Implementation of SavepointManager
	//---------------------------------------------------------------------

	/**
	 * 如果可能, 此实现将委托给底层事务的SavepointManager.
	 */
	@Override
	public Object createSavepoint() throws TransactionException {
		return getSavepointManager().createSavepoint();
	}

	/**
	 * 如果可能, 此实现将委托给底层事务的SavepointManager.
	 */
	@Override
	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().rollbackToSavepoint(savepoint);
	}

	/**
	 * 如果可能, 此实现将委托给底层事务的SavepointManager.
	 */
	@Override
	public void releaseSavepoint(Object savepoint) throws TransactionException {
		getSavepointManager().releaseSavepoint(savepoint);
	}

	/**
	 * 如果可能, 返回底层事务的SavepointManager.
	 * <p>默认实现始终抛出NestedTransactionNotSupportedException.
	 * 
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 */
	protected SavepointManager getSavepointManager() {
		throw new NestedTransactionNotSupportedException("This transaction does not support savepoints");
	}

}
