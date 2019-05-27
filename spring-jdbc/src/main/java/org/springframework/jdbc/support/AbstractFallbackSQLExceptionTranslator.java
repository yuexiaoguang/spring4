package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.util.Assert;

/**
 * {@link SQLExceptionTranslator}实现的基类, 允许回退到其他一些{@link SQLExceptionTranslator}.
 */
public abstract class AbstractFallbackSQLExceptionTranslator implements SQLExceptionTranslator {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private SQLExceptionTranslator fallbackTranslator;


	/**
	 * 覆盖默认的SQL状态回退转换器 (通常是{@link SQLStateSQLExceptionTranslator}).
	 */
	public void setFallbackTranslator(SQLExceptionTranslator fallback) {
		this.fallbackTranslator = fallback;
	}

	/**
	 * 如果有的话, 返回后备异常转换器.
	 */
	public SQLExceptionTranslator getFallbackTranslator() {
		return this.fallbackTranslator;
	}


	/**
	 * 预先检查参数, 调用{@link #doTranslate}, 并在必要时调用{@link #getFallbackTranslator() 后备转换器}.
	 */
	@Override
	public DataAccessException translate(String task, String sql, SQLException ex) {
		Assert.notNull(ex, "Cannot translate a null SQLException");
		if (task == null) {
			task = "";
		}
		if (sql == null) {
			sql = "";
		}

		DataAccessException dae = doTranslate(task, sql, ex);
		if (dae != null) {
			// 找到特定的异常匹配.
			return dae;
		}

		// 寻找后备...
		SQLExceptionTranslator fallback = getFallbackTranslator();
		if (fallback != null) {
			dae = fallback.translate(task, sql, ex);
			if (dae != null) {
				// Fallback exception match found.
				return dae;
			}
		}

		// 无法更准确地识别它.
		return new UncategorizedSQLException(task, sql, ex);
	}

	/**
	 * 用于实际转换给定异常的模板方法.
	 * <p>传入的参数将被预先检查.
	 * 此外, 允许此方法返回{@code null}以指示未找到任何异常匹配, 并且应该启用回退转换.
	 * 
	 * @param task 描述正在尝试的任务的可读文本
	 * @param sql 导致问题的SQL查询或更新
	 * @param ex 有问题的{@code SQLException}
	 * 
	 * @return DataAccessException, 包装{@code SQLException}; 如果未找到异常匹配, 则为{@code null}
	 */
	protected abstract DataAccessException doTranslate(String task, String sql, SQLException ex);


	/**
	 * 为给定的{@link java.sql.SQLException}构建消息{@code String}.
	 * <p>在创建泛型{@link org.springframework.dao.DataAccessException}类的实例时, 由转换器子类调用.
	 * 
	 * @param task 描述正在尝试的任务的可读文本
	 * @param sql 导致问题的SQL语句
	 * @param ex 有问题的{@code SQLException}
	 * 
	 * @return 要使用的消息{@code String}
	 */
	protected String buildMessage(String task, String sql, SQLException ex) {
		return task + "; SQL [" + sql + "]; " + ex.getMessage();
	}

}
