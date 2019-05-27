package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 使用JDBC批处理语句的通用方法.
 * 主要供框架内部使用.
 */
public abstract class BatchUpdateUtils {

	public static int[] executeBatchUpdate(
			String sql, final List<Object[]> batchValues, final int[] columnTypes, JdbcOperations jdbcOperations) {

		return jdbcOperations.batchUpdate(
				sql,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = batchValues.get(i);
						setStatementParameters(values, ps, columnTypes);
					}
					@Override
					public int getBatchSize() {
						return batchValues.size();
					}
				});
	}

	protected static void setStatementParameters(Object[] values, PreparedStatement ps, int[] columnTypes)
			throws SQLException {

		int colIndex = 0;
		for (Object value : values) {
			colIndex++;
			if (value instanceof SqlParameterValue) {
				SqlParameterValue paramValue = (SqlParameterValue) value;
				StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
			}
			else {
				int colType;
				if (columnTypes == null || columnTypes.length < colIndex) {
					colType = SqlTypeValue.TYPE_UNKNOWN;
				}
				else {
					colType = columnTypes[colIndex - 1];
				}
				StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
			}
		}
	}

}
