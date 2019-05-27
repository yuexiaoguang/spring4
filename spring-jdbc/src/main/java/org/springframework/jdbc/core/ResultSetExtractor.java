package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * {@link JdbcTemplate}的查询方法使用的回调接口.
 * 此接口的实现执行从{@link java.sql.ResultSet}提取结果的实际工作, 但不需要担心异常处理.
 * {@link java.sql.SQLException SQLExceptions}将由调用JdbcTemplate捕获并处理.
 *
 * <p>该接口主要用于JDBC框架本身.
 * {@link RowMapper}通常是ResultSet处理的一个更简单的选择, 每行映射一个结果对象, 而不是整个ResultSet的一个结果对象.
 *
 * <p>Note: 与{@link RowCallbackHandler}相反, ResultSetExtractor对象通常是无状态的, 因此可重用,
 * 只要它不访问有状态资源 (例如流式传输LOB内容时的输出流), 或将结果状态保持在对象内.
 */
public interface ResultSetExtractor<T> {

	/**
	 * 实现必须实现此方法来处理整个ResultSet.
	 * 
	 * @param rs 从中提取数据的ResultSet. 实现不应该关闭它: 它将被调用JdbcTemplate关闭.
	 * 
	 * @return 任意结果对象, 或{@code null} (在后一种情况下, 提取器通常是有状态的).
	 * @throws SQLException 如果获取列值或导航时遇到SQLException (也就是说, 不需要捕获SQLException)
	 * @throws DataAccessException 如果是自定义异常
	 */
	T extractData(ResultSet rs) throws SQLException, DataAccessException;

}
