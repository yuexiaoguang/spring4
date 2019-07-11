package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.util.Assert;

/**
 * 复合{@link TransactionAttributeSource}实现, 它迭代{@link TransactionAttributeSource}实例的给定数组.
 */
@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private final TransactionAttributeSource[] transactionAttributeSources;


	/**
	 * @param transactionAttributeSources 要合并的TransactionAttributeSource实例
	 */
	public CompositeTransactionAttributeSource(TransactionAttributeSource[] transactionAttributeSources) {
		Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array must not be null");
		this.transactionAttributeSources = transactionAttributeSources;
	}

	/**
	 * 返回此CompositeTransactionAttributeSource组合的TransactionAttributeSource实例.
	 */
	public final TransactionAttributeSource[] getTransactionAttributeSources() {
		return this.transactionAttributeSources;
	}


	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		for (TransactionAttributeSource tas : this.transactionAttributeSources) {
			TransactionAttribute ta = tas.getTransactionAttribute(method, targetClass);
			if (ta != null) {
				return ta;
			}
		}
		return null;
	}
}
