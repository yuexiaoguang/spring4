package org.springframework.orm.hibernate5;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Hibernate的JTASessionContext的特定于Spring的子类, 为只读事务设置{@code FlushMode.MANUAL}.
 */
@SuppressWarnings("serial")
public class SpringJtaSessionContext extends JTASessionContext {

	public SpringJtaSessionContext(SessionFactoryImplementor factory) {
		super(factory);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected Session buildOrObtainSession() {
		Session session = super.buildOrObtainSession();
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
			session.setFlushMode(FlushMode.MANUAL);
		}
		return session;
	}
}
