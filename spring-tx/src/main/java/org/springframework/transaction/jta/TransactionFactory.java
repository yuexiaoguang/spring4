package org.springframework.transaction.jta;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * 用于根据指定的事务特征创建JTA {@link javax.transaction.Transaction}对象的策略接口.
 *
 * <p>默认实现, {@link SimpleTransactionFactory}, 只包含一个标准的JTA {@link javax.transaction.TransactionManager}.
 * 此策略接口允许更复杂的实现, 以适应特定于供应商的JTA扩展.
 */
public interface TransactionFactory {

	/**
	 * 根据给定的名称和超时创建一个活动的Transaction对象.
	 * 
	 * @param name 事务名称 (may be {@code null})
	 * @param timeout 事务超时 (-1 表示默认超时)
	 * 
	 * @return 活动的Transaction对象 (never {@code null})
	 * @throws NotSupportedException 如果事务管理器不支持指定类型的事务
	 * @throws SystemException 如果事务管理器无法创建事务
	 */
	Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException;

	/**
	 * 确定底层事务管理器是否支持由资源适配器管理的XA事务 (i.e. 没有明确的XA资源登记).
	 * <p>通常是{@code false}.
	 * 由{@link org.springframework.jca.endpoint.AbstractMessageEndpointFactory}检查,
	 * 以区分无效配置和有效的ResourceAdapter管理的事务.
	 */
	boolean supportsResourceAdapterManagedTransactions();

}
