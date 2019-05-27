package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.util.ReflectionUtils;

/**
 * WebLogic的{@link NativeJdbcExtractor}接口的实现, 支持WebLogic Server 9.0及更高版本.
 *
 * <p>返回应用程序代码的底层本机Connection, 而不是WebLogic的包装器实现; 解包本机语句的Connection.
 * 然后可以安全地转换返回的JDBC类, e.g. 到{@code oracle.jdbc.OracleConnection}.
 *
 * <p>可以将此NativeJdbcExtractor设置为<i>允许</i>使用WebLogic DataSource:
 * 如果给定对象不是WebLogic Connection包装器, 则它将按原样返回.
 */
public class WebLogicNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private static final String JDBC_EXTENSION_NAME = "weblogic.jdbc.extensions.WLConnection";


	private final Class<?> jdbcExtensionClass;

	private final Method getVendorConnectionMethod;


	/**
	 * 此构造函数检索WebLogic JDBC扩展接口, 因此可以使用反射获取底层供应商连接.
	 */
	public WebLogicNativeJdbcExtractor() {
		try {
			this.jdbcExtensionClass = getClass().getClassLoader().loadClass(JDBC_EXTENSION_NAME);
			this.getVendorConnectionMethod = this.jdbcExtensionClass.getMethod("getVendorConnection", (Class[]) null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebLogicNativeJdbcExtractor because WebLogic API classes are not available: " + ex);
		}
	}


	/**
	 * 返回{@code true}, 因为WebLogic返回包装的Statement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return true;
	}

	/**
	 * 返回{@code true}, 因为WebLogic返回包装的PreparedStatement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return true;
	}

	/**
	 * 返回{@code true}, 因为WebLogic返回包装的CallableStatement.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return true;
	}

	/**
	 * 通过WebLogic的{@code getVendorConnection}方法检索Connection.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.jdbcExtensionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(this.getVendorConnectionMethod, con);
		}
		return con;
	}

}
