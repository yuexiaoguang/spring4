package org.springframework.orm.hibernate5;

import org.hibernate.QueryException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Hibernate-specific subclass of InvalidDataAccessResourceUsageException,
 * thrown on invalid HQL query syntax.
 */
@SuppressWarnings("serial")
public class HibernateQueryException extends InvalidDataAccessResourceUsageException {

	public HibernateQueryException(QueryException ex) {
		super(ex.getMessage(), ex);
	}

	/**
	 * Return the HQL query string that was invalid.
	 */
	public String getQueryString() {
		return ((QueryException) getCause()).getQueryString();
	}

}
