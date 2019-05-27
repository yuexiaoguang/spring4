package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.util.Assert;

/**
 * Composite {@link TransactionAttributeSource} implementation that iterates
 * over a given array of {@link TransactionAttributeSource} instances.
 */
@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private final TransactionAttributeSource[] transactionAttributeSources;


	/**
	 * Create a new CompositeTransactionAttributeSource for the given sources.
	 * @param transactionAttributeSources the TransactionAttributeSource instances to combine
	 */
	public CompositeTransactionAttributeSource(TransactionAttributeSource[] transactionAttributeSources) {
		Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array must not be null");
		this.transactionAttributeSources = transactionAttributeSources;
	}

	/**
	 * Return the TransactionAttributeSource instances that this
	 * CompositeTransactionAttributeSource combines.
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
