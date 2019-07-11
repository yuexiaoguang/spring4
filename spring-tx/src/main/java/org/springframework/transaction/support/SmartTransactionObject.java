package org.springframework.transaction.support;

import java.io.Flushable;

/**
 * 由能够返回内部回滚标记的事务对象实现的接口, 通常来自另一个参与并将其标记为仅回滚的事务.
 *
 * <p>由DefaultTransactionStatus自动检测, 以便始终返回当前的rollbackOnly标志,
 * 即使不是由当前TransactionStatus产生的.
 */
public interface SmartTransactionObject extends Flushable {

	/**
	 * 返回事务是否在内部标记为仅回滚.
	 * 例如, 可以检查JTA UserTransaction.
	 */
	boolean isRollbackOnly();

	/**
	 * 如果适用, 将底层会话刷新到数据存储区: 例如, 所有受影响的Hibernate/JPA会话.
	 */
	@Override
	void flush();

}
