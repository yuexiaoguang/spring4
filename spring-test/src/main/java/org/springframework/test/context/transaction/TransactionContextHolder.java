package org.springframework.test.context.transaction;

import org.springframework.core.NamedInheritableThreadLocal;

/**
 * {@link InheritableThreadLocal}-based holder for the current {@link TransactionContext}.
 */
class TransactionContextHolder {

	private static final ThreadLocal<TransactionContext> currentTransactionContext =
			new NamedInheritableThreadLocal<TransactionContext>("Test Transaction Context");


	static void setCurrentTransactionContext(TransactionContext transactionContext) {
		currentTransactionContext.set(transactionContext);
	}

	static TransactionContext getCurrentTransactionContext() {
		return currentTransactionContext.get();
	}

	static TransactionContext removeCurrentTransactionContext() {
		TransactionContext transactionContext = currentTransactionContext.get();
		currentTransactionContext.remove();
		return transactionContext;
	}

}