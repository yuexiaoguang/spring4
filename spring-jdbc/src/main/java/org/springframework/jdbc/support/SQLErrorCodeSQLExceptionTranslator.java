package org.springframework.jdbc.support;

import java.lang.reflect.Constructor;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * {@link SQLExceptionTranslator}的实现, 用于分析特定于供应商的错误代码.
 * 比基于SQL状态的实现更精确, 但是特定于供应商.
 *
 * <p>此类适用以下匹配规则:
 * <ul>
 * <li>尝试由任何子类实现的自定义转换. 请注意, 此类是具体的, 通常自己使用, 在这种情况下, 此规则不适用.
 * <li>应用错误代码匹配. 默认情况下, 从SQLErrorCodesFactory获取错误代码.
 * 此工厂从类路径加载"sql-error-codes.xml"文件, 从数据库元数据定义数据库名称的错误代码映射.
 * <li>回退到后备转换器.
 * {@link SQLStateSQLExceptionTranslator}是默认的回退转换器, 仅分析异常的SQL状态.
 * 在引入自己的{@code SQLException}子类层次结构的Java 6上, 默认情况下将使用{@link SQLExceptionSubclassTranslator},
 * 当没有遇到特定的子类时, 它会反过来回到Spring自己的SQL状态转换.
 * </ul>
 *
 * <p>默认情况下, 从此包读取名为"sql-error-codes.xml"的配置文件.
 * 只要从同一个ClassLoader加载Spring JDBC包, 就可以通过类路径根目录中的同名文件 (e.g. 在"/WEB-INF/classes"目录中)覆盖它.
 */
public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

	private static final int MESSAGE_ONLY_CONSTRUCTOR = 1;
	private static final int MESSAGE_THROWABLE_CONSTRUCTOR = 2;
	private static final int MESSAGE_SQLEX_CONSTRUCTOR = 3;
	private static final int MESSAGE_SQL_THROWABLE_CONSTRUCTOR = 4;
	private static final int MESSAGE_SQL_SQLEX_CONSTRUCTOR = 5;


	/** 此转换器使用的错误代码 */
	private SQLErrorCodes sqlErrorCodes;


	/**
	 * 必须设置的SqlErrorCodes或DataSource属性.
	 */
	public SQLErrorCodeSQLExceptionTranslator() {
		setFallbackTranslator(new SQLExceptionSubclassTranslator());
	}

	/**
	 * 创建给定DataSource的SQL错误代码转换器.
	 * 调用此构造函数将导致从DataSource获取Connection以获取元数据.
	 * 
	 * @param dataSource 用于查找元数据并确定哪些错误代码可用的DataSource
	 */
	public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
		this();
		setDataSource(dataSource);
	}

	/**
	 * 创建给定的数据库产品名称的SQL错误代码转换器.
	 * 调用此构造函数将导致从DataSource获取Connection以获取元数据.
	 * 
	 * @param dbName 标识错误代码条目的数据库产品名称
	 */
	public SQLErrorCodeSQLExceptionTranslator(String dbName) {
		this();
		setDatabaseProductName(dbName);
	}

	/**
	 * 给出这些错误代码, 创建一个SQLErrorCode转换器.
	 * 不需要使用连接执行数据库元数据查找.
	 * 
	 * @param sec 错误代码
	 */
	public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
		this();
		this.sqlErrorCodes = sec;
	}


	/**
	 * 设置此转换器的DataSource.
	 * <p>设置此属性将导致从DataSource获取Connection以获取元数据.
	 * 
	 * @param dataSource 用于查找元数据并确定哪些错误代码可用的DataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
	}

	/**
	 * 设置此转换器的数据库产品名称.
	 * <p>设置此属性将避免从DataSource获取连接以获取元数据.
	 * 
	 * @param dbName 标识错误代码条目的数据库产品名称
	 */
	public void setDatabaseProductName(String dbName) {
		this.sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(dbName);
	}

	/**
	 * 设置要用于转换的自定义错误代码.
	 * 
	 * @param sec 要使用的自定义错误代码
	 */
	public void setSqlErrorCodes(SQLErrorCodes sec) {
		this.sqlErrorCodes = sec;
	}

	/**
	 * 返回此转换器使用的错误代码.
	 * 通常通过DataSource确定.
	 */
	public SQLErrorCodes getSqlErrorCodes() {
		return this.sqlErrorCodes;
	}


	@Override
	protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
		SQLException sqlEx = ex;
		if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
			SQLException nestedSqlEx = sqlEx.getNextException();
			if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
				logger.debug("Using nested SQLException from the BatchUpdateException");
				sqlEx = nestedSqlEx;
			}
		}

		// 首先, 尝试从重写方法进行自定义转换.
		DataAccessException dex = customTranslate(task, sql, sqlEx);
		if (dex != null) {
			return dex;
		}

		// 接下来, 尝试使用自定义SQLException转换器.
		if (this.sqlErrorCodes != null) {
			SQLExceptionTranslator customTranslator = this.sqlErrorCodes.getCustomSqlExceptionTranslator();
			if (customTranslator != null) {
				DataAccessException customDex = customTranslator.translate(task, sql, sqlEx);
				if (customDex != null) {
					return customDex;
				}
			}
		}

		// 检查SQLErrorCodes以及相应的错误代码.
		if (this.sqlErrorCodes != null) {
			String errorCode;
			if (this.sqlErrorCodes.isUseSqlStateForTranslation()) {
				errorCode = sqlEx.getSQLState();
			}
			else {
				// 尝试使用实际错误代码查找SQLException, 循环查看原因.
				// E.g. 从JDK 1.6开始适用于java.sql.DataTruncation.
				SQLException current = sqlEx;
				while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException) {
					current = (SQLException) current.getCause();
				}
				errorCode = Integer.toString(current.getErrorCode());
			}

			if (errorCode != null) {
				// 首先查找定义的自定义转换.
				CustomSQLErrorCodesTranslation[] customTranslations = this.sqlErrorCodes.getCustomTranslations();
				if (customTranslations != null) {
					for (CustomSQLErrorCodesTranslation customTranslation : customTranslations) {
						if (Arrays.binarySearch(customTranslation.getErrorCodes(), errorCode) >= 0) {
							if (customTranslation.getExceptionClass() != null) {
								DataAccessException customException = createCustomException(
										task, sql, sqlEx, customTranslation.getExceptionClass());
								if (customException != null) {
									logTranslation(task, sql, sqlEx, true);
									return customException;
								}
							}
						}
					}
				}
				// 接下来, 查找分组的错误代码.
				if (Arrays.binarySearch(this.sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new BadSqlGrammarException(task, sql, sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new InvalidResultSetAccessException(task, sql, sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DuplicateKeyException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new DeadlockLoserDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
				}
				else if (Arrays.binarySearch(this.sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
					logTranslation(task, sql, sqlEx, false);
					return new CannotSerializeTransactionException(buildMessage(task, sql, sqlEx), sqlEx);
				}
			}
		}

		// 无法更精确地识别它 - 把它交给SQLState回退转换器.
		if (logger.isDebugEnabled()) {
			String codes;
			if (this.sqlErrorCodes != null && this.sqlErrorCodes.isUseSqlStateForTranslation()) {
				codes = "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode();
			}
			else {
				codes = "Error code '" + sqlEx.getErrorCode() + "'";
			}
			logger.debug("Unable to translate SQLException with " + codes + ", will now try the fallback translator");
		}

		return null;
	}

	/**
	 * 子类可以重写此方法以尝试从SQLException到DataAccessException的自定义映射.
	 * 
	 * @param task 描述正在尝试的任务的可读文本
	 * @param sql 导致问题的SQL查询或更新. May be {@code null}.
	 * @param sqlEx 违规的SQLException
	 * 
	 * @return null 如果没有可能的自定义转换, 否则由自定义转换产生DataAccessException.
	 * 此异常应包括sqlEx参数作为嵌套的根本原因.
	 * 此实现始终返回null, 这意味着转换器始终回退到默认错误代码.
	 */
	protected DataAccessException customTranslate(String task, String sql, SQLException sqlEx) {
		return null;
	}

	/**
	 * 基于CustomSQLErrorCodesTranslation定义中的给定异常类创建自定义DataAccessException.
	 * 
	 * @param task 描述正在尝试的任务的可读文本
	 * @param sql 导致问题的SQL查询或更新. May be {@code null}.
	 * @param sqlEx 违规的SQLException
	 * @param exceptionClass 要使用的异常类, 如CustomSQLErrorCodesTranslation定义中所定义
	 * 
	 * @return null 如果无法创建自定义异常, 否则生成DataAccessException.
	 * 此异常应包括sqlEx参数作为嵌套的根本原因.
	 */
	protected DataAccessException createCustomException(
			String task, String sql, SQLException sqlEx, Class<?> exceptionClass) {

		// 查找合适的构造器
		try {
			int constructorType = 0;
			Constructor<?>[] constructors = exceptionClass.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length == 1 && String.class == parameterTypes[0]) {
					if (constructorType < MESSAGE_ONLY_CONSTRUCTOR)
						constructorType = MESSAGE_ONLY_CONSTRUCTOR;
				}
				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
						Throwable.class == parameterTypes[1]) {
					if (constructorType < MESSAGE_THROWABLE_CONSTRUCTOR)
						constructorType = MESSAGE_THROWABLE_CONSTRUCTOR;
				}
				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
						SQLException.class == parameterTypes[1]) {
					if (constructorType < MESSAGE_SQLEX_CONSTRUCTOR)
						constructorType = MESSAGE_SQLEX_CONSTRUCTOR;
				}
				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
						String.class == parameterTypes[1] && Throwable.class == parameterTypes[2]) {
					if (constructorType < MESSAGE_SQL_THROWABLE_CONSTRUCTOR)
						constructorType = MESSAGE_SQL_THROWABLE_CONSTRUCTOR;
				}
				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
						String.class == parameterTypes[1] && SQLException.class == parameterTypes[2]) {
					if (constructorType < MESSAGE_SQL_SQLEX_CONSTRUCTOR)
						constructorType = MESSAGE_SQL_SQLEX_CONSTRUCTOR;
				}
			}

			// invoke constructor
			Constructor<?> exceptionConstructor;
			switch (constructorType) {
				case MESSAGE_SQL_SQLEX_CONSTRUCTOR:
					Class<?>[] messageAndSqlAndSqlExArgsClass = new Class<?>[] {String.class, String.class, SQLException.class};
					Object[] messageAndSqlAndSqlExArgs = new Object[] {task, sql, sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndSqlExArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndSqlExArgs);
				case MESSAGE_SQL_THROWABLE_CONSTRUCTOR:
					Class<?>[] messageAndSqlAndThrowableArgsClass = new Class<?>[] {String.class, String.class, Throwable.class};
					Object[] messageAndSqlAndThrowableArgs = new Object[] {task, sql, sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndThrowableArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndThrowableArgs);
				case MESSAGE_SQLEX_CONSTRUCTOR:
					Class<?>[] messageAndSqlExArgsClass = new Class<?>[] {String.class, SQLException.class};
					Object[] messageAndSqlExArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlExArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlExArgs);
				case MESSAGE_THROWABLE_CONSTRUCTOR:
					Class<?>[] messageAndThrowableArgsClass = new Class<?>[] {String.class, Throwable.class};
					Object[] messageAndThrowableArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
					exceptionConstructor = exceptionClass.getConstructor(messageAndThrowableArgsClass);
					return (DataAccessException)exceptionConstructor.newInstance(messageAndThrowableArgs);
				case MESSAGE_ONLY_CONSTRUCTOR:
					Class<?>[] messageOnlyArgsClass = new Class<?>[] {String.class};
					Object[] messageOnlyArgs = new Object[] {task + ": " + sqlEx.getMessage()};
					exceptionConstructor = exceptionClass.getConstructor(messageOnlyArgsClass);
					return (DataAccessException) exceptionConstructor.newInstance(messageOnlyArgs);
				default:
					if (logger.isWarnEnabled()) {
						logger.warn("Unable to find appropriate constructor of custom exception class [" +
								exceptionClass.getName() + "]");
					}
					return null;
				}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unable to instantiate custom exception class [" + exceptionClass.getName() + "]", ex);
			}
			return null;
		}
	}

	private void logTranslation(String task, String sql, SQLException sqlEx, boolean custom) {
		if (logger.isDebugEnabled()) {
			String intro = custom ? "Custom translation of" : "Translating";
			logger.debug(intro + " SQLException with SQL state '" + sqlEx.getSQLState() +
					"', error code '" + sqlEx.getErrorCode() + "', message [" + sqlEx.getMessage() +
					"]; SQL was [" + sql + "] for task [" + task + "]");
		}
	}
}
