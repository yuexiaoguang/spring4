package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 连接器不支持特定CCI操作时抛出的异常.
 */
@SuppressWarnings("serial")
public class CciOperationNotSupportedException extends InvalidDataAccessResourceUsageException {

	public CciOperationNotSupportedException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
