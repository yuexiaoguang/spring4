package org.springframework.orm.hibernate3;

import org.hibernate.QueryException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Hibernate-specific subclass of InvalidDataAccessResourceUsageException,
 * thrown on invalid HQL query syntax.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
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
