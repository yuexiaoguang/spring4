package org.springframework.jdbc.core.namedparam;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BatchUpdateUtils;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * 使用命名参数处理JDBC批处理语句的通用工具方法.
 * 主要供框架内部使用.
 */
public class NamedParameterBatchUpdateUtils extends BatchUpdateUtils {

	public static int[] executeBatchUpdateWithNamedParameters(final ParsedSql parsedSql,
			final SqlParameterSource[] batchArgs, JdbcOperations jdbcOperations) {

		if (batchArgs.length <= 0) {
			return new int[] {0};
		}

		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, batchArgs[0]);
		return jdbcOperations.batchUpdate(
				sqlToUse,
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Object[] values = NamedParameterUtils.buildValueArray(parsedSql, batchArgs[i], null);
						int[] columnTypes = NamedParameterUtils.buildSqlTypeArray(parsedSql, batchArgs[i]);
						setStatementParameters(values, ps, columnTypes);
					}
					@Override
					public int getBatchSize() {
						return batchArgs.length;
					}
				});
	}
}
