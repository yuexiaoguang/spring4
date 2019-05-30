package org.springframework.orm.hibernate4;

import org.hibernate.HibernateException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 特定于Hibernate的UncategorizedDataAccessException子类,
 * 用于与任何具体{@code org.springframework.dao}异常不匹配的Hibernate系统错误.
 */
@SuppressWarnings("serial")
public class HibernateSystemException extends UncategorizedDataAccessException {

	public HibernateSystemException(HibernateException cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
