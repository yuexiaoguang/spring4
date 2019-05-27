package org.springframework.jdbc.support.lob;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Helper类, 用于注册事务同步以关闭LobCreator, 更喜欢Spring事务同步, 并回退到普通JTA事务同步.
 */
public abstract class LobCreatorUtils {

	private static final Log logger = LogFactory.getLog(LobCreatorUtils.class);


	/**
	 * 注册事务同步以关闭给定的LobCreator, 更喜欢Spring事务同步, 并回退到普通的JTA事务同步.
	 * 
	 * @param lobCreator 在事务完成后关闭的LobCreator
	 * @param jtaTransactionManager 当没有Spring事务同步可用时, 可以回退的JTA TransactionManager (may be {@code null})
	 * 
	 * @throws IllegalStateException 如果既没有活动的Spring事务同步, 也没有活动的JTA事务同步
	 */
	public static void registerTransactionSynchronization(
			LobCreator lobCreator, TransactionManager jtaTransactionManager) throws IllegalStateException {

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering Spring transaction synchronization for LobCreator");
			TransactionSynchronizationManager.registerSynchronization(
				new SpringLobCreatorSynchronization(lobCreator));
		}
		else {
			if (jtaTransactionManager != null) {
				try {
					int jtaStatus = jtaTransactionManager.getStatus();
					if (jtaStatus == Status.STATUS_ACTIVE || jtaStatus == Status.STATUS_MARKED_ROLLBACK) {
						logger.debug("Registering JTA transaction synchronization for LobCreator");
						jtaTransactionManager.getTransaction().registerSynchronization(
								new JtaLobCreatorSynchronization(lobCreator));
						return;
					}
				}
				catch (Throwable ex) {
					throw new TransactionSystemException(
							"Could not register synchronization with JTA TransactionManager", ex);
				}
			}
			throw new IllegalStateException("Active Spring transaction synchronization or active " +
				"JTA transaction with specified [javax.transaction.TransactionManager] required");
		}
	}

}
