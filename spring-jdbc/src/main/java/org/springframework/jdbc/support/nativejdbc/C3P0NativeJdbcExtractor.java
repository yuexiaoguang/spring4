package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.C3P0ProxyConnection;

import org.springframework.util.ReflectionUtils;

/**
 * C3P0连接池的{@link NativeJdbcExtractor}接口实现.
 *
 * <p>返回应用程序代码的底层本机Connection, 而不是C3P0的包装器实现; 解包本机语句的Connection.
 * 然后可以安全地转换返回的JDBC类, e.g. 到{@code oracle.jdbc.OracleConnection}.
 *
 * <p>可以将此NativeJdbcExtractor设置为<i>允许</i>使用C3P0数据源:
 * 如果给定对象不是C3P0包装器, 它将按原样返回.
 *
 * <p>请注意, 此类需要C3P0 0.8.5或更高版本; 对于早期的C3P0版本, 请使用SimpleNativeJdbcExtractor (不适用于C3P0 0.8.5或更高版本).
 */
public class C3P0NativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private final Method getRawConnectionMethod;


	/**
	 * 此方法不能直接使用; 它更适合作为C3P0的"rawConnectionOperation" API的回调方法.
	 * 
	 * @param con 本机Connection句柄
	 * 
	 * @return 本机Connection句柄
	 */
	public static Connection getRawConnection(Connection con) {
		return con;
	}


	public C3P0NativeJdbcExtractor() {
		try {
			this.getRawConnectionMethod = getClass().getMethod("getRawConnection", new Class<?>[] {Connection.class});
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Internal error in C3P0NativeJdbcExtractor: " + ex.getMessage());
		}
	}


	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return true;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return true;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return true;
	}

	/**
	 * 通过C3P0的{@code rawConnectionOperation} API检索Connection,
	 * 使用{@code getRawConnection}作为回调来访问原始Connection (否则C3P0不直接支持).
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (con instanceof C3P0ProxyConnection) {
			C3P0ProxyConnection cpCon = (C3P0ProxyConnection) con;
			try {
				return (Connection) cpCon.rawConnectionOperation(
						this.getRawConnectionMethod, null, new Object[] {C3P0ProxyConnection.RAW_CONNECTION});
			}
			catch (SQLException ex) {
				throw ex;
			}
			catch (Exception ex) {
				ReflectionUtils.handleReflectionException(ex);
			}
		}
		return con;
	}

}
