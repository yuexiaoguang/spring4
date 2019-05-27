package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 为Oracle的JDBC驱动程序预先配置的{@link Jdbc4NativeJdbcExtractor}, 指定以下特定于供应商的API类型以进行解包:
 * <ul>
 * <li>{@code oracle.jdbc.OracleConnection}
 * <li>{@code oracle.jdbc.OracleStatement}
 * <li>{@code oracle.jdbc.OraclePreparedStatement}
 * <li>{@code oracle.jdbc.OracleCallableStatement}
 * <li>{@code oracle.jdbc.OracleResultSet}
 * </ul>
 *
 * <p>Note: 这适用于任何符合JDBC 4.0的连接池, 无需特定于连接池的设置.
 * 换句话说, 从JDBC 4.0开始, NativeJdbcExtractors通常将针对特定驱动程序, 而不是针对特定池实现.
 */
public class OracleJdbc4NativeJdbcExtractor extends Jdbc4NativeJdbcExtractor {

	@SuppressWarnings("unchecked")
	public OracleJdbc4NativeJdbcExtractor() {
		try {
			setConnectionType((Class<Connection>) getClass().getClassLoader().loadClass("oracle.jdbc.OracleConnection"));
			setStatementType((Class<Statement>) getClass().getClassLoader().loadClass("oracle.jdbc.OracleStatement"));
			setPreparedStatementType((Class<PreparedStatement>) getClass().getClassLoader().loadClass("oracle.jdbc.OraclePreparedStatement"));
			setCallableStatementType((Class<CallableStatement>) getClass().getClassLoader().loadClass("oracle.jdbc.OracleCallableStatement"));
			setResultSetType((Class<ResultSet>) getClass().getClassLoader().loadClass("oracle.jdbc.OracleResultSet"));
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize OracleJdbc4NativeJdbcExtractor because Oracle API classes are not available: " + ex);
		}
	}

}
