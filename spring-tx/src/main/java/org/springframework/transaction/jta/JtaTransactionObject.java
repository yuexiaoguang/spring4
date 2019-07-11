package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * JTA事务对象, 表示 {@link javax.transaction.UserTransaction}.
 * 通过Spring的{@link JtaTransactionManager}用作事务对象.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class JtaTransactionObject implements SmartTransactionObject {

	private final UserTransaction userTransaction;

	boolean resetTransactionTimeout = false;


	/**
	 * @param userTransaction 当前事务的JTA UserTransaction
	 * (共享对象或通过新的每个事务lookuip检索)
	 */
	public JtaTransactionObject(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * 返回当前事务的JTA UserTransaction对象.
	 */
	public final UserTransaction getUserTransaction() {
		return this.userTransaction;
	}


	/**
	 * 此实现检查UserTransaction的仅回滚标志.
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
	 * 此实现触发刷新回调, 假设它们将刷新所有受影响的ORM会话.
	 */
	@Override
	public void flush() {
		TransactionSynchronizationUtils.triggerFlush();
	}

}
