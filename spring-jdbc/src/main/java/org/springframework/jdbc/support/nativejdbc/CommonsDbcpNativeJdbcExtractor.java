package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.util.ReflectionUtils;

/**
 * Apache Commons DBCP连接池(版本1.1或更高版本)的{@link NativeJdbcExtractor}接口的实现.
 *
 * <p>将底层本机Connection, Statement等返回给应用程序代码, 而不是DBCP的包装器实现.
 * 然后可以安全地转换返回的JDBC类, e.g. 到{@code oracle.jdbc.OracleConnection}.
 *
 * <p>可以将此NativeJdbcExtractor设置为<i>允许</i>使用Commons DBCP DataSource:
 * 如果给定对象不是Commons DBCP包装器, 它将按原样返回.
 *
 * <p>请注意，此版本的CommonsDbcpNativeJdbcExtractor将针对{@code org.apache.commons.dbcp}中的原始Commons DBCP,
 * 以及{@code org.apache.tomcat.dbcp.dbcp}包中Tomcat 5.5的重定位Commons DBCP版本.
 *
 * @deprecated as of Spring 4.2, in favor of Commons DBCP 2.x and JDBC 4.x
 */
@Deprecated
public class CommonsDbcpNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private static final String GET_INNERMOST_DELEGATE_METHOD_NAME = "getInnermostDelegate";


	/**
	 * 从给定的Commons DBCP对象中提取最里面的委托.
	 * 如果找不到底层对象, 则回退到给定对象.
	 * 
	 * @param obj the Commons DBCP Connection/Statement/ResultSet
	 * 
	 * @return 底层本机Connection/Statement/ResultSet
	 */
	private static Object getInnermostDelegate(Object obj) throws SQLException {
		if (obj == null) {
			return null;
		}
		try {
			Class<?> classToAnalyze = obj.getClass();
			while (!Modifier.isPublic(classToAnalyze.getModifiers())) {
				classToAnalyze = classToAnalyze.getSuperclass();
				if (classToAnalyze == null) {
					// No public provider class found -> fall back to given object.
					return obj;
				}
			}
			Method getInnermostDelegate = classToAnalyze.getMethod(GET_INNERMOST_DELEGATE_METHOD_NAME, (Class[]) null);
			Object delegate = ReflectionUtils.invokeJdbcMethod(getInnermostDelegate, obj);
			return (delegate != null ? delegate : obj);
		}
		catch (NoSuchMethodException ex) {
			return obj;
		}
		catch (SecurityException ex) {
			throw new IllegalStateException("Commons DBCP getInnermostDelegate method is not accessible: " + ex);
		}
	}


	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		return (Connection) getInnermostDelegate(con);
	}

	@Override
	public Statement getNativeStatement(Statement stmt) throws SQLException {
		return (Statement) getInnermostDelegate(stmt);
	}

	@Override
	public PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException {
		return (PreparedStatement) getNativeStatement(ps);
	}

	@Override
	public CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException {
		return (CallableStatement) getNativeStatement(cs);
	}

	@Override
	public ResultSet getNativeResultSet(ResultSet rs) throws SQLException {
		return (ResultSet) getInnermostDelegate(rs);
	}

}
