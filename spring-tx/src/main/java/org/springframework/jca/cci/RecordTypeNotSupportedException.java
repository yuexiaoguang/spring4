package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 由于连接器不支持所需的CCI记录类型, 因此创建CCI记录失败时抛出异常.
 */
@SuppressWarnings("serial")
public class RecordTypeNotSupportedException extends InvalidDataAccessResourceUsageException {

	public RecordTypeNotSupportedException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
