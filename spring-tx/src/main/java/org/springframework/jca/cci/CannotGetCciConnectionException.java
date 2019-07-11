package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * 当无法使用CCI连接到EIS时抛出的致命异常.
 */
@SuppressWarnings("serial")
public class CannotGetCciConnectionException extends DataAccessResourceFailureException {

	public CannotGetCciConnectionException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
