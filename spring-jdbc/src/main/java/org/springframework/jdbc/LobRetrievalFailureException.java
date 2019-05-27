package org.springframework.jdbc;

import java.io.IOException;

import org.springframework.dao.DataRetrievalFailureException;

/**
 * 无法检索LOB时抛出的异常.
 */
@SuppressWarnings("serial")
public class LobRetrievalFailureException extends DataRetrievalFailureException {

	public LobRetrievalFailureException(String msg) {
		super(msg);
	}

	public LobRetrievalFailureException(String msg, IOException ex) {
		super(msg, ex);
	}

}
