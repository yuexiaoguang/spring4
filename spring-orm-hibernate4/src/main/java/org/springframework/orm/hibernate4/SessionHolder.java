package org.springframework.orm.hibernate4;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * 会话持有者, 包装Hibernate会话和Hibernate事务.
 * 对于给定的SessionFactory, HibernateTransactionManager将此类的实例绑定到线程.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
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
