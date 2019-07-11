package org.springframework.transaction;

/**
 * 指定API的接口, 以通用的编程方式管理事务保存点.
 * 通过TransactionStatus进行扩展, 以公开特定事务的保存点管理功能.
 *
 * <p>请注意, 保存点只能在活动事务中工作.
 * 只需使用此程序化保存点处理即可满足高级需求; 否则, 最好使用PROPAGATION_NESTED进行子事务.
 *
 * <p>此接口受JDBC 3.0的Savepoint机制启发, 但独立于任何特定的持久化技术.
 */
public interface SavepointManager {

	/**
	 * 创建一个新的保存点.
	 * 可以通过{@code rollbackToSavepoint}回滚到特定保存点, 并通过{@code releaseSavepoint}显式释放不再需要的保存点.
	 * <p>请注意, 大多数事务管理器将在事务完成时自动释放保存点.
	 * 
	 * @return 保存点对象, 传递到{@link #rollbackToSavepoint}或{@link #releaseSavepoint}
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果无法创建保存点, 例如因为事务处于不适当的状态
	 */
	Object createSavepoint() throws TransactionException;

	/**
	 * 回滚到给定的保存点.
	 * <p>之后不会自动释放保存点.
	 * 可以显式调用{@link #releaseSavepoint(Object)}或依赖于事务完成时的自动释放.
	 * 
	 * @param savepoint 要回滚到的保存点
	 * 
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果回滚失败
	 */
	void rollbackToSavepoint(Object savepoint) throws TransactionException;

	/**
	 * 显式释放给定的保存点.
	 * <p>请注意, 大多数事务管理器将在事务完成时自动释放保存点.
	 * <p>如果在事务完成时最终发生适当的资源清理, 实现应尽可能无声地失败.
	 * 
	 * @param savepoint 要释放的保存点
	 * 
	 * @throws NestedTransactionNotSupportedException 如果底层事务不支持保存点
	 * @throws TransactionException 如果释放失败
	 */
	void releaseSavepoint(Object savepoint) throws TransactionException;

}
