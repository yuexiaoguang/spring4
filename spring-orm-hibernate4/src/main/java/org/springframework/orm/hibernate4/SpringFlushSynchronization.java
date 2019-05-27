package org.springframework.orm.hibernate4;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * Simple synchronization adapter that propagates a {@code flush()} call
 * to the underlying Hibernate Session. Used in combination with JTA.
 */
public class SpringFlushSynchronization extends TransactionSynchronizationAdapter {

	private final Session session;


	public SpringFlushSynchronization(Session session) {
		this.session = session;
	}


	@Override
	public void flush() {
		try {
			SessionFactoryUtils.logger.debug("Flushing Hibernate Session on explicit request");
			this.session.flush();
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}


	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SpringFlushSynchronization &&
				this.session == ((SpringFlushSynchronization) obj).session);
	}

	@Override
	public int hashCode() {
		return this.session.hashCode();
	}

}
