package org.springframework.test.context.transaction;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * {@code TestTransaction}提供了一组静态实用程序方法,
 * 用于在<em>test</em>方法, <em>before</em>方法, 和<em>after</em>方法中与<em>测试管理的事务</em>交互.
 *
 * <p>有关<em>测试管理的事务</em>的详细说明, 请参阅{@link TransactionalTestExecutionListener}的javadocs.
 *
 * <p>只要启用了{@code TransactionalTestExecutionListener}, 就会自动提供对{@code TestTransaction}的支持.
 * 请注意, {@code TransactionalTestExecutionListener}通常默认启用,
 * 但也可以通过{@link TestExecutionListeners @TestExecutionListeners}注解手动启用它.
 */
public class TestTransaction {

	/**
	 * 确定测试管理的事务当前是否<em>活动</em>.
	 * 
	 * @return {@code true} 如果测试管理的事务当前处于活动状态
	 */
	public static boolean isActive() {
		TransactionContext transactionContext = TransactionContextHolder.getCurrentTransactionContext();
		if (transactionContext != null) {
			TransactionStatus transactionStatus = transactionContext.getTransactionStatus();
			return (transactionStatus != null && !transactionStatus.isCompleted());
		}
		return false;
	}

	/**
	 * 确定当前测试管理的事务是否已被{@linkplain #flagForRollback() 标记为回滚}或{@linkplain #flagForCommit() 标记为提交}.
	 * 
	 * @return {@code true} 如果当前测试管理的事务被标记为回滚; {@code false} 如果当前测试管理的事务被标记为提交
	 * @throws IllegalStateException 如果当前测试的事务处于非活动状态
	 */
	public static boolean isFlaggedForRollback() {
		return requireCurrentTransactionContext().isFlaggedForRollback();
	}

	/**
	 * 标记当前测试管理的事务为<em>回滚</em>.
	 * <p>调用此方法将<em>不</em>结束当前事务.
	 * 相反, 此标志的值将用于确定当前测试管理的事务是否应该在{@linkplain #end ended}后回滚或提交.
	 * 
	 * @throws IllegalStateException 如果当前测试没有活动的事务
	 */
	public static void flagForRollback() {
		setFlaggedForRollback(true);
	}

	/**
	 * 标记当前测试管理的事务为<em>提交</em>.
	 * <p>调用此方法将<em>不</em>结束当前事务.
	 * 相反, 此标志的值将用于确定当前测试管理的事务是否应该在{@linkplain #end ended}后回滚或提交.
	 * 
	 * @throws IllegalStateException 如果当前测试没有活动的事务
	 */
	public static void flagForCommit() {
		setFlaggedForRollback(false);
	}

	/**
	 * 启动一个新的测试管理的事务.
	 * <p>如果已调用{@link #end}或之前未启动任何事务, 则仅调用此方法.
	 * 
	 * @throws IllegalStateException 如果无法检索事务上下文, 或者当前测试的事务已处于活动状态
	 */
	public static void start() {
		requireCurrentTransactionContext().startTransaction();
	}

	/**
	 * 根据{@linkplain #isFlaggedForRollback 回滚标志}, 立即强制执行当前测试管理的事务的<em>提交</em>或<em>回滚</em>.
	 * 
	 * @throws IllegalStateException 如果无法检索事务上下文, 或者当前测试的事务处于非活动状态
	 */
	public static void end() {
		requireCurrentTransactionContext().endTransaction();
	}


	private static TransactionContext requireCurrentTransactionContext() {
		TransactionContext txContext = TransactionContextHolder.getCurrentTransactionContext();
		Assert.state(txContext != null, "TransactionContext is not active");
		return txContext;
	}

	private static void setFlaggedForRollback(boolean flag) {
		requireCurrentTransactionContext().setFlaggedForRollback(flag);
	}

}
