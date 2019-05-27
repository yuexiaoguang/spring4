package org.springframework.orm.hibernate4;

import java.lang.reflect.Method;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

/**
 * Implementation of Hibernate 3.1's CurrentSessionContext interface
 * that delegates to Spring's SessionFactoryUtils for providing a
 * Spring-managed current Session.
 *
 * <p>This CurrentSessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 */
@SuppressWarnings("serial")
public class SpringSessionContext implements CurrentSessionContext {

	private final SessionFactoryImplementor sessionFactory;

	private TransactionManager transactionManager;

	private CurrentSessionContext jtaSessionContext;


	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public SpringSessionContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		try {
			Object jtaPlatform = sessionFactory.getServiceRegistry().getService(ConfigurableJtaPlatform.jtaPlatformClass);
			Method rtmMethod = ConfigurableJtaPlatform.jtaPlatformClass.getMethod("retrieveTransactionManager");
			this.transactionManager = (TransactionManager) ReflectionUtils.invokeMethod(rtmMethod, jtaPlatform);
			if (this.transactionManager != null) {
				this.jtaSessionContext = new SpringJtaSessionContext(sessionFactory);
			}
		}
		catch (Exception ex) {
			LogFactory.getLog(SpringSessionContext.class).warn(
					"Could not introspect Hibernate JtaPlatform for SpringJtaSessionContext", ex);
		}
	}


	/**
	 * Retrieve the Spring-managed Session for the current thread, if any.
	 */
	@Override
	public Session currentSession() throws HibernateException {
		Object value = TransactionSynchronizationManager.getResource(this.sessionFactory);
		if (value instanceof Session) {
			return (Session) value;
		}
		else if (value instanceof SessionHolder) {
			SessionHolder sessionHolder = (SessionHolder) value;
			Session session = sessionHolder.getSession();
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(
						new SpringSessionSynchronization(sessionHolder, this.sessionFactory, false));
				sessionHolder.setSynchronizedWithTransaction(true);
				// Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
				// with FlushMode.MANUAL, which needs to allow flushing within the transaction.
				FlushMode flushMode = session.getFlushMode();
				if (flushMode.equals(FlushMode.MANUAL) &&
						!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					session.setFlushMode(FlushMode.AUTO);
					sessionHolder.setPreviousFlushMode(flushMode);
				}
			}
			return session;
		}

		if (this.transactionManager != null) {
			try {
				if (this.transactionManager.getStatus() == Status.STATUS_ACTIVE) {
					Session session = this.jtaSessionContext.currentSession();
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.registerSynchronization(new SpringFlushSynchronization(session));
					}
					return session;
				}
			}
			catch (SystemException ex) {
				throw new HibernateException("JTA TransactionManager found but status check failed", ex);
			}
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			Session session = this.sessionFactory.openSession();
			if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				session.setFlushMode(FlushMode.MANUAL);
			}
			SessionHolder sessionHolder = new SessionHolder(session);
			TransactionSynchronizationManager.registerSynchronization(
					new SpringSessionSynchronization(sessionHolder, this.sessionFactory, true));
			TransactionSynchronizationManager.bindResource(this.sessionFactory, sessionHolder);
			sessionHolder.setSynchronizedWithTransaction(true);
			return session;
		}
		else {
			throw new HibernateException("Could not obtain transaction-synchronized Session for current thread");
		}
	}

}
