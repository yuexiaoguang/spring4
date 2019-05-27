package org.springframework.orm.hibernate3;

import org.hibernate.HibernateException;
import org.hibernate.classic.Session;
import org.hibernate.context.CurrentSessionContext;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Implementation of Hibernate 3.1's CurrentSessionContext interface
 * that delegates to Spring's SessionFactoryUtils for providing a
 * Spring-managed current Session.
 *
 * <p>Used by Spring's {@link LocalSessionFactoryBean} when told to expose a
 * transaction-aware SessionFactory. This is the default as of Spring 2.5.
 *
 * <p>This CurrentSessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class SpringSessionContext implements CurrentSessionContext {

	private final SessionFactoryImplementor sessionFactory;


	/**
	 * Create a new SpringSessionContext for the given Hibernate SessionFactory.
	 * @param sessionFactory the SessionFactory to provide current Sessions for
	 */
	public SpringSessionContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}


	/**
	 * Retrieve the Spring-managed Session for the current thread, if any.
	 */
	@Override
	public Session currentSession() throws HibernateException {
		try {
			return (org.hibernate.classic.Session) SessionFactoryUtils.doGetSession(this.sessionFactory, false);
		}
		catch (IllegalStateException ex) {
			throw new HibernateException(ex.getMessage());
		}
	}

}
