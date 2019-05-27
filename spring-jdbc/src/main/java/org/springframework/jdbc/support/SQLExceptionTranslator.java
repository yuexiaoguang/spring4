package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * 用于在{@link SQLException SQLExceptions}
 * 和Spring的数据访问策略不可知的{@link DataAccessException}层次结构之间进行转换的策略接口.
 *
 * <p>实现可以是通用的 (例如, 使用JDBC的{@link java.sql.SQLException#getSQLState() SQLState}代码)
 * 或完全专有 (例如, 使用Oracle错误代码)以获得更高的精度.
 */
public interface SQLExceptionTranslator {

	/**
	 * 将给定的{@link SQLException}转换为通用{@link DataAccessException}.
	 * <p>返回的DataAccessException应该包含原始的{@code SQLException}作为根本原因.
	 * 但是, 客户端代码通常可能不依赖于此, 因为DataAccessExceptions可能也是由其他资源API引起的.
	 * 也就是说, 当期望基于JDBC的访问发生时, {@code getRootCause() instanceof SQLException}检查 (以及后续的强制转换)被认为是可靠的.
	 * 
	 * @param task 描述正在尝试的任务的可读文本
	 * @param sql 导致问题的SQL查询或更新 (may be {@code null})
	 * @param ex 有问题的{@code SQLException}
	 * 
	 * @return DataAccessException, 包装{@code SQLException}
	 */
	DataAccessException translate(String task, String sql, SQLException ex);

}
