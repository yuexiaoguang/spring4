package org.springframework.transaction.support;

import org.springframework.core.Ordered;

/**
 * 包含空方法实现的简单{@link TransactionSynchronization}适配器, 用于更轻松地覆盖单个方法.
 *
 * <p>还实现了{@link Ordered}接口, 以便以声明方式控制同步的执行顺序.
 * 默认的{@link #getOrder() order}是{{@link Ordered#LOWEST_PRECEDENCE}, 表示延迟执行; 为较早的执行返回较低的值.
 */
public abstract class TransactionSynchronizationAdapter implements TransactionSynchronization, Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCommit() {
	}

	@Override
	public void afterCompletion(int status) {
	}

}
