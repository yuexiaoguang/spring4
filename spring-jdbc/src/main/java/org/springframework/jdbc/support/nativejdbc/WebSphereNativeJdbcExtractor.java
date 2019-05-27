package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.util.ReflectionUtils;

/**
 * WebSphere的{@link NativeJdbcExtractor}接口的实现, 支持WebSphere Application Server 6.1及更高版本.
 *
 * <p>返回应用程序代码的底层本机Connection, 而不是WebSphere的包装器实现; 解包本机语句的Connection.
 * 然后可以安全地转换返回的JDBC类, e.g. 到{@code oracle.jdbc.OracleConnection}.
 *
 * <p>可以将此NativeJdbcExtractor设置为<i>允许</i>使用WebSphere DataSource:
 * 如果给定对象不是WebSphere Connection包装器, 则它将按原样返回.
 */
public class WebSphereNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private static final String JDBC_ADAPTER_CONNECTION_NAME = "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection";

	private static final String JDBC_ADAPTER_UTIL_NAME = "com.ibm.ws.rsadapter.jdbc.WSJdbcUtil";


	private Class<?> webSphereConnectionClass;

	private Method webSphereNativeConnectionMethod;


	/**
	 * 此构造函数检索WebSphere JDBC适配器类, 因此可以使用反射获取底层供应商连接.
	 */
	public WebSphereNativeJdbcExtractor() {
		try {
			this.webSphereConnectionClass = getClass().getClassLoader().loadClass(JDBC_ADAPTER_CONNECTION_NAME);
			Class<?> jdbcAdapterUtilClass = getClass().getClassLoader().loadClass(JDBC_ADAPTER_UTIL_NAME);
			this.webSphereNativeConnectionMethod =
					jdbcAdapterUtilClass.getMethod("getNativeConnection", new Class<?>[] {this.webSphereConnectionClass});
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebSphereNativeJdbcExtractor because WebSphere API classes are not available: " + ex);
		}
	}


	/**
	 * 返回{@code true}, 因为WebSphere返回包装的Statement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return true;
	}

	/**
	 * 返回{@code true}, 因为WebSphere返回包装的PreparedStatement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return true;
	}

	/**
	 * 返回{@code true}, 因为WebSphere返回包装的CallableStatement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return true;
	}

	/**
	 * 通过WebSphere的{@code getNativeConnection}方法检索Connection.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.webSphereConnectionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(this.webSphereNativeConnectionMethod, null, con);
		}
		return con;
	}
}
