package org.springframework.transaction.jta;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.springframework.util.Assert;

/**
 * 托管JTA事务句柄的适配器, 获取 JTA {@link javax.transaction.TransactionManager}引用,
 * 并为其创建JTA {@link javax.transaction.Transaction}句柄.
 */
public class ManagedTransactionAdapter implements Transaction {

	private final TransactionManager transactionManager;


	/**
	 * @param transactionManager 要包装的JTA TransactionManager
	 */
	public ManagedTransactionAdapter(TransactionManager transactionManager) throws SystemException {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回此适配器委托给的JTA TransactionManager.
	 */
	public final TransactionManager getTransactionManager() {
		return this.transactionManager;
	}


	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, SystemException {
		this.transactionManager.commit();
	}

	@Override
	public void rollback() throws SystemException {
		this.transactionManager.rollback();
	}

	@Override
	public void setRollbackOnly() throws SystemException {
		this.transactionManager.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

	@Override
	public boolean enlistResource(XAResource xaRes) throws RollbackException, SystemException {
		return this.transactionManager.getTransaction().enlistResource(xaRes);
	}

	@Override
	public boolean delistResource(XAResource xaRes, int flag) throws SystemException {
		return this.transactionManager.getTransaction().delistResource(xaRes, flag);
	}

	@Override
	public void registerSynchronization(Synchronization sync) throws RollbackException, SystemException {
		this.transactionManager.getTransaction().registerSynchronization(sync);
	}

}
