package org.springframework.orm.hibernate5;

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
		SessionFactoryUtils.flush(this.session, false);
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
