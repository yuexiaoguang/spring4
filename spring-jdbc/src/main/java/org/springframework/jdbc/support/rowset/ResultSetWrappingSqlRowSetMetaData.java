package org.springframework.jdbc.support.rowset;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * Spring的{@link SqlRowSetMetaData}接口的默认实现, 包装{@link java.sql.ResultSetMetaData}实例,
 * 捕获任何{@link SQLException}并将它们转换为相应的Spring {@link InvalidResultSetAccessException}.
 *
 * <p>由{@link ResultSetWrappingSqlRowSet}使用.
 */
public class ResultSetWrappingSqlRowSetMetaData implements SqlRowSetMetaData {

	private final ResultSetMetaData resultSetMetaData;

	private String[] columnNames;


	/**
	 * @param resultSetMetaData 要包装的断开连接的ResultSetMetaData实例 (通常是{@code javax.sql.RowSetMetaData}实例)
	 */
	public ResultSetWrappingSqlRowSetMetaData(ResultSetMetaData resultSetMetaData) {
		this.resultSetMetaData = resultSetMetaData;
	}


	@Override
	public String getCatalogName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getCatalogName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnClassName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnClassName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getColumnCount() throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnCount();
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String[] getColumnNames() throws InvalidResultSetAccessException {
		if (this.columnNames == null) {
			this.columnNames = new String[getColumnCount()];
			for (int i = 0; i < getColumnCount(); i++) {
				this.columnNames[i] = getColumnName(i + 1);
			}
		}
		return this.columnNames;
	}

	@Override
	public int getColumnDisplaySize(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnDisplaySize(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnLabel(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnLabel(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getColumnType(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnType(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getColumnTypeName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getColumnTypeName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getPrecision(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getPrecision(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public int getScale(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getScale(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getSchemaName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getSchemaName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public String getTableName(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.getTableName(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isCaseSensitive(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isCaseSensitive(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isCurrency(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isCurrency(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}

	@Override
	public boolean isSigned(int column) throws InvalidResultSetAccessException {
		try {
			return this.resultSetMetaData.isSigned(column);
		}
		catch (SQLException se) {
			throw new InvalidResultSetAccessException(se);
		}
	}
}
