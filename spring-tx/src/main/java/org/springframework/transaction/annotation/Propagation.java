package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * 表示与{@link Transactional}注解一起使用的事务传播行为的枚举, 对应于{@link TransactionDefinition}接口.
 */
public enum Propagation {

	/**
	 * 支持当前事务, 如果不存在则创建新事务.
	 * 类似于同名的EJB事务属性.
	 * <p>这是事务注解的默认设置.
	 */
	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

	/**
	 * 支持当前事务, 如果不存在则以非事务方式执行.
	 * 类似于同名的EJB事务属性.
	 * <p>Note: 对于具有事务同步的事务管理器, PROPAGATION_SUPPORTS与根本没有事务略有不同,
	 * 因为它定义了同步将应用于的事务范围.
	 * 因此, 将为整个指定范围共享相同的资源(JDBC Connection, Hibernate Session, etc).
	 * 请注意, 这取决于事务管理器的实际同步配置.
	 */
	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

	/**
	 * 支持当前事务, 如果不存在则抛出异常.
	 * 类似于同名的EJB事务属性.
	 */
	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

	/**
	 * 创建一个新事务, 并暂停当前事务.
	 * 类似于同名的EJB事务属性.
	 * <p><b>NOTE:</b> 实际的事务暂停将无法在所有事务管理器上开箱即用.
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它需要{@code javax.transaction.TransactionManager}使其可用 (在标准Java EE中是特定于服务器的).
	 */
	REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

	/**
	 * 以非事务方式执行, 暂停当前事务.
	 * 类似于同名的EJB事务属性.
	 * <p><b>NOTE:</b> 实际的事务暂停将无法在所有事务管理器上开箱即用.
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它需要{@code javax.transaction.TransactionManager}使其可用 (在标准Java EE中是特定于服务器的).
	 */
	NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

	/**
	 * 以非事务方式执行, 如果事务存在, 抛出异常.
	 * 类似于同名的EJB事务属性.
	 */
	NEVER(TransactionDefinition.PROPAGATION_NEVER),

	/**
	 * 如果当前事务存在, 则在嵌套事务中执行, 其行为类似于PROPAGATION_REQUIRED.
	 * EJB中没有类似的功能.
	 * <p>Note: 实际创建嵌套事务仅适用于特定事务管理器.
	 * 开箱即用, 这仅适用于处理JDBC 3.0驱动程序时的JDBC DataSourceTransactionManager.
	 * 一些JTA提供者也可能支持嵌套事务.
	 */
	NESTED(TransactionDefinition.PROPAGATION_NESTED);


	private final int value;


	Propagation(int value) { this.value = value; }

	public int value() { return this.value; }

}
