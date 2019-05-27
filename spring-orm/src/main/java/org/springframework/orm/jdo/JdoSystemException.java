package org.springframework.orm.jdo;

import javax.jdo.JDOException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * JDO特定的UncategorizedDataAccessException子类,
 * 用于与任何具体{@code org.springframework.dao}异常不匹配的JDO系统错误.
 */
@SuppressWarnings("serial")
public class JdoSystemException extends UncategorizedDataAccessException {

	public JdoSystemException(JDOException ex) {
		super(ex.getMessage(), ex);
	}

}
