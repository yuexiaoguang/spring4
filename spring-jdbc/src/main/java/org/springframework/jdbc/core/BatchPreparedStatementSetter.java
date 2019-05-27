package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}类使用的批量更新回调接口.
 *
 * <p>此接口为JdbcTemplate类提供的{@link java.sql.PreparedStatement}设置值,
 * 对于使用相同SQL的批处理中的每个更新. 实现负责设置任何必要的参数.
 * 已经提供了带占位符的SQL.
 *
 * <p>实现<i>不</i>需要关注可能从他们尝试的操作中抛出的SQLExceptions.
 * JdbcTemplate类将适当地捕获和处理SQLExceptions.
 */
public interface BatchPreparedStatementSetter {

	/**
	 * 在给定的PreparedStatement上设置参数值.
	 * 
	 * @param ps 用于调用setter方法的PreparedStatement
	 * @param i 在批处理中发出的语句的索引, 从0开始
	 * 
	 * @throws SQLException 如果遇到SQLException (i.e. 不需要捕获SQLException)
	 */
	void setValues(PreparedStatement ps, int i) throws SQLException;

	/**
	 * 返回批次的大小.
	 * 
	 * @return 批处理中的语句数量
	 */
	int getBatchSize();

}
