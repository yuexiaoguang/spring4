package org.springframework.jca.cci.core;

import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;

/**
 * 指定EIS上的一组基本CCI操作的接口.
 * 由CciTemplate实现. 不经常使用, 但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 *
 * <p>或者, 可以模拟标准CCI基础结构. 但是, 模拟此接口所构成的工作要少得多.
 */
public interface CciOperations {

	/**
	 * 使用CCI在EIS上执行请求, 实现为在CCI连接上进行的回调操作.
	 * 这允许在Spring的托管CCI环境中实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JCA ResourceExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象
	 * @throws DataAccessException
	 */
	<T> T execute(ConnectionCallback<T> action) throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行请求, 实现为处理CCI交互的回调操作.
	 * 这允许在Spring的托管CCI环境中对单个Interaction实现任意数据访问操作:
	 * 也就是说, 参与Spring管理的事务, 并将JCA ResourceExceptions转换为Spring的DataAccessException层次结构.
	 * <p>回调操作可以返回结果对象, 例如域对象或域对象的集合.
	 * 
	 * @param action 指定操作的回调对象
	 * 
	 * @return 操作返回的结果对象
	 * @throws DataAccessException
	 */
	<T> T execute(InteractionCallback<T> action) throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputRecord 输入记录
	 * 
	 * @return 输出记录
	 * @throws DataAccessException
	 */
	Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录
	 * 
	 * @throws DataAccessException
	 */
	void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputCreator 创建要使用的输入记录的对象
	 * 
	 * @return 输出记录
	 * @throws DataAccessException
	 */
	Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputRecord 输入记录
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * 
	 * @return 使用RecordExtractor对象提取的输出数据
	 * @throws DataAccessException
	 */
	<T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputCreator 创建要使用的输入记录的对象
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * 
	 * @return 使用RecordExtractor对象提取的输出数据
	 * @throws DataAccessException
	 */
	<T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

}
