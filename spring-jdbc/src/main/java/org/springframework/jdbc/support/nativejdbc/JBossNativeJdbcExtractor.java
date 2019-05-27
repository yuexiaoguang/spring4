package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.util.ReflectionUtils;

/**
 * JBoss的{@link NativeJdbcExtractor}接口的实现, 支持JBoss Application Server 3.2.4+.
 * 从Spring 3.1.1开始, 它也支持JBoss 7.
 *
 * <p>将底层本机Connection, Statement等返回给应用程序代码, 而不是JBoss的包装器实现.
 * 然后可以安全地转换返回的JDBC类, e.g. 到{@code oracle.jdbc.OracleConnection}.
 *
 * <p>可以将此NativeJdbcExtractor设置为<i>允许</i>使用JBoss连接池:
 * 如果给定的对象不是JBoss包装器, 它将按原样返回.
 */
public class JBossNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	// JBoss 7
	private static final String JBOSS_JCA_PREFIX = "org.jboss.jca.adapters.jdbc.";

	// JBoss <= 6
	private static final String JBOSS_RESOURCE_PREFIX = "org.jboss.resource.adapter.jdbc.";


	private Class<?> wrappedConnectionClass;

	private Class<?> wrappedStatementClass;

	private Class<?> wrappedResultSetClass;

	private Method getUnderlyingConnectionMethod;

	private Method getUnderlyingStatementMethod;

	private Method getUnderlyingResultSetMethod;


	/**
	 * 此构造函数检索JBoss JDBC包装器类, 因此可以使用反射获取底层供应商连接.
	 */
	public JBossNativeJdbcExtractor() {
		String prefix = JBOSS_JCA_PREFIX;
		try {
			// trying JBoss 7 jca package first...
			this.wrappedConnectionClass = getClass().getClassLoader().loadClass(prefix + "WrappedConnection");
		}
		catch (ClassNotFoundException ex) {
			// JBoss 7 jca package not found -> try traditional resource package.
			prefix = JBOSS_RESOURCE_PREFIX;
			try {
				this.wrappedConnectionClass = getClass().getClassLoader().loadClass(prefix + "WrappedConnection");
			}
			catch (ClassNotFoundException ex2) {
				throw new IllegalStateException("Could not initialize JBossNativeJdbcExtractor: neither JBoss 7's [" +
						JBOSS_JCA_PREFIX + ".WrappedConnection] nor traditional JBoss [" + JBOSS_RESOURCE_PREFIX +
						".WrappedConnection] found");
			}
		}
		try {
			this.wrappedStatementClass = getClass().getClassLoader().loadClass(prefix + "WrappedStatement");
			this.wrappedResultSetClass = getClass().getClassLoader().loadClass(prefix + "WrappedResultSet");
			this.getUnderlyingConnectionMethod =
				this.wrappedConnectionClass.getMethod("getUnderlyingConnection", (Class[]) null);
			this.getUnderlyingStatementMethod =
				this.wrappedStatementClass.getMethod("getUnderlyingStatement", (Class[]) null);
			this.getUnderlyingResultSetMethod =
				this.wrappedResultSetClass.getMethod("getUnderlyingResultSet", (Class[]) null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBossNativeJdbcExtractor because of missing JBoss API methods/classes: " + ex);
		}
	}


	/**
	 * 通过JBoss的{@code getUnderlyingConnection}方法检索Connection.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.wrappedConnectionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingConnectionMethod, con);
		}
		return con;
	}

	/**
	 * 通过JBoss的{@code getUnderlyingStatement}方法检索Connection.
	 */
	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		if (this.wrappedStatementClass.isAssignableFrom(stmt.getClass())) {
			return (Statement) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingStatementMethod, stmt);
		}
		return stmt;
	}

	/**
	 * 通过JBoss的{@code getUnderlyingStatement}方法检索Connection.
	 */
	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return (PreparedStatement) getNativeStatement(ps);
	}

	/**
	 * 通过JBoss的{@code getUnderlyingStatement}方法检索Connection.
	 */
	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return (CallableStatement) getNativeStatement(cs);
	}

	/**
	 * 通过JBoss的{@code getUnderlyingResultSet}方法检索Connection.
	 */
	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		if (this.wrappedResultSetClass.isAssignableFrom(rs.getClass())) {
			return (ResultSet) ReflectionUtils.invokeJdbcMethod(this.getUnderlyingResultSetMethod, rs);
		}
		return rs;
	}
}
