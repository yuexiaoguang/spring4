package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}用于以行为单位映射{@link java.sql.ResultSet}的行的接口.
 * 此接口的实现执行将每行映射到结果对象的实际工作, 但不需要担心异常处理.
 * {@link java.sql.SQLException SQLExceptions}将被调用JdbcTemplate捕获并处理.
 *
 * <p>通常用于{@link JdbcTemplate}的查询方法或存储过程的out参数.
 * RowMapper 对象通常是无状态的, 因此可以重用; 它们是在一个地方实现行映射逻辑的理想选择.
 *
 * <p>或者, 考虑从{@code jdbc.object}包中继承{@link org.springframework.jdbc.object.MappingSqlQuery}:
 * 可以使用该样式构建可执行查询对象(包含行映射逻辑), 而不是使用单独的JdbcTemplate和RowMapper对象.
 */
public interface RowMapper<T> {

	/**
	 * 实现必须实现此方法以映射ResultSet中的每一行数据.
	 * 此方法不应在ResultSet上调用{@code next()}; 它只应映射当前行的值.
	 * 
	 * @param rs 要映射的ResultSet (为当前行预先初始化)
	 * @param rowNum 当前行的编号
	 * 
	 * @return 当前行的结果对象 (may be {@code null})
	 * @throws SQLException 如果获取列值时, 遇到SQLException (也就是说, 不需要捕获SQLException)
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
