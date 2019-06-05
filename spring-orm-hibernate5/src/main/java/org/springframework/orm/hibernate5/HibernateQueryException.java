package org.springframework.orm.hibernate5;

import org.hibernate.QueryException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * InvalidDataAccessResourceUsageException的特定于Hibernate的子类, 抛出无效的HQL查询语法.
 */
@SuppressWarnings("serial")
public class HibernateQueryException extends InvalidDataAccessResourceUsageException {

	public HibernateQueryException(QueryException ex) {
		super(ex.getMessage(), ex);
	}

	/**
	 * 返回无效的HQL查询字符串.
	 */
	public String getQueryString() {
		return ((QueryException) getCause()).getQueryString();
	}

}
