package org.springframework.orm.hibernate4.support;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.hibernate4.HibernateTemplate;

/**
 * Convenient super class for Hibernate-based data access objects.
 *
 * <p>Requires a {@link org.hibernate.SessionFactory} to be set, providing a
 * {@link org.springframework.orm.hibernate4.HibernateTemplate} based on it to
 * subclasses through the {@link #getHibernateTemplate()} method.
 * Can alternatively be initialized directly with a HibernateTemplate,
 * in order to reuse the latter's settings such as the SessionFactory,
 * exception translator, flush mode, etc.
 *
 * <p>This class will create its own HibernateTemplate instance if a SessionFactory
 * is passed in. The "allowCreate" flag on that HibernateTemplate will be "true"
 * by default. A custom HibernateTemplate instance can be used through overriding
 * {@link #createHibernateTemplate}.
 *
 * <p><b>NOTE: Hibernate access code can also be coded in plain Hibernate style.
 * Hence, for newly started projects, consider adopting the standard Hibernate
 * style of coding data access objects instead, based on
 * {@link org.hibernate.SessionFactory#getCurrentSession()}.
 * This HibernateTemplate primarily exists as a migration helper for Hibernate 3
 * based data access code, to benefit from bug fixes in Hibernate 4.x.</b>
 */
public abstract class HibernateDaoSupport extends DaoSupport {

	private HibernateTemplate hibernateTemplate;


	/**
	 * Set the Hibernate SessionFactory to be used by this DAO.
	 * Will automatically create a HibernateTemplate for the given SessionFactory.
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
			this.hibernateTemplate = createHibernateTemplate(sessionFactory);
		}
	}

	/**
	 * Create a HibernateTemplate for the given SessionFactory.
	 * Only invoked if populating the DAO with a SessionFactory reference!
	 * <p>Can be overridden in subclasses to provide a HibernateTemplate instance
	 * with different configuration, or a custom HibernateTemplate subclass.
	 * @param sessionFactory the Hibernate SessionFactory to create a HibernateTemplate for
	 * @return the new HibernateTemplate instance
	 * @see #setSessionFactory
	 */
	protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
		return new HibernateTemplate(sessionFactory);
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * Set the HibernateTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 * @see #setSessionFactory
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Return the HibernateTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 * <p><b>Note: The returned HibernateTemplate is a shared instance.</b>
	 * You may introspect its configuration, but not modify the configuration
	 * (other than from within an {@link #initDao} implementation).
	 * Consider creating a custom HibernateTemplate instance via
	 * {@code new HibernateTemplate(getSessionFactory())}, in which case
	 * you're allowed to customize the settings on the resulting instance.
	 */
	public final HibernateTemplate getHibernateTemplate() {
	  return this.hibernateTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("'sessionFactory' or 'hibernateTemplate' is required");
		}
	}


	/**
	 * Conveniently obtain the current Hibernate Session.
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @see org.hibernate.SessionFactory#getCurrentSession()
	 */
	protected final Session currentSession() throws DataAccessResourceFailureException {
		return getSessionFactory().getCurrentSession();
	}

}
