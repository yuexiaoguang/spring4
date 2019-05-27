package org.springframework.jdbc.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.jdbc.support.SqlValue;

/**
 * PreparedStatementSetter/Creator 和 CallableStatementCreator实现的实用方法,
 * 提供复杂的参数管理 (包括对LOB值的支持).
 *
 * <p>由PreparedStatementCreatorFactory和CallableStatementCreatorFactory使用,
 * 但也可以在自定义 setter/creator实现中直接使用.
 */
public abstract class StatementCreatorUtils {

	/**
	 * 指示Spring完全忽略{@link java.sql.ParameterMetaData#getParameterType}的系统属性,
	 * i.e. 从来没有尝试为{@link StatementCreatorUtils#setNull}调用检索{@link PreparedStatement#getParameterMetaData()}.
	 * <p>有效默认值为 "false", 首先尝试{@code getParameterType}调用,
	 * 并根据一般数据库的众所周知的行为回退到{@link PreparedStatement#setNull} / {@link PreparedStatement#setObject}调用.
	 * Spring记录了具有非工作{@code getParameterType}实现的JDBC驱动程序, 并且不会再次尝试为该驱动程序调用该方法, 总是会退回.
	 * <p>如果在运行时遇到错误行为, 请考虑将此标志切换为 "true",
	 * e.g. 如果从{@code getParameterType}抛出异常, 则使用连接池设置{@link PreparedStatement}实例 (在JBoss AS 7上报告).
	 * <p>请注意, 默认情况下, 此标志在Oracle 12c上为"true", 因为在这种情况下{@code getParameterType}调用可能会发生泄漏.
	 * 需要将标志显式设置为 "false", 以强制使用{@code getParameterType}用于Oracle驱动程序.
	 */
	public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "spring.jdbc.getParameterType.ignore";


	static final Boolean shouldIgnoreGetParameterType;

	static final Set<String> driversWithNoSupportForGetParameterType =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(1));

	private static final Log logger = LogFactory.getLog(StatementCreatorUtils.class);

	private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<Class<?>, Integer>(32);

	static {
		String propVal = SpringProperties.getProperty(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);
		shouldIgnoreGetParameterType = (propVal != null ? Boolean.valueOf(propVal) : null);

		javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
		javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
		javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
		javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
		javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
		javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
	}


	/**
	 * 从给定的Java类型派生默认SQL类型.
	 * 
	 * @param javaType 要翻译的Java类型
	 * 
	 * @return 相应的SQL类型, 或{@link SqlTypeValue#TYPE_UNKNOWN}
	 */
	public static int javaTypeToSqlParameterType(Class<?> javaType) {
		Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
		if (sqlType != null) {
			return sqlType;
		}
		if (Number.class.isAssignableFrom(javaType)) {
			return Types.NUMERIC;
		}
		if (isStringValue(javaType)) {
			return Types.VARCHAR;
		}
		if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
			return Types.TIMESTAMP;
		}
		return SqlTypeValue.TYPE_UNKNOWN;
	}

	/**
	 * 设置参数的值.
	 * 使用的方法基于参数的SQL类型, 我们可以处理复杂类型, 如数组和LOB.
	 * 
	 * @param ps 预准备语句或可回调语句
	 * @param paramIndex 正在设置的参数的索引
	 * @param param 声明时的参数, 包括类型
	 * @param inValue 要设置的值
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, SqlParameter param, Object inValue)
			throws SQLException {

		setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
	}

	/**
	 * 设置参数的值.
	 * 使用的方法基于参数的SQL类型, 我们可以处理复杂类型, 如数组和LOB.
	 * 
	 * @param ps 预准备语句或可回调语句
	 * @param paramIndex 正在设置的参数的索引
	 * @param sqlType 参数的SQL类型
	 * @param inValue 要设置的值 (普通值或 SqlTypeValue)
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, Object inValue)
			throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
	}

	/**
	 * 设置参数的值.
	 * 使用的方法基于参数的SQL类型, 我们可以处理复杂类型, 如数组和LOB.
	 * 
	 * @param ps 预准备语句或可回调语句
	 * @param paramIndex 正在设置的参数的索引
	 * @param sqlType 参数的SQL类型
	 * @param typeName 参数的类型名称 (可选, 仅用于 SQL NULL 和 SqlTypeValue)
	 * @param inValue 要设置的值 (普通值或 SqlTypeValue)
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
			Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
	}

	/**
	 * 设置参数的值.
	 * 使用的方法基于参数的SQL类型, 我们可以处理复杂类型, 如数组和LOB.
	 * 
	 * @param ps 预准备语句或可回调语句
	 * @param paramIndex 正在设置的参数的索引
	 * @param sqlType 参数的SQL类型
	 * @param typeName 参数的类型名称 (可选, 仅用于 SQL NULL 和 SqlTypeValue)
	 * @param scale 小数点后的位数 (对于DECIMAL和NUMERIC类型)
	 * @param inValue 要设置的值 (普通值或 SqlTypeValue)
	 * 
	 * @throws SQLException 如果由PreparedStatement方法抛出
	 */
	private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, int sqlType,
			String typeName, Integer scale, Object inValue) throws SQLException {

		String typeNameToUse = typeName;
		int sqlTypeToUse = sqlType;
		Object inValueToUse = inValue;

		// override type info?
		if (inValue instanceof SqlParameterValue) {
			SqlParameterValue parameterValue = (SqlParameterValue) inValue;
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex +
						", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName());
			}
			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
				sqlTypeToUse = parameterValue.getSqlType();
			}
			if (parameterValue.getTypeName() != null) {
				typeNameToUse = parameterValue.getTypeName();
			}
			inValueToUse = parameterValue.getValue();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
					", parameter value [" + inValueToUse +
					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
		}

		if (inValueToUse == null) {
			setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
		}
		else {
			setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
		}
	}

	/**
	 * 将指定的PreparedStatement参数设置为null, 并遵循特定于数据库的特性.
	 */
	private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException {
		if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
			boolean useSetObject = false;
			Integer sqlTypeToUse = null;
			DatabaseMetaData dbmd = null;
			String jdbcDriverName = null;
			boolean tryGetParameterType = true;

			if (shouldIgnoreGetParameterType == null) {
				try {
					dbmd = ps.getConnection().getMetaData();
					jdbcDriverName = dbmd.getDriverName();
					tryGetParameterType = !driversWithNoSupportForGetParameterType.contains(jdbcDriverName);
					if (tryGetParameterType && jdbcDriverName.startsWith("Oracle")) {
						// 默认情况下, 避免将getParameterType与Oracle 12c驱动程序一起使用:
						// 需要通过 spring.jdbc.getParameterType.ignore=false 显式激活
						tryGetParameterType = false;
						driversWithNoSupportForGetParameterType.add(jdbcDriverName);
					}
				}
				catch (Throwable ex) {
					logger.debug("Could not check connection metadata", ex);
				}
			}
			else {
				tryGetParameterType = !shouldIgnoreGetParameterType;
			}

			if (tryGetParameterType) {
				try {
					sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("JDBC 3.0 getParameterType call not supported - using fallback method instead: " + ex);
					}
				}
			}

			if (sqlTypeToUse == null) {
				// JDBC驱动程序不符合 JDBC 3.0 -> 继续进行特定于数据库的检查
				sqlTypeToUse = Types.NULL;
				try {
					if (dbmd == null) {
						dbmd = ps.getConnection().getMetaData();
					}
					if (jdbcDriverName == null) {
						jdbcDriverName = dbmd.getDriverName();
					}
					if (shouldIgnoreGetParameterType == null) {
						// 注册不支持getParameterType的JDBC驱动程序, 但Oracle 12c驱动程序除外, 其中getParameterType仅对特定语句失败
						// (所以上面抛出的异常并不表示普遍缺乏支持).
						driversWithNoSupportForGetParameterType.add(jdbcDriverName);
					}
					String databaseProductName = dbmd.getDatabaseProductName();
					if (databaseProductName.startsWith("Informix") ||
							(jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
							// "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
						useSetObject = true;
					}
					else if (databaseProductName.startsWith("DB2") ||
							jdbcDriverName.startsWith("jConnect") ||
							jdbcDriverName.startsWith("SQLServer")||
							jdbcDriverName.startsWith("Apache Derby")) {
						sqlTypeToUse = Types.VARCHAR;
					}
				}
				catch (Throwable ex) {
					logger.debug("Could not check connection metadata", ex);
				}
			}
			if (useSetObject) {
				ps.setObject(paramIndex, null);
			}
			else {
				ps.setNull(paramIndex, sqlTypeToUse);
			}
		}
		else if (typeName != null) {
			ps.setNull(paramIndex, sqlType, typeName);
		}
		else {
			ps.setNull(paramIndex, sqlType);
		}
	}

	private static void setValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
			Integer scale, Object inValue) throws SQLException {

		if (inValue instanceof SqlTypeValue) {
			((SqlTypeValue) inValue).setTypeValue(ps, paramIndex, sqlType, typeName);
		}
		else if (inValue instanceof SqlValue) {
			((SqlValue) inValue).setValue(ps, paramIndex);
		}
		else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR ) {
			ps.setString(paramIndex, inValue.toString());
		}
		else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
			ps.setNString(paramIndex, inValue.toString());
		}
		else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
			String strVal = inValue.toString();
			if (strVal.length() > 4000) {
				// 对于较旧的Oracle驱动程序是必需的, 特别是在针对Oracle 10数据库运行时.
				// 由于它使用标准的JDBC 4.0 API, 因此也可以正常使用其他驱动程序/数据库.
				try {
					if (sqlType == Types.NCLOB) {
						ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
					}
					else {
						ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
					}
					return;
				}
				catch (AbstractMethodError err) {
					logger.debug("JDBC driver does not implement JDBC 4.0 'setClob(int, Reader, long)' method", err);
				}
				catch (SQLFeatureNotSupportedException ex) {
					logger.debug("JDBC driver does not support JDBC 4.0 'setClob(int, Reader, long)' method", ex);
				}
			}
			// Fallback: setString 或 setNString绑定
			if (sqlType == Types.NCLOB) {
				ps.setNString(paramIndex, strVal);
			}
			else {
				ps.setString(paramIndex, strVal);
			}
		}
		else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
			if (inValue instanceof BigDecimal) {
				ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
			}
			else if (scale != null) {
				ps.setObject(paramIndex, inValue, sqlType, scale);
			}
			else {
				ps.setObject(paramIndex, inValue, sqlType);
			}
		}
		else if (sqlType == Types.BOOLEAN) {
			if (inValue instanceof Boolean) {
				ps.setBoolean(paramIndex, (Boolean) inValue);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.BOOLEAN);
			}
		}
		else if (sqlType == Types.DATE) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Date) {
					ps.setDate(paramIndex, (java.sql.Date) inValue);
				}
				else {
					ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.DATE);
			}
		}
		else if (sqlType == Types.TIME) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Time) {
					ps.setTime(paramIndex, (java.sql.Time) inValue);
				}
				else {
					ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIME);
			}
		}
		else if (sqlType == Types.TIMESTAMP) {
			if (inValue instanceof java.util.Date) {
				if (inValue instanceof java.sql.Timestamp) {
					ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
				}
				else {
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
				}
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
			}
		}
		else if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER &&
				"Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
			if (isStringValue(inValue.getClass())) {
				ps.setString(paramIndex, inValue.toString());
			}
			else if (isDateValue(inValue.getClass())) {
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
			}
			else if (inValue instanceof Calendar) {
				Calendar cal = (Calendar) inValue;
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				// 回退到没有指定SQL类型的泛型setObject调用.
				ps.setObject(paramIndex, inValue);
			}
		}
		else {
			// 回退到没有指定SQL类型的泛型setObject调用.
			ps.setObject(paramIndex, inValue, sqlType);
		}
	}

	/**
	 * 检查给定值是否可以视为String值.
	 */
	private static boolean isStringValue(Class<?> inValueType) {
		// 将任何CharSequence (包括 StringBuffer 和 StringBuilder)视为 String.
		return (CharSequence.class.isAssignableFrom(inValueType) ||
				StringWriter.class.isAssignableFrom(inValueType));
	}

	/**
	 * 检查给定值是否为{@code java.util.Date} (但不是JDBC特定的子类之一).
	 */
	private static boolean isDateValue(Class<?> inValueType) {
		return (java.util.Date.class.isAssignableFrom(inValueType) &&
				!(java.sql.Date.class.isAssignableFrom(inValueType) ||
						java.sql.Time.class.isAssignableFrom(inValueType) ||
						java.sql.Timestamp.class.isAssignableFrom(inValueType)));
	}

	/**
	 * 清理传递给execute方法的参数值所持有的所有资源. 例如, 这对于关闭LOB值很重要.
	 * 
	 * @param paramValues 提供的参数值. May be {@code null}.
	 */
	public static void cleanupParameters(Object... paramValues) {
		if (paramValues != null) {
			cleanupParameters(Arrays.asList(paramValues));
		}
	}

	/**
	 * 清理传递给execute方法的参数值所持有的所有资源. 例如, 这对于关闭LOB值很重要.
	 * 
	 * @param paramValues 提供的参数值. May be {@code null}.
	 */
	public static void cleanupParameters(Collection<?> paramValues) {
		if (paramValues != null) {
			for (Object inValue : paramValues) {
				if (inValue instanceof DisposableSqlTypeValue) {
					((DisposableSqlTypeValue) inValue).cleanup();
				}
				else if (inValue instanceof SqlValue) {
					((SqlValue) inValue).cleanup();
				}
			}
		}
	}
}
