package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * ResultSetExtractor接口的适配器实现, 该接口委托给RowMapper, 该RowMapper应该为每一行创建一个对象.
 * 每个对象都添加到此ResultSetExtractor的结果列表中.
 *
 * <p>对于数据库表中每行一个对象的典型情况很有用.
 * 结果列表中的条目数将与行数匹配.
 *
 * <p>请注意, RowMapper对象通常是无状态的, 因此可以重用; 只是RowMapperResultSetExtractor适配器是有状态的.
 *
 * <p>JdbcTemplate的用法示例:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowMapper rowMapper = new UserRowMapper();  // reusable object
 *
 * List allUsers = (List) jdbcTemplate.query(
 *     "select * from user",
 *     new RowMapperResultSetExtractor(rowMapper, 10));
 *
 * User user = (User) jdbcTemplate.queryForObject(
 *     "select * from user where id=?", new Object[] {id},
 *     new RowMapperResultSetExtractor(rowMapper, 1));</pre>
 *
 * <p>或者, 考虑从{@code jdbc.object}包中继承MappingSqlQuery:
 * 可以拥有可执行查询对象(包含行映射逻辑), 而不是使用单独的JdbcTemplate和RowMapper对象.
 */
public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

	private final RowMapper<T> rowMapper;

	private final int rowsExpected;


	/**
	 * @param rowMapper 为每一行创建一个对象的RowMapper
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
		this(rowMapper, 0);
	}

	/**
	 * @param rowMapper 为每一行创建一个对象的RowMapper
	 * @param rowsExpected 预期行数 (仅用于优化集合处理)
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
		Assert.notNull(rowMapper, "RowMapper is required");
		this.rowMapper = rowMapper;
		this.rowsExpected = rowsExpected;
	}


	@Override
	public List<T> extractData(ResultSet rs) throws SQLException {
		List<T> results = (this.rowsExpected > 0 ? new ArrayList<T>(this.rowsExpected) : new ArrayList<T>());
		int rowNum = 0;
		while (rs.next()) {
			results.add(this.rowMapper.mapRow(rs, rowNum++));
		}
		return results;
	}
}
