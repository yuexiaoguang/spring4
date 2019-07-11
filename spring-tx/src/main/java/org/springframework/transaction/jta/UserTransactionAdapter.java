package org.springframework.transaction.jta;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.util.Assert;

/**
 * JTA UserTransaction句柄的适配器, 获取JTA {@link javax.transaction.TransactionManager}引用,
 * 并为其创建JTA {@link javax.transaction.UserTransaction}句柄.
 *
 * <p>JTA UserTransaction接口是JTA TransactionManager接口的精确子集.
 * 不幸的是, 它不能作为TransactionManager的超级接口,
 * 这需要在通过UserTransaction接口与TransactionManager句柄通信时使用此类的适配器.
 *
 * <p>在某些情况下, Spring的{@link JtaTransactionManager}在内部使用. 不适合直接用于应用程序代码.
 */
public class UserTransactionAdapter implements UserTransaction {

	private final TransactionManager transactionManager;


	/**
	 * @param transactionManager 要包装的JTA TransactionManager
	 */
	public UserTransactionAdapter(TransactionManager transactionManager) {
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
	public void setTransactionTimeout(int timeout) throws SystemException {
		this.transactionManager.setTransactionTimeout(timeout);
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		this.transactionManager.begin();
	}

	@Override
	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, SystemException {
		this.transactionManager.commit();
	}

	@Override
	public void rollback() throws SecurityException, SystemException {
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

}
