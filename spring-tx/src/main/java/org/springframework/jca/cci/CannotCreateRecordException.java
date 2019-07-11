package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * 由于连接器内部原因, 创建CCI记录失败时抛出的异常.
 */
@SuppressWarnings("serial")
public class CannotCreateRecordException extends DataAccessResourceFailureException {

	public CannotCreateRecordException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
