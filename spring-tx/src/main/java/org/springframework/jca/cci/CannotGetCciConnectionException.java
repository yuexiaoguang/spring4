package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Fatal exception thrown when we can't connect to an EIS using CCI.
 */
@SuppressWarnings("serial")
public class CannotGetCciConnectionException extends DataAccessResourceFailureException {

	/**
	 * Constructor for CannotGetCciConnectionException.
	 * @param msg message
	 * @param ex ResourceException root cause
	 */
	public CannotGetCciConnectionException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
