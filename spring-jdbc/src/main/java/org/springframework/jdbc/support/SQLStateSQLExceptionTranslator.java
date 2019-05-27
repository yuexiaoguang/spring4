package org.springframework.jdbc.support;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * {@link SQLExceptionTranslator}实现, 根据前两位数字 (SQL状态 "class")分析{@link SQLException}中的SQL状态.
 * 检测标准SQL状态值和已知的特定于供应商的SQL状态.
 *
 * <p>无法诊断所有问题, 但可以在数据库之间移植, 不需要特殊初始化 (没有数据库供应商检测等.).
 * 要获得更精确的转换, 考虑{@link SQLErrorCodeSQLExceptionTranslator}.
 */
public class SQLStateSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

	private static final Set<String> BAD_SQL_GRAMMAR_CODES = new HashSet<String>(8);

	private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = new HashSet<String>(8);

	private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = new HashSet<String>(8);

	private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = new HashSet<String>(8);

	private static final Set<String> CONCURRENCY_FAILURE_CODES = new HashSet<String>(4);


	static {
		BAD_SQL_GRAMMAR_CODES.add("07");	// 动态SQL错误
		BAD_SQL_GRAMMAR_CODES.add("21");	// 基数冲突
		BAD_SQL_GRAMMAR_CODES.add("2A");	// 语法错误直接SQL
		BAD_SQL_GRAMMAR_CODES.add("37");	// 语法错误动态SQL
		BAD_SQL_GRAMMAR_CODES.add("42");	// 一般SQL语法错误
		BAD_SQL_GRAMMAR_CODES.add("65");	// Oracle: 未知标识符

		DATA_INTEGRITY_VIOLATION_CODES.add("01");	// 数据截断
		DATA_INTEGRITY_VIOLATION_CODES.add("02");	// 没有找到数据
		DATA_INTEGRITY_VIOLATION_CODES.add("22");	// 值超出范围
		DATA_INTEGRITY_VIOLATION_CODES.add("23");	// 完整性约束违规
		DATA_INTEGRITY_VIOLATION_CODES.add("27");	// 触发的数据更改违规
		DATA_INTEGRITY_VIOLATION_CODES.add("44");	// 违规检查

		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("08");	 // 连接异常
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("53");	 // PostgreSQL: 资源不足 (e.g. 磁盘已满)
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("54");	 // PostgreSQL: 程序限制超出 (e.g. 语句过于复杂)
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("57");	 // DB2: 内存不足/数据库未启动
		DATA_ACCESS_RESOURCE_FAILURE_CODES.add("58");	 // DB2: 意外的系统错误

		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JW");	 // Sybase: 内部I/O错误
		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JZ");	 // Sybase: 意外的I/O 错误
		TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("S1");	 // DB2: 沟通失败

		CONCURRENCY_FAILURE_CODES.add("40");	// 事务回滚
		CONCURRENCY_FAILURE_CODES.add("61");	// Oracle: 死锁
	}


	@Override
	protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
		// First, the getSQLState check...
		String sqlState = getSqlState(ex);
		if (sqlState != null && sqlState.length() >= 2) {
			String classCode = sqlState.substring(0, 2);
			if (logger.isDebugEnabled()) {
				logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
			}
			if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
				return new BadSqlGrammarException(task, sql, ex);
			}
			else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
				return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
			}
			else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
				return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
			}
			else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
				return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
			}
			else if (CONCURRENCY_FAILURE_CODES.contains(classCode)) {
				return new ConcurrencyFailureException(buildMessage(task, sql, ex), ex);
			}
		}

		// For MySQL: 指示超时的异常类名称?
		// (因为MySQL不会抛出JDBC 4 SQLTimeoutException)
		if (ex.getClass().getName().contains("Timeout")) {
			return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
		}

		// 无法解决任何问题 - 诉诸于UncategorizedSQLException.
		return null;
	}

	/**
	 * 从提供的{@link SQLException 异常}获取SQL状态代码.
	 * <p>一些JDBC驱动程序嵌套来自批处理更新的实际异常, 因此可能需要深入研究嵌套异常.
	 * 
	 * @param ex 要从中提取{@link SQLException#getSQLState() SQL状态}的异常
	 * 
	 * @return SQL状态代码
	 */
	private String getSqlState(SQLException ex) {
		String sqlState = ex.getSQLState();
		if (sqlState == null) {
			SQLException nestedEx = ex.getNextException();
			if (nestedEx != null) {
				sqlState = nestedEx.getSQLState();
			}
		}
		return sqlState;
	}
}
