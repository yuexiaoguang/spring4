package org.springframework.jca.cci.object;

import java.io.IOException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jca.cci.core.support.CommAreaRecord;

/**
 * 用于访问COMMAREA记录的EIS操作对象.
 * 通用MappingRecordOperation类的子类.
 */
public abstract class MappingCommAreaOperation extends MappingRecordOperation {

	public MappingCommAreaOperation() {
	}

	/**
	 * @param connectionFactory 用于获取连接的ConnectionFactory
	 * @param interactionSpec 配置交互的规范
	 */
	public MappingCommAreaOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		super(connectionFactory, interactionSpec);
	}


	@Override
	protected final Record createInputRecord(RecordFactory recordFactory, Object inObject) {
		try {
			return new CommAreaRecord(objectToBytes(inObject));
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}

	@Override
	protected final Object extractOutputData(Record record) throws DataAccessException {
		CommAreaRecord commAreaRecord = (CommAreaRecord) record;
		try {
			return bytesToObject(commAreaRecord.toByteArray());
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}


	/**
	 * 用于将对象转换为COMMAREA字节的方法.
	 * 
	 * @param inObject 输入数据
	 * 
	 * @return COMMAREA的字节
	 * @throws IOException 如果由I/O方法抛出
	 * @throws DataAccessException 如果转换失败
	 */
	protected abstract byte[] objectToBytes(Object inObject) throws IOException, DataAccessException;

	/**
	 * 用于将COMMAREA的字节转换为对象的方法.
	 * 
	 * @param bytes COMMAREA的字节
	 * 
	 * @return 输出数据
	 * @throws IOException 如果由I/O方法抛出
	 * @throws DataAccessException 如果转换失败
	 */
	protected abstract Object bytesToObject(byte[] bytes) throws IOException, DataAccessException;

}
