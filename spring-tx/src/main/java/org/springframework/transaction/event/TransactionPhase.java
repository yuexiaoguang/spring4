package org.springframework.transaction.event;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * The phase at which a transactional event listener applies.
 */
public enum TransactionPhase {

	/**
	 * Fire the event before transaction commit.
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	BEFORE_COMMIT,

	/**
	 * Fire the event after the commit has completed successfully.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and
	 * therefore executes in the same after-completion sequence of events,
	 * (and not in {@link TransactionSynchronization#afterCommit()}).
	 */
	AFTER_COMMIT,

	/**
	 * Fire the event if the transaction has rolled back.
	 * <p>Note: This is a specialization of {@link #AFTER_COMPLETION} and
	 * therefore executes in the same after-completion sequence of events.
	 */
	AFTER_ROLLBACK,

	/**
	 * Fire the event after the transaction has completed.
	 * <p>For more fine-grained events, use {@link #AFTER_COMMIT} or
	 * {@link #AFTER_ROLLBACK} to intercept transaction commit
	 * or rollback, respectively.
	 * @see TransactionSynchronization#afterCompletion(int)
	 */
	AFTER_COMPLETION

}
