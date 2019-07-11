package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

/**
 * {@link TransactionDefinition}实现, 将所有调用委托给给定的目标{@link TransactionDefinition}实例.
 * 需要子类化, 子类重写不应该简单地委托给目标实例的特定方法.
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionDefinition implements TransactionDefinition, Serializable {

	private final TransactionDefinition targetDefinition;


	/**
	 * @param targetDefinition 要委托给的目标TransactionAttribute
	 */
	public DelegatingTransactionDefinition(TransactionDefinition targetDefinition) {
		Assert.notNull(targetDefinition, "Target definition must not be null");
		this.targetDefinition = targetDefinition;
	}


	@Override
	public int getPropagationBehavior() {
		return this.targetDefinition.getPropagationBehavior();
	}

	@Override
	public int getIsolationLevel() {
		return this.targetDefinition.getIsolationLevel();
	}

	@Override
	public int getTimeout() {
		return this.targetDefinition.getTimeout();
	}

	@Override
	public boolean isReadOnly() {
		return this.targetDefinition.isReadOnly();
	}

	@Override
	public String getName() {
		return this.targetDefinition.getName();
	}


	@Override
	public boolean equals(Object obj) {
		return this.targetDefinition.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.targetDefinition.hashCode();
	}

	@Override
	public String toString() {
		return this.targetDefinition.toString();
	}

}
