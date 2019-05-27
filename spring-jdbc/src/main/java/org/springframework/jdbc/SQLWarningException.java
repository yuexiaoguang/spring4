package org.springframework.jdbc;

import java.sql.SQLWarning;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * 当不忽略{@link java.sql.SQLWarning SQLWarnings}时抛出异常.
 *
 * <p>如果报告了SQLWarning, 则操作已完成, 因此如果我们在查看警告时不满意, 则需要明确回滚.
 * 我们可能会选择忽略 (并记录)警告, 或者将其包装并以此SQLWarningException的形式抛出.
 */
@SuppressWarnings("serial")
public class SQLWarningException extends UncategorizedDataAccessException {

	/**
	 * @param msg 详细信息
	 * @param ex JDBC警告
	 */
	public SQLWarningException(String msg, SQLWarning ex) {
		super(msg, ex);
	}

	/**
	 * 返回底层SQLWarning.
	 */
	public SQLWarning SQLWarning() {
		return (SQLWarning) getCause();
	}

}
