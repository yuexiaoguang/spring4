package org.springframework.transaction.interceptor;

import java.io.Serializable;

import org.springframework.transaction.support.DelegatingTransactionDefinition;

/**
 * {@link TransactionAttribute}实现, 它将所有调用委托给给定的目标{@link TransactionAttribute}实例.
 * 它需要子类化, 子类重写不应该简单地委托给目标实例的特定方法.
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute extends DelegatingTransactionDefinition
		implements TransactionAttribute, Serializable {

	private final TransactionAttribute targetAttribute;


	/**
	 * @param targetAttribute 要委托的目标TransactionAttribute
	 */
	public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
		super(targetAttribute);
		this.targetAttribute = targetAttribute;
	}


	@Override
	public String getQualifier() {
		return this.targetAttribute.getQualifier();
	}

	@Override
	public boolean rollbackOn(Throwable ex) {
		return this.targetAttribute.rollbackOn(ex);
	}

}
