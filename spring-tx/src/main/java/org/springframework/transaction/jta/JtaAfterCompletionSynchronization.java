package org.springframework.transaction.jta;

import java.util.List;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * 适用于JTA同步的适配器, 在外部JTA事务完成后, 调用Spring {@link TransactionSynchronization}对象的
 * {@code afterCommit} / {@code afterCompletion}回调.
 * 在参与现有(非Spring)JTA事务时应用.
 */
public class JtaAfterCompletionSynchronization implements Synchronization {

	private final List<TransactionSynchronization> synchronizations;


	/**
	 * @param synchronizations TransactionSynchronization对象
	 */
	public JtaAfterCompletionSynchronization(List<TransactionSynchronization> synchronizations) {
		this.synchronizations = synchronizations;
	}


	@Override
	public void beforeCompletion() {
	}

	@Override
	public void afterCompletion(int status) {
		switch (status) {
			case Status.STATUS_COMMITTED:
				try {
					TransactionSynchronizationUtils.invokeAfterCommit(this.synchronizations);
				}
				finally {
					TransactionSynchronizationUtils.invokeAfterCompletion(
							this.synchronizations, TransactionSynchronization.STATUS_COMMITTED);
				}
				break;
			case Status.STATUS_ROLLEDBACK:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_ROLLED_BACK);
				break;
			default:
				TransactionSynchronizationUtils.invokeAfterCompletion(
						this.synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
		}
	}
}
