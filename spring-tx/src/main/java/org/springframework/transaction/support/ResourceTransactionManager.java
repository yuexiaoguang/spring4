package org.springframework.transaction.support;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}接口的扩展,
 * 指示在单个目标资源上操作的本机资源事务管理器.
 * 此类事务管理器与JTA事务管理器的不同之处在于, 它们不会将XA事务登记用于开放数量的资源,
 * 而是专注于利用单个目标资源的本机功能和简单性.
 *
 * <p>该接口主要用于事务管理器的抽象内省, 为客户端提供关于给定的事务管理器类型, 以及事务管理器正在运行的具体资源的提示.
 */
public interface ResourceTransactionManager extends PlatformTransactionManager {

	/**
	 * 返回此事务管理器操作的资源工厂, e.g. JDBC DataSource或JMS ConnectionFactory.
	 * <p>此目标资源工厂通常用作每个线程绑定的{@link TransactionSynchronizationManager}的资源的资源键.
	 * 
	 * @return 目标资源工厂 (never {@code null})
	 */
	Object getResourceFactory();

}
