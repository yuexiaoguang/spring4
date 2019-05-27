package org.springframework.orm.jdo;

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOFatalDataStoreException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * DataAccessResourceFailureException的JDO特定子类.
 * 转换JDO的JDODataStoreException和JDOFatalDataStoreException.
 */
@SuppressWarnings("serial")
public class JdoResourceFailureException extends DataAccessResourceFailureException {

	public JdoResourceFailureException(JDODataStoreException ex) {
		super(ex.getMessage(), ex);
	}

	public JdoResourceFailureException(JDOFatalDataStoreException ex) {
		super(ex.getMessage(), ex);
	}

}
