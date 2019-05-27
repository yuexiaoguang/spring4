package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * 用于{@link NativeJdbcExtractor}接口的抽象适配器类, 用于简化基本提取器的实现.
 * 基本上返回所有方法上传入的JDBC对象.
 *
 * <p>{@code getNativeConnection}在委托给{@code doGetNativeConnection}进行实际解包之前,
 * 检查ConnectionProxy链, 例如从TransactionAwareDataSourceProxy检查.
 * 可以为特定连接池覆盖两者中的任何一个, 但建议后者参与ConnectionProxy解包.
 *
 * <p>{@code getNativeConnection}也会应用回退, 如果第一个本机提取过程失败, 即返回与传入的相同的连接.
 * 假设在这种情况下会进行一些额外的代理:
 * 因此, 它通过{@code conHandle.getMetaData().getConnection()}从DatabaseMetaData检索底层本机Connection,
 * 并根据该Connection句柄重试本机提取过程.
 * 例如, 这适用于Hibernate 3.1的{@code Session.connection()}公开的Connection代理.
 *
 * <p>实现{@code getNativeConnectionFromStatement}方法, 只需使用Statement的Connection委托给{@code getNativeConnection}.
 * 这是大多数提取器实现将坚持的, 除非为特定池提供更高效的版本.
 */
public abstract class NativeJdbcExtractorAdapter implements NativeJdbcExtractor {

	/**
	 * 默认{@code false}.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return false;
	}

	/**
	 * 默认{@code false}.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return false;
	}

	/**
	 * 默认{@code false}.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return false;
	}

	/**
	 * 检查ConnectionProxy链, 然后委托给doGetNativeConnection.
	 * <p>Spring的 TransactionAwareDataSourceProxy 和 LazyConnectionDataSourceProxy 使用ConnectionProxy.
	 * 它背后的目标连接通常是来自本地连接池的目标连接, 由具体子类的doGetNativeConnection实现解包.
	 */
	@Override
	public Connection getNativeConnection(Connection con) throws SQLException {
		if (con == null) {
			return null;
		}
		Connection targetCon = DataSourceUtils.getTargetConnection(con);
		Connection nativeCon = doGetNativeConnection(targetCon);
		if (nativeCon == targetCon) {
			// 没有收到不同的Connection, 所以假设还有一些额外的代理.
			// 检查是否从DatabaseMetaData.getConnection()调用得到了不同的东西.
			DatabaseMetaData metaData = targetCon.getMetaData();
			// 以下检查仅适用于可能无法携带DatabaseMetaData实例的模拟Connection.
			if (metaData != null) {
				Connection metaCon = metaData.getConnection();
				if (metaCon != null && metaCon != targetCon) {
					// 收到了一个不同的Connection: 用它重试本机提取过程.
					nativeCon = doGetNativeConnection(metaCon);
				}
			}
		}
		return nativeCon;
	}

	/**
	 * 无法解包: 返回传入的Connection.
	 */
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return con;
	}

	/**
	 * 通过Statement的Connection检索Connection.
	 */
	@Override
	public Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException {
		if (stmt == null) {
			return null;
		}
		return getNativeConnection(stmt.getConnection());
	}

	/**
	 * 无法解包: 返回传入的Statement.
	 */
	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return stmt;
	}

	/**
	 * 无法解包: 返回传入的PreparedStatement.
	 */
	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return ps;
	}

	/**
	 * 无法解包: 返回传入的CallableStatement.
	 */
	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return cs;
	}

	/**
	 * 无法解包: 返回传入的ResultSet.
	 */
	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return rs;
	}
}
