package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

/**
 * CCI RecordFactory接口的实现, 始终抛出NotSupportedException.
 *
 * <p>用作RecordFactory参数的占位符 (例如, 由RecordCreator回调定义),
 * 特别是当连接器的{@code ConnectionFactory.getRecordFactory()}实现恰好提前抛出NotSupportedException,
 * 而不是从RecordFactory的方法抛出异常时.
 */
public class NotSupportedRecordFactory implements RecordFactory {

	@Override
	public MappedRecord createMappedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

	@Override
	public IndexedRecord createIndexedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

}
