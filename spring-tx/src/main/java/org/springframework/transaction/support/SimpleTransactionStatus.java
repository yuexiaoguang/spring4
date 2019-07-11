package org.springframework.transaction.support;

/**
 * 简单的{@link org.springframework.transaction.TransactionStatus}实现.
 *
 * <p>派生自{@link AbstractTransactionStatus}并添加一个显式的{@link #isNewTransaction() "newTransaction"}标志.
 *
 * <p>Spring的任何预先构建的{@link org.springframework.transaction.PlatformTransactionManager}实现都不使用此类.
 * 它主要作为自定义事务管理器实现的开始, 作为测试事务代码的静态模拟 (作为模拟{@code PlatformTransactionManager}的一部分,
 * 或作为传递到要测试的{@link TransactionCallback}的参数提供).
 */
public class SimpleTransactionStatus extends AbstractTransactionStatus {

	private final boolean newTransaction;


	public SimpleTransactionStatus() {
		this(true);
	}

	/**
	 * @param newTransaction 是否表示新事务
	 */
	public SimpleTransactionStatus(boolean newTransaction) {
		this.newTransaction = newTransaction;
	}


	@Override
	public boolean isNewTransaction() {
		return this.newTransaction;
	}

}
