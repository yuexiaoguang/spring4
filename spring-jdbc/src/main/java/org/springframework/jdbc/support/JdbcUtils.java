package org.springframework.jdbc.support;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.UsesJava7;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * 使用JDBC的通用工具方法.
 * 主要供框架内部使用, 也适用于自定义JDBC访问代码.
 */
public abstract class JdbcUtils {

	/**
	 * 指示未知(或未指定)SQL类型的常量.
	 */
	public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;


	// Check for JDBC 4.1 getObject(int, Class) method - available on JDK 7 and higher
	private static final boolean getObjectWithTypeAvailable =
			ClassUtils.hasMethod(ResultSet.class, "getObject", int.class, Class.class);

	private static final Log logger = LogFactory.getLog(JdbcUtils.class);


	/**
	 * 关闭给定的JDBC Connection并忽略任何抛出的异常.
	 * 这对手动JDBC代码中的典型finally块非常有用.
	 * 
	 * @param con 要关闭的JDBC连接 (may be {@code null})
	 */
	public static void closeConnection(Connection con) {
		if (con != null) {
			try {
				con.close();
			}
			catch (SQLException ex) {
				logger.debug("Could not close JDBC Connection", ex);
			}
			catch (Throwable ex) {
				// 不信任JDBC驱动程序: 它可能会抛出RuntimeException 或 Error.
				logger.debug("Unexpected exception on closing JDBC Connection", ex);
			}
		}
	}

	/**
	 * 关闭给定的JDBC语句并忽略任何抛出的异常.
	 * 这对手动JDBC代码中的典型finally块非常有用.
	 * 
	 * @param stmt 要关闭的JDBC语句 (may be {@code null})
	 */
	public static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close JDBC Statement", ex);
			}
			catch (Throwable ex) {
				// 不信任JDBC驱动程序: 它可能会抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JDBC Statement", ex);
			}
		}
	}

	/**
	 * 关闭给定的JDBC ResultSet并忽略任何抛出的异常.
	 * 这对手动JDBC代码中的典型finally块非常有用.
	 * 
	 * @param rs 要关闭的JDBC ResultSet (may be {@code null})
	 */
	public static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close JDBC ResultSet", ex);
			}
			catch (Throwable ex) {
				// 不信任JDBC驱动程序: 它可能会抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JDBC ResultSet", ex);
			}
		}
	}

	/**
	 * 使用指定的值类型从ResultSet中检索JDBC列值.
	 * <p>使用特定类型的ResultSet访问器方法, 对于未知类型, 回退到{@link #getResultSetValue(java.sql.ResultSet, int)}.
	 * <p>请注意, 如果类型未知, 则返回的值可能无法分配给指定的必需类型.
	 * 调用代码需要适当地处理这种情况, e.g. 抛出相应的异常.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * @param requiredType 所需的值类型 (may be {@code null})
	 * 
	 * @return 值对象 (可能不是指定的必需类型, 需要进一步的转换步骤)
	 * @throws SQLException 如果由JDBC API抛出
	 */
	@UsesJava7  // 保护可选使用JDBC 4.1 (由于getObjectWithTypeAvailable检查, 使用1.6也是安全的)
	public static Object getResultSetValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {
		if (requiredType == null) {
			return getResultSetValue(rs, index);
		}

		Object value;

		// 尽可能明确地提取类型值.
		if (String.class == requiredType) {
			return rs.getString(index);
		}
		else if (boolean.class == requiredType || Boolean.class == requiredType) {
			value = rs.getBoolean(index);
		}
		else if (byte.class == requiredType || Byte.class == requiredType) {
			value = rs.getByte(index);
		}
		else if (short.class == requiredType || Short.class == requiredType) {
			value = rs.getShort(index);
		}
		else if (int.class == requiredType || Integer.class == requiredType) {
			value = rs.getInt(index);
		}
		else if (long.class == requiredType || Long.class == requiredType) {
			value = rs.getLong(index);
		}
		else if (float.class == requiredType || Float.class == requiredType) {
			value = rs.getFloat(index);
		}
		else if (double.class == requiredType || Double.class == requiredType ||
				Number.class == requiredType) {
			value = rs.getDouble(index);
		}
		else if (BigDecimal.class == requiredType) {
			return rs.getBigDecimal(index);
		}
		else if (java.sql.Date.class == requiredType) {
			return rs.getDate(index);
		}
		else if (java.sql.Time.class == requiredType) {
			return rs.getTime(index);
		}
		else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
			return rs.getTimestamp(index);
		}
		else if (byte[].class == requiredType) {
			return rs.getBytes(index);
		}
		else if (Blob.class == requiredType) {
			return rs.getBlob(index);
		}
		else if (Clob.class == requiredType) {
			return rs.getClob(index);
		}
		else if (requiredType.isEnum()) {
			// 枚举可以通过String或枚举索引值表示:
			// 将枚举类型转换保留到调用者(e.g. a ConversionService), 但要确保除了String或Integer之外什么也不返回.
			Object obj = rs.getObject(index);
			if (obj instanceof String) {
				return obj;
			}
			else if (obj instanceof Number) {
				// 防御性地将 Number转换为Integer (根据ConversionService的IntegerToEnumConverterFactory的需要)用作索引
				return NumberUtils.convertNumberToTargetClass((Number) obj, Integer.class);
			}
			else {
				// e.g. 在Postgres上: getObject返回一个PGObject, 但需要一个String
				return rs.getString(index);
			}
		}

		else {
			// 一些未知的类型 -> 依赖于getObject.
			if (getObjectWithTypeAvailable) {
				try {
					return rs.getObject(index, requiredType);
				}
				catch (AbstractMethodError err) {
					logger.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
				}
				catch (SQLFeatureNotSupportedException ex) {
					logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
				}
				catch (SQLException ex) {
					logger.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
				}
			}

			// JSR-310 / Joda-Time类型的相应SQL类型, 由调用者转换它们 (e.g. 通过ConversionService).
			String typeName = requiredType.getSimpleName();
			if ("LocalDate".equals(typeName)) {
				return rs.getDate(index);
			}
			else if ("LocalTime".equals(typeName)) {
				return rs.getTime(index);
			}
			else if ("LocalDateTime".equals(typeName)) {
				return rs.getTimestamp(index);
			}

			// 回退到没有类型规范的getObject, 如果需要, 再次留给调用者来转换值.
			return getResultSetValue(rs, index);
		}

		// 必要时执行was-null检查 (对于JDBC驱动程序返回基础类型的结果).
		return (rs.wasNull() ? null : value);
	}

	/**
	 * 使用最合适的值类型从ResultSet中检索JDBC列值.
	 * 返回的值应该是一个分离的值对象, 与活动的ResultSet没有任何关系:
	 * 特别是, 它不应该是Blob或Clob对象, 而应该分别是字节数组或字符串表示.
	 * <p>使用{@code getObject(index)}方法, 但包含额外的"hacks"以获取Oracle 10g,
	 * 为其TIMESTAMP数据类型返回非标准对象, 为DATE列返回{@code java.sql.Date}, 以省去时间部分:
	 * 这些列将明确提取为标准{@code java.sql.Timestamp}对象.
	 * 
	 * @param rs 保存数据的ResultSet
	 * @param index 列索引
	 * 
	 * @return 值对象
	 * @throws SQLException 如果由JDBC API抛出
	 */
	public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
		Object obj = rs.getObject(index);
		String className = null;
		if (obj != null) {
			className = obj.getClass().getName();
		}
		if (obj instanceof Blob) {
			Blob blob = (Blob) obj;
			obj = blob.getBytes(1, (int) blob.length());
		}
		else if (obj instanceof Clob) {
			Clob clob = (Clob) obj;
			obj = clob.getSubString(1, (int) clob.length());
		}
		else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
			obj = rs.getTimestamp(index);
		}
		else if (className != null && className.startsWith("oracle.sql.DATE")) {
			String metaDataClassName = rs.getMetaData().getColumnClassName(index);
			if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
				obj = rs.getTimestamp(index);
			}
			else {
				obj = rs.getDate(index);
			}
		}
		else if (obj instanceof java.sql.Date) {
			if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
				obj = rs.getTimestamp(index);
			}
		}
		return obj;
	}

	/**
	 * 通过给定的DatabaseMetaDataCallback提取数据库元数据.
	 * <p>此方法将打开与数据库的连接并检索数据库元数据.
	 * 由于在为数据源配置异常转换功能之前调用此方法, 因此此方法不能依赖于SQLException转换功能.
	 * <p>任何异常都将包装到MetaDataAccessException中. 这是一个受检异常, 任何调用代码都应该捕获并处理此异常.
	 * 可以只记录错误并希望获得最佳效果, 但是当您尝试再次访问数据库时, 可能会出现更严重的错误.
	 * 
	 * @param dataSource 用于提取元数据的DataSource
	 * @param action 将执行实际工作的回调
	 * 
	 * @return 包含提取信息的对象, 由DatabaseMetaDataCallback的{@code processMetaData}方法返回
	 * @throws MetaDataAccessException 如果元数据访问失败
	 */
	public static Object extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback action)
			throws MetaDataAccessException {

		Connection con = null;
		try {
			con = DataSourceUtils.getConnection(dataSource);
			if (con == null) {
				// 应该只在测试环境中发生
				throw new MetaDataAccessException("Connection returned by DataSource [" + dataSource + "] was null");
			}
			DatabaseMetaData metaData = con.getMetaData();
			if (metaData == null) {
				// 应该只在测试环境中发生
				throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
			}
			return action.processMetaData(metaData);
		}
		catch (CannotGetJdbcConnectionException ex) {
			throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
		}
		catch (SQLException ex) {
			throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
		}
		catch (AbstractMethodError err) {
			throw new MetaDataAccessException(
					"JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	/**
	 * 在DatabaseMetaData上为给定的DataSource调用指定的方法, 并提取调用结果.
	 * 
	 * @param dataSource 用于提取元数据的DataSource
	 * @param metaDataMethodName 要调用的DatabaseMetaData方法的名称
	 * 
	 * @return 指定的DatabaseMetaData方法返回的对象
	 * @throws MetaDataAccessException 如果无法访问DatabaseMetaData或无法调用指定的方法
	 */
	public static Object extractDatabaseMetaData(DataSource dataSource, final String metaDataMethodName)
			throws MetaDataAccessException {

		return extractDatabaseMetaData(dataSource,
				new DatabaseMetaDataCallback() {
					@Override
					public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
						try {
							return DatabaseMetaData.class.getMethod(metaDataMethodName).invoke(dbmd);
						}
						catch (NoSuchMethodException ex) {
							throw new MetaDataAccessException("No method named '" + metaDataMethodName +
									"' found on DatabaseMetaData instance [" + dbmd + "]", ex);
						}
						catch (IllegalAccessException ex) {
							throw new MetaDataAccessException(
									"Could not access DatabaseMetaData method '" + metaDataMethodName + "'", ex);
						}
						catch (InvocationTargetException ex) {
							if (ex.getTargetException() instanceof SQLException) {
								throw (SQLException) ex.getTargetException();
							}
							throw new MetaDataAccessException(
									"Invocation of DatabaseMetaData method '" + metaDataMethodName + "' failed", ex);
						}
					}
				});
	}

	/**
	 * 返回给定的JDBC驱动程序是否支持JDBC 2.0批量更新.
	 * <p>通常在执行一组给定语句之前调用:
	 * 判断是否应该通过JDBC 2.0批处理机制执行SQL语句集, 或者只是以传统的逐个方式执行.
	 * <p>如果"supportsBatchUpdates"方法抛出异常并在这种情况下返回{@code false}, 则记录警告.
	 * 
	 * @param con 要检查的Connection
	 * 
	 * @return 是否支持JDBC 2.0批量更新
	 */
	public static boolean supportsBatchUpdates(Connection con) {
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			if (dbmd != null) {
				if (dbmd.supportsBatchUpdates()) {
					logger.debug("JDBC driver supports batch updates");
					return true;
				}
				else {
					logger.debug("JDBC driver does not support batch updates");
				}
			}
		}
		catch (SQLException ex) {
			logger.debug("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
		}
		return false;
	}

	/**
	 * 即使各种驱动程序/平台在运行时提供不同的名称, 也要为正在使用的目标数据库提取公用名.
	 * 
	 * @param source 数据库元数据中提供的名称
	 * 
	 * @return 要使用的通用名称 (e.g. "DB2" or "Sybase")
	 */
	public static String commonDatabaseName(String source) {
		String name = source;
		if (source != null && source.startsWith("DB2")) {
			name = "DB2";
		}
		else if ("Sybase SQL Server".equals(source) ||
				"Adaptive Server Enterprise".equals(source) ||
				"ASE".equals(source) ||
				"sql server".equalsIgnoreCase(source) ) {
			name = "Sybase";
		}
		return name;
	}

	/**
	 * 检查给定的SQL类型是否为数字.
	 * 
	 * @param sqlType 要检查的SQL类型
	 * 
	 * @return 类型是否为数字
	 */
	public static boolean isNumeric(int sqlType) {
		return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType ||
				Types.DOUBLE == sqlType || Types.FLOAT == sqlType || Types.INTEGER == sqlType ||
				Types.NUMERIC == sqlType || Types.REAL == sqlType || Types.SMALLINT == sqlType ||
				Types.TINYINT == sqlType);
	}

	/**
	 * 确定要使用的列名称. 列名是基于使用ResultSetMetaData的查找确定的.
	 * <p>此方法实现考虑了JDBC 4.0规范中表达的最新说明:
	 * <p><i>columnLabel - 使用SQL AS子句指定的列的标签.
	 * 如果未指定SQL AS子句, 则标签是列的名称</i>.
	 * 
	 * @param resultSetMetaData 要使用的当前元数据
	 * @param columnIndex 要查找的列的索引
	 * 
	 * @return 要使用的列名
	 * @throws SQLException 查找失败
	 */
	public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (!StringUtils.hasLength(name)) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	/**
	 * 使用"驼峰规则"将带有下划线的列名转换为相应的属性名.
	 * 像"customer_number"这样的名称将匹配"customerNumber"属性名称.
	 * 
	 * @param name 要转换的列名称
	 * 
	 * @return 使用"驼峰规则"的名称
	 */
	public static String convertUnderscoreNameToPropertyName(String name) {
		StringBuilder result = new StringBuilder();
		boolean nextIsUpper = false;
		if (name != null && name.length() > 0) {
			if (name.length() > 1 && name.charAt(1) == '_') {
				result.append(Character.toUpperCase(name.charAt(0)));
			}
			else {
				result.append(Character.toLowerCase(name.charAt(0)));
			}
			for (int i = 1; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c == '_') {
					nextIsUpper = true;
				}
				else {
					if (nextIsUpper) {
						result.append(Character.toUpperCase(c));
						nextIsUpper = false;
					}
					else {
						result.append(Character.toLowerCase(c));
					}
				}
			}
		}
		return result.toString();
	}
}
