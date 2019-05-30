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
 * 实现Hibernate 3.1的CurrentSessionContext接口, 该接口委托给Spring的SessionFactoryUtils以提供Spring管理的当前Session.
 *
 * <p>也可以通过"hibernate.current_session_context_class"属性在自定义SessionFactory设置中指定此CurrentSessionContext实现,
 * 并将此类的完全限定名称作为值.
 */
@SuppressWarnings("serial")
public class SpringSessionContext implements CurrentSessionContext {

	private final SessionFactoryImplementor sessionFactory;

	private TransactionManager transactionManager;

	private CurrentSessionContext jtaSessionContext;


	/**
	 * @param sessionFactory 提供当前Session的SessionFactory
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
	 * 检索当前线程的Spring管理的Session.
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
				// 切换到 FlushMode.AUTO, 因为必须假设一个线程绑定的Session与FlushMode.MANUAL, 它需要允许在事务中刷新.
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
