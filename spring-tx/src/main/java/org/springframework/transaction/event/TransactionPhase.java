package org.springframework.transaction.event;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * 应用事务事件监听器的阶段.
 */
public enum TransactionPhase {

	/**
	 * 在事务提交之前触发事件.
	 */
	BEFORE_COMMIT,

	/**
	 * 提交成功完成后触发事件.
	 * <p>Note: 这是{@link #AFTER_COMPLETION}的细化, 因此在相同的完成后事件序列中执行,
	 * (而不是在{@link TransactionSynchronization#afterCommit()}中执行).
	 */
	AFTER_COMMIT,

	/**
	 * 如果事务已回滚, 则触发事件.
	 * <p>Note: 这是{@link #AFTER_COMPLETION}的细化, 因此在相同的完成后事件序列中执行.
	 */
	AFTER_ROLLBACK,

	/**
	 * 在事务完成后触发事件.
	 * <p>对于更细粒度的事件, 使用{@link #AFTER_COMMIT}或{@link #AFTER_ROLLBACK}分别拦截事务提交或回滚.
	 */
	AFTER_COMPLETION

}
