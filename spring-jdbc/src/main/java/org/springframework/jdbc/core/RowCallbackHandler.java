package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}使用的接口, 用于按行处理{@link java.sql.ResultSet}的行.
 * 此接口的实现执行处理每一行的实际工作, 但不必担心异常处理.
 * {@link java.sql.SQLException SQLExceptions}将被调用JdbcTemplate捕获并处理.
 *
 * <p>与{@link ResultSetExtractor}相反, RowCallbackHandler对象通常是有状态的:
 * 它将结果状态保持在对象中, 以供稍后检查.
 * 有关用法示例, 请参阅{@link RowCountCallbackHandler}.
 *
 * <p>如果需要每行精确映射一个结果对象, 将它们组装到List中, 请考虑使用{@link RowMapper}.
 */
public interface RowCallbackHandler {

	/**
	 * 实现必须实现此方法来处理ResultSet中的每一行数据.
	 * 此方法不应在ResultSet上调用{@code next()}; 它只应该提取当前行的值.
	 * <p>具体实现的选择取决于它:
	 * 一个简单的实现可能只是计算行, 而另一个实现可能会构建一个XML文档.
	 * 
	 * @param rs 要处理的ResultSet (为当前行预先初始化)
	 * 
	 * @throws SQLException 如果获取列值时, 遇到SQLException (也就是说, 不需要捕获SQLException)
	 */
	void processRow(ResultSet rs) throws SQLException;

}
