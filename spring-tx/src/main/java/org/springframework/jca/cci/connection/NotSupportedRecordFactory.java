package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

/**
 * Implementation of the CCI RecordFactory interface that always throws
 * NotSupportedException.
 *
 * <p>Useful as a placeholder for a RecordFactory argument (for example as
 * defined by the RecordCreator callback), in particular when the connector's
 * {@code ConnectionFactory.getRecordFactory()} implementation happens to
 * throw NotSupportedException early rather than throwing the exception from
 * RecordFactory's methods.
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