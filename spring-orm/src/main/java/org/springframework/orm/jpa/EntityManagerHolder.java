package org.springframework.orm.jpa;

import javax.persistence.EntityManager;

import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * 包装JPA EntityManager的保存器.
 * 对于给定的EntityManagerFactory, JpaTransactionManager将此类的实例绑定到线程.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class EntityManagerHolder extends ResourceHolderSupport {

	private final EntityManager entityManager;

	private boolean transactionActive;

	private SavepointManager savepointManager;


	public EntityManagerHolder(EntityManager entityManager) {
		Assert.notNull(entityManager, "EntityManager must not be null");
		this.entityManager = entityManager;
	}


	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	protected void setSavepointManager(SavepointManager savepointManager) {
		this.savepointManager = savepointManager;
	}

	protected SavepointManager getSavepointManager() {
		return this.savepointManager;
	}

	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
		this.savepointManager = null;
	}

}
