package org.springframework.transaction;

import java.sql.Connection;

/**
 * 定义符合Spring的事务属性的接口.
 * 基于类似于EJB CMT属性的传播行为定义.
 *
 * <p>请注意, 除非启动实际的新事务, 否则不会应用隔离级别和超时设置.
 * 由于只有{@link #PROPAGATION_REQUIRED}, {@link #PROPAGATION_REQUIRES_NEW}
 * 和{@link #PROPAGATION_NESTED}可能导致这种情况, 因此在其他情况下指定这些设置通常没有意义.
 * 此外, 请注意, 并非所有事务管理器都支持这些高级功能, 因此在给定非默认值时可能会抛出相应的异常.
 *
 * <p>{@link #isReadOnly() 只读标志}适用于任何事务上下文, 无论是由实际资源事务支持, 还是在资源级别以非事务方式操作.
 * 在后一种情况下, 该标志仅适用于应用程序内的托管资源, 例如Hibernate {@code Session}.
 */
public interface TransactionDefinition {

	/**
	 * 支持当前事务; 如果不存在则创建一个新的.
	 * 类似于同名的EJB事务属性.
	 * <p>这通常是事务定义的默认设置, 通常定义事务同步范围.
	 */
	int PROPAGATION_REQUIRED = 0;

	/**
	 * 支持当前事务; 如果不存在, 则执行非事务性.
	 * 类似于同名的EJB事务属性.
	 * <p><b>NOTE:</b> 对于具有事务同步的事务管理器, {@code PROPAGATION_SUPPORTS}与根本没有事务略有不同,
	 * 因为它定义了同步可能适用于的事务范围.
	 * 因此, 将为整个指定范围共享相同的资源 (JDBC {@code Connection}, Hibernate {@code Session}等).
	 * 请注意, 确切的行为取决于事务管理器的实际同步配置!
	 * <p>一般情况下, 请谨慎使用{@code PROPAGATION_SUPPORTS}!
	 * 特别是, 不要依赖于{@code PROPAGATION_SUPPORTS}范围内的{@code PROPAGATION_REQUIRED}
	 * 或{@code PROPAGATION_REQUIRES_NEW} (这可能会导致运行时出现同步冲突).
	 * 如果这样的嵌套是不可避免的, 请确保适当地配置事务管理器 (通常切换到"实际事务上的同步").
	 */
	int PROPAGATION_SUPPORTS = 1;

	/**
	 * 支持当前事务; 如果不存在当前事务则抛出异常. 类似于同名的EJB事务属性.
	 * <p>请注意, {@code PROPAGATION_MANDATORY}范围内的事务同步将始终由周围的事务驱动.
	 */
	int PROPAGATION_MANDATORY = 2;

	/**
	 * 创建一个新事务, 暂停当前事务. 类似于同名的EJB事务属性.
	 * <p><b>NOTE:</b> 实际的事务暂停将无法在所有事务管理器上开箱即用.
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它需要{@code javax.transaction.TransactionManager}使其可用 (在标准Java EE中是特定于服务器的).
	 * <p>{@code PROPAGATION_REQUIRES_NEW}范围始终定义自己的事务同步. 现有同步将被暂停并适当恢复.
	 */
	int PROPAGATION_REQUIRES_NEW = 3;

	/**
	 * 不支持当前事务; 而是总是以非事务方式执行.
	 * 类似于同名的EJB事务属性.
	 * <p><b>NOTE:</b> 实际的事务暂停将无法在所有事务管理器上开箱即用.
	 * 这特别适用于{@link org.springframework.transaction.jta.JtaTransactionManager},
	 * 它需要{@code javax.transaction.TransactionManager}使其可用 (在标准Java EE中是特定于服务器的).
	 * <p>请注意, {@code PROPAGATION_NOT_SUPPORTED}范围内的事务同步<i>不</i>可用. 现有同步将被暂停并适当恢复.
	 */
	int PROPAGATION_NOT_SUPPORTED = 4;

	/**
	 * 不支持当前事务; 如果当前事务存在则抛出异常. 类似于同名的EJB事务属性.
	 * <p>请注意, {@code PROPAGATION_NEVER}范围内的事务同步<i>不</i>可用.
	 */
	int PROPAGATION_NEVER = 5;

	/**
	 * 如果当前事务存在, 则在嵌套事务中执行, 其行为类似于{@link #PROPAGATION_REQUIRED}. EJB中没有类似的功能.
	 * <p><b>NOTE:</b> 实际创建嵌套事务仅适用于特定事务管理器.
	 * 开箱即用, 这仅适用于使用JDBC 3.0驱动程序时的JDBC
	 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
	 * 一些JTA提供者也可能支持嵌套事务.
	 */
	int PROPAGATION_NESTED = 6;


	/**
	 * 使用底层数据存储的默认隔离级别.
	 * 所有其他级别对应于JDBC隔离级别.
	 */
	int ISOLATION_DEFAULT = -1;

	/**
	 * 表示可能发生脏读, 不可重复读和幻像读.
	 * <p>此级别允许在提交该行中的任何更改之前, 由另一个事务读取由一个事务更改的行 ("脏读").
	 * 如果回滚任何更改, 则第二个事务将检索到无效行.
	 */
	int ISOLATION_READ_UNCOMMITTED = Connection.TRANSACTION_READ_UNCOMMITTED;

	/**
	 * 表示禁止脏读; 可以发生不可重复的读取和幻像读取.
	 * <p>此级别仅禁止事务读取具有未提交更改的行.
	 */
	int ISOLATION_READ_COMMITTED = Connection.TRANSACTION_READ_COMMITTED;

	/**
	 * 表示防止脏读和不可重复读; 可以发生幻像读取.
	 * <p>此级别禁止事务读取具有未提交更改的行, 并且还禁止一个事务读取行, 第二个事务更改行,
	 * 然后第一个事务重新读取行, 获取到不同值的情况 ("不可重复读").
	 */
	int ISOLATION_REPEATABLE_READ = Connection.TRANSACTION_REPEATABLE_READ;

	/**
	 * 表示防止脏读, 不可重复读和幻像读.
	 * <p>此级别包括{@link #ISOLATION_REPEATABLE_READ}中的禁止,
	 * 并进一步禁止第一个事务读取满足{@code WHERE}条件的所有行, 第二个事务插入满足{@code WHERE}条件的行,
	 * 然后第一个事务重新读取相同的条件, 在第二个读取中检索附加的"幻像"行.
	 */
	int ISOLATION_SERIALIZABLE = Connection.TRANSACTION_SERIALIZABLE;


	/**
	 * 使用底层事务系统的默认超时, 如果不支持超时, 则使用no​​ne.
	 */
	int TIMEOUT_DEFAULT = -1;


	/**
	 * 返回传播行为.
	 * <p>必须返回{@link TransactionDefinition 此接口}上定义的{@code PROPAGATION_XXX}常量之一.
	 * 
	 * @return 传播行为
	 */
	int getPropagationBehavior();

	/**
	 * 返回隔离级别.
	 * <p>必须返回{@link TransactionDefinition 此接口}上定义的{@code ISOLATION_XXX}常量之一.
	 * 这些常量旨在匹配{@link java.sql.Connection}上相同常量的值.
	 * <p>专门设计用于{@link #PROPAGATION_REQUIRED}或{@link #PROPAGATION_REQUIRES_NEW}, 因为它仅适用于新启动的事务.
	 * 如果希望隔离级别声明, 在参与具有不同隔离级别的现有事务时被拒绝,
	 * 请考虑在事务管理器上将"validateExistingTransactions"标志切换为"true"标志切换为.
	 * <p>请注意, 不支持自定义隔离级别的事务管理器在给定除{@link #ISOLATION_DEFAULT}之外的任何其他级别时将抛出异常.
	 * 
	 * @return 隔离级别
	 */
	int getIsolationLevel();

	/**
	 * 返回事务超时.
	 * <p>必须返回秒数, 或{@link #TIMEOUT_DEFAULT}.
	 * <p>专门设计用于{@link #PROPAGATION_REQUIRED}或{@link #PROPAGATION_REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务.
	 * <p>请注意, 不支持超时的事务管理器在给定除{@link #TIMEOUT_DEFAULT}以外的任何其他超时时将抛出异常.
	 * 
	 * @return 事务超时
	 */
	int getTimeout();

	/**
	 * 返回是否优化只读事务.
	 * <p>只读标志适用于任何事务上下文, 无论是由实际资源事务支持 ({@link #PROPAGATION_REQUIRED}/{@link #PROPAGATION_REQUIRES_NEW}),
	 * 还是在资源级别以非事务方式运行 ({@link #PROPAGATION_SUPPORTS}).
	 * 在后一种情况下, 该标志仅适用于应用程序内的托管资源, 例如Hibernate {@code Session}.
	 * <p>这仅仅是实际事务子系统的提示; 它<i>不一定</i>导致写访问尝试的失败.
	 * 当被要求进行只读事务时, 无法解释只读提示的事务管理器将<i>不会</i>抛出异常.
	 * 
	 * @return {@code true} 如果要将事务优化为只读
	 */
	boolean isReadOnly();

	/**
	 * 返回此事务的名称. 可以是{@code null}.
	 * <p>这将用作事务监视器中显示的事务名称 (例如, WebLogic).
	 * <p>对于Spring的声明性事务, 公开的名称将默认是{@code 完全限定的类名 + "." + 方法名称}.
	 * 
	 * @return 此事务的名称
	 */
	String getName();

}
