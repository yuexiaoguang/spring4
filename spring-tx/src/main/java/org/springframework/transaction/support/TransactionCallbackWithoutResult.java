package org.springframework.transaction.support;

import org.springframework.transaction.TransactionStatus;

/**
 * TransactionCallback实现的简单便利类.
 * 允许实现没有结果的doInTransaction版本, i.e. 不需要return语句.
 */
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {

	@Override
	public final Object doInTransaction(TransactionStatus status) {
		doInTransactionWithoutResult(status);
		return null;
	}

	/**
	 * 在事务上下文中由{@code TransactionTemplate.execute}调用.
	 * 不需要关心事务本身, 尽管它可以通过给定的状态对象检索和影响当前事务的状态, e.g. 设置仅回滚.
	 * <p>回调抛出的RuntimeException被视为强制执行回滚的应用程序异常. 异常会传播到模板的调用者.
	 * <p>请注意, 使用JTA时: JTA事务仅适用于事务性JNDI资源, 因此如果需要事务支持, 则实现需要使用此类资源.
	 * 
	 * @param status 关联的事务状态
	 */
	protected abstract void doInTransactionWithoutResult(TransactionStatus status);

}
