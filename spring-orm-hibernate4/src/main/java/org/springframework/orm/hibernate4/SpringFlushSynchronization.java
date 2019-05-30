package org.springframework.orm.hibernate4;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * 简单的同步适配器, 它将{@code flush()}调用传播到底层的Hibernate会话. 与JTA结合使用.
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
