package org.springframework.jdbc.support.rowset;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.lang.UsesJava7;

/**
 * Spring的{@link SqlRowSet}接口的默认实现, 包装{@link java.sql.ResultSet},
 * 捕获任何{@link SQLException}并将它们转换为相应的Spring {@link InvalidResultSetAccessException}.
 *
 * <p>如果SqlRowSet应该以断开连接的方式使用, 则传入的ResultSet应该已经断开连接.
 * 这意味着通常会传入{@code javax.sql.rowset.CachedRowSet}, 它实现了ResultSet接口.
 *
 * <p>Note: 从JDBC 4.0开始, 已经阐明任何使用String来标识列的方法都应该使用列标签.
 * 使用SQL查询字符串中的ALIAS关键字分配列标签. 当查询不使用ALIAS时, 默认标签是列名.
 * 大多数JDBC ResultSet实现都遵循这种新模式, 但有一些例外, 例如{@code com.sun.rowset.CachedRowSetImpl}类, 它只使用列名, 忽略任何列标签.
 * 从Spring 3.0.5开始, ResultSetWrappingSqlRowSet会将列标签转换为正确的列索引,
 * 以便为{@code com.sun.rowset.CachedRowSetImpl}提供更好的支持,
 * 这是使用RowSets时{@link org.springframework.jdbc.core.JdbcTemplate}使用的默认实现.
 *
 * <p>Note: 此类通过SqlRowSet接口实现{@code java.io.Serializable}标记接口,
 * 但如果其中包含的断开连接的ResultSet/RowSet是可序列化的, 则实际上只能序列化.
 * 大多数CachedRowSet实现实际上是可序列化的, 因此通常应该可行.
 */
public class ResultSetWrappingSqlRowSet implements SqlRowSet {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -4688694393146734764L;


	private final ResultSet resultSet;

	private final SqlRowSetMetaData rowSetMetaData;

	private final Map<String, Integer> columnLabelMap;


	/**
	 * @param resultSet 要包装的断开连接的ResultSet (通常是{@code javax.sql.rowset.CachedRowSet})
	 * 
	 * @throws InvalidResultSetAccessException 如果提取ResultSetMetaData失败
	 */
	public ResultSetWrappingSqlRowSet(ResultSet resultSet) throws InvalidResultSetAccessException {
		this.resultSet = resultSet;
		try {
			this.rowSetMetaData = new ResultSetWrappingSqlRowSetMetaData(resultSet.getMetaData());
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
		try {
			ResultSetMetaData rsmd = resultSet.getMetaData();
			if (rsmd != null) {
				int columnCount = rsmd.getColumnCount();
				this.columnLabelMap = new HashMap<String, Integer>(columnCount);
				for (int i = 1; i <= columnCount; i++) {
					String key = rsmd.getColumnLabel(i);
					// 确保为任何给定名称保留第一个匹配列, 如ResultSet的类型级别javadoc中所定义 (第81到83行).
					if (!this.columnLabelMap.containsKey(key)) {
						this.columnLabelMap.put(key, i);
					}
				}
			}
			else {
				this.columnLabelMap = Collections.emptyMap();
			}
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}

	}


	/**
	 * 返回底层ResultSet (通常是{@code javax.sql.rowset.CachedRowSet}).
	 */
	public final ResultSet getResultSet() {
		return this.resultSet;
	}

	@Override
	public final SqlRowSetMetaData getMetaData() {
		return this.rowSetMetaData;
	}

	@Override
	public int findColumn(String columnLabel) throws InvalidResultSetAccessException {
		Integer columnIndex = this.columnLabelMap.get(columnLabel);
		if (columnIndex != null) {
			return columnIndex;
		}
		else {
			try {
				return this.resultSet.findColumn(columnLabel);
			}
			catch (SQLException se) {
				throw new InvalidResultSetAccessException(se);
			}
		}
	}


	// RowSet methods for extracting data values

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getBigDecimal(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getBoolean(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException {
		return getBoolean(findColumn(columnLabel));
	}

	@Override
	public byte getByte(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getByte(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public byte getByte(String columnLabel) throws InvalidResultSetAccessException {
		return getByte(findColumn(columnLabel));
	}

	@Override
	public Date getDate(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDate(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Date getDate(String columnLabel) throws InvalidResultSetAccessException {
		return getDate(findColumn(columnLabel));
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDate(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getDate(findColumn(columnLabel), cal);
	}

	@Override
	public double getDouble(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getDouble(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public double getDouble(String columnLabel) throws InvalidResultSetAccessException {
		return getDouble(findColumn(columnLabel));
	}

	@Override
	public float getFloat(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getFloat(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public float getFloat(String columnLabel) throws InvalidResultSetAccessException {
		return getFloat(findColumn(columnLabel));
	}

	@Override
	public int getInt(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getInt(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getInt(String columnLabel) throws InvalidResultSetAccessException {
		return getInt(findColumn(columnLabel));
	}

	@Override
	public long getLong(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getLong(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public long getLong(String columnLabel) throws InvalidResultSetAccessException {
		return getLong(findColumn(columnLabel));
	}

	@Override
	public String getNString(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getNString(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getNString(String columnLabel) throws InvalidResultSetAccessException {
		return getNString(findColumn(columnLabel));
	}

	@Override
	public Object getObject(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Object getObject(String columnLabel) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex, map);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel), map);
	}

	@UsesJava7
	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getObject(columnIndex, type);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException {
		return getObject(findColumn(columnLabel), type);
	}

	@Override
	public short getShort(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getShort(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public short getShort(String columnLabel) throws InvalidResultSetAccessException {
		return getShort(findColumn(columnLabel));
	}

	@Override
	public String getString(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getString(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getString(String columnLabel) throws InvalidResultSetAccessException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public Time getTime(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTime(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Time getTime(String columnLabel) throws InvalidResultSetAccessException {
		return getTime(findColumn(columnLabel));
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTime(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getTime(findColumn(columnLabel), cal);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTimestamp(columnIndex);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException {
		return getTimestamp(findColumn(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getTimestamp(columnIndex, cal);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException {
		return getTimestamp(findColumn(columnLabel), cal);
	}


	// RowSet navigation methods

	@Override
	public boolean absolute(int row) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.absolute(row);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public void afterLast() throws InvalidResultSetAccessException {
		try {
			this.resultSet.afterLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public void beforeFirst() throws InvalidResultSetAccessException {
		try {
			this.resultSet.beforeFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean first() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.first();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getRow() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.getRow();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isAfterLast() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isAfterLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isBeforeFirst() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isBeforeFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isFirst() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isFirst();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isLast() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.isLast();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean last() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.last();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean next() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.next();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean previous() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.previous();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean relative(int rows) throws InvalidResultSetAccessException {
		try {
			return this.resultSet.relative(rows);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean wasNull() throws InvalidResultSetAccessException {
		try {
			return this.resultSet.wasNull();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}
}
