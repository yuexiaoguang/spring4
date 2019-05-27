package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * {@link RowMapper}实现, 为每一行创建一个{@code java.util.Map}, 将所有列表示为键值对:
 * 每列一个条目, 列名为键.
 *
 * <p>可以通过分别覆盖{@link #createColumnMap}和{@link #getColumnKey},
 * 来自定义要使用的Map实现和用于列Map中每列的键.
 *
 * <p><b>Note:</b> 默认情况下, ColumnMapRowMapper将尝试使用不区分大小写的键构建linked Map,
 * 以保留列顺序以及允许任何大小写用于列名.
 * 这需要类路径上的Commons Collections (将被自动检测).
 * 否则, 后备是一个标准的linked HashMap, 它仍然会保留列顺序, 但要求应用程序在驱动程序公开的相同大小写中指定列名.
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {

	@Override
	public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			String column = JdbcUtils.lookupColumnName(rsmd, i);
			mapOfColumnValues.put(getColumnKey(column), getColumnValue(rs, i));
		}
		return mapOfColumnValues;
	}

	/**
	 * 创建要用作列映射的Map实例.
	 * <p>默认情况下, 将创建一个链接的不区分大小写的Map.
	 * 
	 * @param columnCount 列数, 用作Map的初始容量
	 * 
	 * @return 新的Map实例
	 */
	protected Map<String, Object> createColumnMap(int columnCount) {
		return new LinkedCaseInsensitiveMap<Object>(columnCount);
	}

	/**
	 * 确定要用于列Map中给定列的键.
	 * 
	 * @param columnName ResultSet返回的列名
	 * 
	 * @return 要使用的列键
	 */
	protected String getColumnKey(String columnName) {
		return columnName;
	}

	/**
	 * 检索指定列的JDBC对象值.
	 * <p>默认实现使用{@code getObject}方法.
	 * 此外, 此实现还包括一个"hack"来绕过Oracle返回其TIMESTAMP数据类型的非标准对象.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * 
	 * @return 返回的Object
	 */
	protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}
}
