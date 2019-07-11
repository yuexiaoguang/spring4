package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * 枚举, 表示与{@link Transactional}注释一起使用的事务隔离级别, 对应于{@link TransactionDefinition}接口.
 */
public enum Isolation {

	/**
	 * 使用底层数据存储的默认隔离级别.
	 * 所有其他级别对应于JDBC隔离级别.
	 */
	DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

	/**
	 * 一个常量, 表示可以进行脏读, 不可重复读和幻像读.
	 * 此级别允许在提交该行中的任何更改之前, 由另一个事务读取由一个事务更改的行 ("脏读").
	 * 如果回滚任何更改, 则第二个事务将检索到无效行.
	 */
	READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

	/**
	 * 一个常量, 指示禁止脏读; 可以发生不可重复的读取和幻像读取.
	 * 此级别仅禁止事务读取具有未提交更改的行.
	 */
	READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

	/**
	 * 一个常量, 表示禁止脏读和不可重复读; 可以发生幻像读取.
	 * 此级别禁止事务读取具有未提交更改的行, 并且还禁止一个事务读取行, 第二个事务更改行,
	 * 然后第一个事务重新读取行, 第二次获取不同值的情况 ("不可重复的读取").
	 */
	REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

	/**
	 * 一个常量, 表示禁止脏读, 不可重复读和幻像读.
	 * 此级别包括{@code ISOLATION_REPEATABLE_READ}中的禁止, 并进一步禁止一个事务读取满足{@code WHERE}条件的所有行,
	 * 第二个事务插入满足{@code WHERE}条件的行, 第一个事务使用相同的条件重新读取, 在第二次读取中检索附加的"幻像"行的情况.
	 */
	SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);


	private final int value;


	Isolation(int value) { this.value = value; }

	public int value() { return this.value; }

}
