package org.springframework.orm.jdo;

import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOUserException;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * InvalidDataAccessApiUsageException的JDO特定子类.
 * 转换JDO的JDOUserException和JDOFatalUserException.
 */
@SuppressWarnings("serial")
public class JdoUsageException extends InvalidDataAccessApiUsageException {

	public JdoUsageException(JDOUserException ex) {
		super(ex.getMessage(), ex);
	}

	public JdoUsageException(JDOFatalUserException ex) {
		super(ex.getMessage(), ex);
	}

}
