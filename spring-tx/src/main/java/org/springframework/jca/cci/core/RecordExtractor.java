package org.springframework.jca.cci.core;

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;

/**
 * 用于从CCI Record实例中提取结果对象的回调接口.
 *
 * <p>用于在CciTemplate中创建输出对象.
 * 或者, 输出记录也可以按原样返回到客户端代码.
 * 如果将CCI ResultSet作为执行结果, 几乎总是希望实现RecordExtractor,
 * 以便能够以托管方式读取ResultSet, 同时在读取ResultSet时CCI连接仍处于打开状态.
 *
 * <p>此接口的实现执行提取结果的实际工作, 但不需要担心异常处理.
 * CciTemplate类将正确捕获和处理ResourceException.
 */
public interface RecordExtractor<T> {

	/**
	 * 处理给定Record中的数据, 创建相应的结果对象.
	 * 
	 * @param record 从中提取数据的记录 (可能是CCI ResultSet)
	 * 
	 * @return 任意结果对象, 或{@code null} (在后一种情况下, 提取器通常是有状态的)
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 * @throws SQLException 如果由ResultSet方法抛出, 则自动转换为DataAccessException
	 * @throws DataAccessException 自定义异常
	 */
	T extractData(Record record) throws ResourceException, SQLException, DataAccessException;

}
