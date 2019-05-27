package org.springframework.orm.hibernate4;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Session holder, wrapping a Hibernate Session and a Hibernate Transaction.
 * HibernateTransactionManager binds instances of this class to the thread,
 * for a given SessionFactory.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 */
public class SessionHolder extends ResourceHolderSupport {

	private Session session;

	private Transaction transaction;

	private FlushMode previousFlushMode;


	public SessionHolder(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	public Session getSession() {
		return this.session;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public Transaction getTransaction() {
		return this.transaction;
	}

	public void setPreviousFlushMode(FlushMode previousFlushMode) {
		this.previousFlushMode = previousFlushMode;
	}

	public FlushMode getPreviousFlushMode() {
		return this.previousFlushMode;
	}


	@Override
	public void clear() {
		super.clear();
		this.transaction = null;
		this.previousFlushMode = null;
	}

}
