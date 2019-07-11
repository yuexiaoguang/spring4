package org.springframework.jca.cci.object;

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.core.RecordExtractor;

/**
 * EIS操作对象, 需要映射输入和输出对象, 转换为CCI记录和从CCI记录转换.
 *
 * <p>具体的子类必须实现抽象的{@code createInputRecord(RecordFactory, Object)}和{@code extractOutputData(Record)}方法,
 * 从对象创建输入Record, 以及将输出Record转换为对象.
 */
public abstract class MappingRecordOperation extends EisOperation {

	public MappingRecordOperation() {
	}

	/**
	 * 使用ConnectionFactory和规范 (连接和交互).
	 * 
	 * @param connectionFactory 用于获取连接的ConnectionFactory
	 */
	public MappingRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}

	/**
	 * 设置应该用于创建默认输出记录的RecordCreator.
	 * <p>默认无: 将调用返回输出记录的CCI的{@code Interaction.execute}变体.
	 * <p>如果总是需要使用传入的输出记录调用CCI的{@code Interaction.execute}变体, 请在此处指定RecordCreator.
	 * 然后将调用此RecordCreator来创建默认输出Record实例.
	 */
	public void setOutputRecordCreator(RecordCreator creator) {
		getCciTemplate().setOutputRecordCreator(creator);
	}

	/**
	 * 执行此操作对象封装的交互.
	 * 
	 * @param inputObject 输入数据, 由{@code createInputRecord}方法转换为Record
	 * 
	 * @return 使用{@code extractOutputData}方法提取的输出数据
	 * @throws DataAccessException
	 */
	public Object execute(Object inputObject) throws DataAccessException {
		return getCciTemplate().execute(
				getInteractionSpec(), new RecordCreatorImpl(inputObject), new RecordExtractorImpl());
	}


	/**
	 * 子类必须实现此方法以从传递到{@code execute}方法的输入对象生成输入记录.
	 * 
	 * @param inputObject 传入的输入对象
	 * 
	 * @return CCI输入Record
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 */
	protected abstract Record createInputRecord(RecordFactory recordFactory, Object inputObject)
			throws ResourceException, DataAccessException;

	/**
	 * 子类必须实现此方法, 以将CCI执行返回的Record转换为{@code execute}方法的结果对象.
	 * 
	 * @param outputRecord CCI执行返回的Record
	 * 
	 * @return 结果对象
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 */
	protected abstract Object extractOutputData(Record outputRecord)
			throws ResourceException, SQLException, DataAccessException;


	/**
	 * RecordCreator的实现, 它调用封闭类的{@code createInputRecord}方法.
	 */
	protected class RecordCreatorImpl implements RecordCreator {

		private final Object inputObject;

		public RecordCreatorImpl(Object inObject) {
			this.inputObject = inObject;
		}

		@Override
		public Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException {
			return createInputRecord(recordFactory, this.inputObject);
		}
	}


	/**
	 * RecordExtractor的实现, 它调用封闭类的{@code extractOutputData}方法.
	 */
	protected class RecordExtractorImpl implements RecordExtractor<Object> {

		@Override
		public Object extractData(Record record) throws ResourceException, SQLException, DataAccessException {
			return extractOutputData(record);
		}
	}
}
