package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 委托给JDBC 4.0 {@code unwrap}方法的{@link NativeJdbcExtractor}实现，由{@link java.sql.Wrapper}定义.
 * 通常需要指定供应商{@link #setConnectionType Connection类型} / {@link #setStatementType Statement类型}
 *  / {@link #setResultSetType ResultSet类型}来提取, 因为JDBC 4.0仅实际解包到给定的目标类型.
 *
 * <p>Note: 仅在实际运行JDBC 4.0驱动程序时使用此方法, 并使用支持JDBC 4.0 API的连接池
 * (i.e. 至少接受JDBC 4.0 API调用并将它们传递给底层驱动程序)!
 * 除此之外, 不需要特定于连接池的设置.
 * 从JDBC 4.0开始, NativeJdbcExtractor通常将针对特定驱动程序而不是针对特定池实现 (e.g. {@link OracleJdbc4NativeJdbcExtractor}).
 */
public class Jdbc4NativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private Class<? extends Connection> connectionType = Connection.class;

	private Class<? extends Statement> statementType = Statement.class;

	private Class<? extends PreparedStatement> preparedStatementType = PreparedStatement.class;

	private Class<? extends CallableStatement> callableStatementType = CallableStatement.class;

	private Class<? extends ResultSet> resultSetType = ResultSet.class;


	/**
	 * 设置供应商的Connection类型, e.g. {@code oracle.jdbc.OracleConnection}.
	 */
	public void setConnectionType(Class<? extends Connection> connectionType) {
		this.connectionType = connectionType;
	}

	/**
	 * 设置供应商的Statement类型, e.g. {@code oracle.jdbc.OracleStatement}.
	 */
	public void setStatementType(Class<? extends Statement> statementType) {
		this.statementType = statementType;
	}

	/**
	 * 设置供应商的PreparedStatement类型, e.g. {@code oracle.jdbc.OraclePreparedStatement}.
	 */
	public void setPreparedStatementType(Class<? extends PreparedStatement> preparedStatementType) {
		this.preparedStatementType = preparedStatementType;
	}

	/**
	 * 设置供应商的 CallableStatement类型, e.g. {@code oracle.jdbc.OracleCallableStatement}.
	 */
	public void setCallableStatementType(Class<? extends CallableStatement> callableStatementType) {
		this.callableStatementType = callableStatementType;
	}

	/**
	 * 设置供应商的ResultSet类型, e.g. {@code oracle.jdbc.OracleResultSet}.
	 */
	public void setResultSetType(Class<? extends ResultSet> resultSetType) {
		this.resultSetType = resultSetType;
	}


	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return con.unwrap(this.connectionType);
	}

	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return stmt.unwrap(this.statementType);
	}

	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return ps.unwrap(this.preparedStatementType);
	}

	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return cs.unwrap(this.callableStatementType);
	}

	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return rs.unwrap(this.resultSetType);
	}
}
