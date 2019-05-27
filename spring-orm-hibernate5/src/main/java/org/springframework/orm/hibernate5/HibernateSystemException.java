package org.springframework.orm.hibernate5;

import org.hibernate.HibernateException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Hibernate-specific subclass of UncategorizedDataAccessException,
 * for Hibernate system errors that do not match any concrete
 * {@code org.springframework.dao} exceptions.
 */
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
