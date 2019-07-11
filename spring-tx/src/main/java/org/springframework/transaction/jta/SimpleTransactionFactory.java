package org.springframework.transaction.jta;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.springframework.util.Assert;

/**
 * {@link TransactionFactory}策略接口的默认实现, 只需包装标准JTA {@link javax.transaction.TransactionManager}.
 *
 * <p>不支持事务名称; 只是忽略任何指定的名称.
 */
public class SimpleTransactionFactory implements TransactionFactory {

	private final TransactionManager transactionManager;


	/**
	 * @param transactionManager 要包装的JTA TransactionManager
	 */
	public SimpleTransactionFactory(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}


	@Override
	public Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException {
		if (timeout >= 0) {
			this.transactionManager.setTransactionTimeout(timeout);
		}
		this.transactionManager.begin();
		return new ManagedTransactionAdapter(this.transactionManager);
	}

	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return false;
	}

}
