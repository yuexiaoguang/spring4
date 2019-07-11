package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;

/**
 * EIS操作对象, 接受传入的CCI输入记录并返回相应的CCI输出记录.
 */
public class SimpleRecordOperation extends EisOperation {

	public SimpleRecordOperation() {
	}

	/**
	 * 使用ConnectionFactory和规范 (连接和交互).
	 * 
	 * @param connectionFactory 用于获取连接的ConnectionFactory
	 */
	public SimpleRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}

	/**
	 * 执行此操作对象封装的CCI交互.
	 * <p>此方法将调用CCI的{@code Interaction.execute}变体, 该变体返回输出记录.
	 * 
	 * @param inputRecord 输入记录
	 * 
	 * @return 输出记录
	 * @throws DataAccessException
	 */
	public Record execute(Record inputRecord) throws DataAccessException {
		return getCciTemplate().execute(getInteractionSpec(), inputRecord);
	}

	/**
	 * 执行此操作对象封装的CCI交互.
	 * <p>此方法将使用传入的输出Record调用CCI的{@code Interaction.execute}变体.
	 * 
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录
	 * 
	 * @throws DataAccessException
	 */
	public void execute(Record inputRecord, Record outputRecord) throws DataAccessException {
		getCciTemplate().execute(getInteractionSpec(), inputRecord, outputRecord);
	}

}
