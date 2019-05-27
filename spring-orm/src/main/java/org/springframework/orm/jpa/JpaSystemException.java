package org.springframework.orm.jpa;

import javax.persistence.PersistenceException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 特定于JPA的子类UncategorizedDataAccessException,
 * 用于与任何具体{@code org.springframework.dao}异常不匹配的JPA系统错误.
 */
@SuppressWarnings("serial")
public class JpaSystemException extends UncategorizedDataAccessException {

	@Deprecated
	public JpaSystemException(PersistenceException ex) {
		super(ex.getMessage(), ex);
	}

	public JpaSystemException(RuntimeException ex) {
		super(ex.getMessage(), ex);
	}

}
