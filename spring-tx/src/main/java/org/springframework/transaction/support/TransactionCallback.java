package org.springframework.transaction.support;

import org.springframework.transaction.TransactionStatus;

/**
 * Callback interface for transactional code. Used with {@link TransactionTemplate}'s
 * {@code execute} method, often as anonymous class within a method implementation.
 *
 * <p>Typically used to assemble various calls to transaction-unaware data access
 * services into a higher-level service method with transaction demarcation. As an
 * alternative, consider the use of declarative transaction demarcation (e.g. through
 * Spring's {@link org.springframework.transaction.annotation.Transactional} annotation).
 */
public interface TransactionCallback<T> {

	/**
	 * Gets called by {@link TransactionTemplate#execute} within a transactional context.
	 * Does not need to care about transactions itself, although it can retrieve and
	 * influence the status of the current transaction via the given status object,
	 * e.g. setting rollback-only.
	 * <p>Allows for returning a result object created within the transaction, i.e. a
	 * domain object or a collection of domain objects. A RuntimeException thrown by the
	 * callback is treated as application exception that enforces a rollback. Any such
	 * exception will be propagated to the caller of the template, unless there is a
	 * problem rolling back, in which case a TransactionException will be thrown.
	 * @param status associated transaction status
	 * @return a result object, or {@code null}
	 */
	T doInTransaction(TransactionStatus status);

}
