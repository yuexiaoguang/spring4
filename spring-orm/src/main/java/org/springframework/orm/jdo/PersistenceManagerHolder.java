package org.springframework.orm.jdo;

import javax.jdo.PersistenceManager;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * 包装JDO PersistenceManager的Holder.
 * 对于给定的PersistenceManagerFactory, JdoTransactionManager将此类的实例绑定到线程.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class PersistenceManagerHolder extends ResourceHolderSupport {

	private final PersistenceManager persistenceManager;

	private boolean transactionActive;


	public PersistenceManagerHolder(PersistenceManager persistenceManager) {
		Assert.notNull(persistenceManager, "PersistenceManager must not be null");
		this.persistenceManager = persistenceManager;
	}


	public PersistenceManager getPersistenceManager() {
		return this.persistenceManager;
	}

	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}

}
