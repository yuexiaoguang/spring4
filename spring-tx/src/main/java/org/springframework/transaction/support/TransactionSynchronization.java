package org.springframework.transaction.support;

import java.io.Flushable;

/**
 * 事务同步回调的接口.
 * 由AbstractPlatformTransactionManager支持.
 *
 * <p>TransactionSynchronization实现可以实现Ordered接口来影响它们的执行顺序.
 * 未实现Ordered接口的同步将附加到同步链的末尾.
 *
 * <p>Spring执行的系统同步使用特定的顺序值, 允许与其执行顺序进行细粒度的交互.
 */
public interface TransactionSynchronization extends Flushable {

	/** 正确提交时的完成状态 */
	int STATUS_COMMITTED = 0;

	/** 正确回滚时的完成状态 */
	int STATUS_ROLLED_BACK = 1;

	/** 启发式混合完成或系统错误时的完成状态 */
	int STATUS_UNKNOWN = 2;


	/**
	 * 暂停此同步.
	 * 从TransactionSynchronizationManager解绑资源.
	 */
	void suspend();

	/**
	 * 恢复此同步.
	 * 将资源重新绑定到TransactionSynchronizationManager.
	 */
	void resume();

	/**
	 * 如果适用, 将底层会话刷新到数据存储区: 例如, Hibernate/JPA会话.
	 */
	@Override
	void flush();

	/**
	 * 在事务提交之前调用 (在"beforeCompletion"之前).
	 * 可以刷新事务 O/R Mapping到数据库的会话.
	 * <p>此回调<i>不</i>表示事务将实际提交. 调用此方法后仍可以执行回滚决策.
	 * 此回调意味着仅在提交仍有机会发生时才执行相关的工作, 例如将SQL语句刷新到数据库.
	 * <p>请注意，异常将传播到提交调用者并导致事务回滚.
	 * 
	 * @param readOnly 是否将事务定义为只读事务
	 * 
	 * @throws RuntimeException 如果有错误; 将<b>传播给调用者</b> (note: 不要在这里抛出TransactionException子类!)
	 */
	void beforeCommit(boolean readOnly);

	/**
	 * 在事务提交/回滚之前调用.
	 * 可以在事务完成<i>之前</i>执行资源清理.
	 * <p>即使在{@code beforeCommit}引发异常时, 也会在{@code beforeCommit}之后调用此方法.
	 * 对于任何结果, 此回调允许在事务完成之前关闭资源.
	 * 
	 * @throws RuntimeException 如果有错误; 将<b>记录但不传播</b>
	 * (note: 不要在这里抛出TransactionException子类!)
	 */
	void beforeCompletion();

	/**
	 * 在事务提交后调用. 
	 * 主事务<i>成功</i>提交<i>之后</i>, 可以执行进一步的操作.
	 * <p>可以提交成功提交主事务后的其他操作, 例如确认消息或电子邮件.
	 * <p><b>NOTE:</b> 该事务已经提交, 但事务资源可能仍然是活动的和可访问的.
	 * 因此, 此时触发的任何数据访问代码仍将"参与"原始事务, 允许执行一些清理 (不再执行任何提交!),
	 * 除非它明确声明它需要在单独的事务中运行.
	 * Hence: <b>对于从此处调用的任何事务操作, 使用{@code PROPAGATION_REQUIRES_NEW}.</b>
	 * 
	 * @throws RuntimeException 如果有错误; 将<b>传播给调用者</b> (note: 不要在这里抛出TransactionException子类!)
	 */
	void afterCommit();

	/**
	 * 在事务提交/回滚后调用.
	 * 事务完成<i>之后</i>, 可以执行资源清理.
	 * <p><b>NOTE:</b> 事务已经提交或回滚, 但事务资源可能仍然是活动的和可访问的.
	 * 因此, 此时触发的任何数据访问代码仍将"参与"原始事务, 允许执行一些清理 (不再执行任何提交!),
	 * 除非它明确声明它需要在单独的事务中运行.
	 * Hence: <b>对于从此处调用的任何事务操作, 使用{@code PROPAGATION_REQUIRES_NEW}.</b>
	 * 
	 * @param status 根据{@code STATUS_*}常量的完成状态
	 * 
	 * @throws RuntimeException 如果有错误; 将<b>记录但不传播</b> (note: 不要在这里抛出TransactionException子类!)
	 */
	void afterCompletion(int status);

}
