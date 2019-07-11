package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * TransactionAttributeSource的非常简单的实现, 它将始终为所有提供给它的方法返回相同的TransactionAttribute.
 * 可以指定TransactionAttribute, 但是默认为PROPAGATION_REQUIRED.
 * 这可以用于希望使用相同的事务属性, 并且所有方法都由事务拦截器处理的情况.
 */
@SuppressWarnings("serial")
public class MatchAlwaysTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();


	/**
	 * 允许使用String格式指定事务属性，例如, "PROPAGATION_REQUIRED".
	 * 
	 * @param transactionAttribute 要使用的transactionAttribute的String形式.
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}


	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		return (method == null || ClassUtils.isUserLevelMethod(method) ? this.transactionAttribute : null);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MatchAlwaysTransactionAttributeSource)) {
			return false;
		}
		MatchAlwaysTransactionAttributeSource otherTas = (MatchAlwaysTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.transactionAttribute, otherTas.transactionAttribute);
	}

	@Override
	public int hashCode() {
		return MatchAlwaysTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.transactionAttribute;
	}

}
