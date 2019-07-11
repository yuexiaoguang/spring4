package org.springframework.transaction;

/**
 * 这是Spring的事务基础结构的中心接口.
 * 应用程序可以直接使用它, 但它主要不是作为API:
 * 通常, 应用程序将使用TransactionTemplate或通过AOP进行声明式事务划分.
 *
 * <p>对于实现者, 建议从提供的
 * {@link org.springframework.transaction.support.AbstractPlatformTransactionManager}类派生,
 * 该类预先实现定义的传播行为并负责事务同步处理.
 * 子类必须为底层事务的特定状态实现模板方法, 例如: begin, suspend, resume, commit.
 *
 * <p>此策略接口的默认实现是
 * {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 和{@link org.springframework.jdbc.datasource.DataSourceTransactionManager},
 * 它可以作为其他事务策略的实现指南.
 */
public interface PlatformTransactionManager {

	/**
	 * 根据指定的传播行为, 返回当前活动的事务或创建新事务.
	 * <p>请注意, 隔离级别或超时等参数仅适用于新事务, 因此在参与活动事务时会被忽略.
	 * <p>此外, 并非每个事务管理器都支持所有事务定义设置:
	 * 当遇到不支持的设置时, 正确的事务管理器实现应该抛出异常.
	 * <p>上述规则的一个例外是只读标志, 如果不支持显式只读模式, 则应忽略该标志.
	 * 从本质上讲, 只读标志只是潜在优化的提示.
	 * 
	 * @param definition TransactionDefinition实例 (默认情况下可以是{@code null}), 描述传播行为, 隔离级别, 超时等.
	 * 
	 * @return 表示新事务或当前事务的事务状态对象
	 * @throws TransactionException 在查找, 创建或系统错误的情况下
	 * @throws IllegalTransactionStateException 如果无法执行给定的事务定义 (例如, 当前活动的事务与指定的传播行为冲突)
	 */
	TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException;

	/**
	 * 就给定事务的状态提交给定事务. 如果事务已以编程方式标记为仅回滚, 则执行回滚.
	 * <p>如果事务不是新事务, 则省略提交以正确参与周围事务.
	 * 如果先前的事务已被暂停, 以能够创建新事务, 则在提交新事务后恢复上一个事务.
	 * <p>请注意, 当提交调用完成时, 无论是正常还是抛出异常, 事务都必须完全完成和清除. 在这种情况下, 不应该进行回滚调用.
	 * <p>如果此方法抛出除TransactionException之外的异常, 则某些提交前错误会导致提交尝试失败.
	 * 例如, an O/R Mapping工具可能在提交之前尝试刷新对数据库的更改, 结果是DataAccessException导致事务失败.
	 * 在这种情况下, 原始异常将传播给此提交方法的调用者.
	 * 
	 * @param status {@code getTransaction}方法返回的对象
	 * 
	 * @throws UnexpectedRollbackException 在事务协调器启动的意外回滚的情况下
	 * @throws HeuristicCompletionException 在由事务协调器的启发式决策引起的事务失败的情况下
	 * @throws TransactionSystemException 在提交或系统错误的情况下 (通常由基本资源故障引起)
	 * @throws IllegalTransactionStateException 如果给定的事务已经完成 (即已提交或已回滚)
	 */
	void commit(TransactionStatus status) throws TransactionException;

	/**
	 * 执行给定事务的回滚.
	 * <p>如果事务不是新事务, 只需将其设置为回滚, 以便正确参与周围的事务.
	 * 如果先前的事务已被暂停, 以能够创建新事务, 则在回滚新事务后恢复上一个事务.
	 * <p><b>如果提交引发异常, 请不要在事务上调用回滚.</b>
	 * 即使在提交异常的情况下, 事务也将在提交返回时完成并清除.
	 * 因此, 提交失败后的回滚调用将导致IllegalTransactionStateException.
	 * 
	 * @param status {@code getTransaction}方法返回的对象
	 * 
	 * @throws TransactionSystemException 在回滚或系统错误的情况下 (通常由基本资源故障引起)
	 * @throws IllegalTransactionStateException 如果给定的事务已经完成 (即已提交或已回滚)
	 */
	void rollback(TransactionStatus status) throws TransactionException;

}
