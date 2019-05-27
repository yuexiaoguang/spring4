package org.springframework.orm.hibernate3;

import org.hibernate.HibernateException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Hibernate-specific subclass of UncategorizedDataAccessException,
 * for Hibernate system errors that do not match any concrete
 * {@code org.springframework.dao} exceptions.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class HibernateSystemException extends UncategorizedDataAccessException {

	/**
	 * Create a new HibernateSystemException,
	 * wrapping an arbitrary HibernateException.
	 * @param cause the HibernateException thrown
	 */
	public HibernateSystemException(HibernateException cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
