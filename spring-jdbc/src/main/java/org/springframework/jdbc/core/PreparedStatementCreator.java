package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的两个核心回调接口之一.
 * 此接口创建由JDBCTemplate类提供的给定连接的PreparedStatement.
 * 实现负责提供SQL和任何必要的参数.
 *
 * <p>实现<i>不</i>需要关注可能从他们尝试的操作中抛出的SQLExceptions.
 * JdbcTemplate类将适当地捕获和处理SQLExceptions.
 *
 * <p>如果PreparedStatementCreator能够提供用于PreparedStatement创建的SQL, 它还应该实现SqlProvider接口.
 * 如果出现异常, 这可以提供更好的上下文信息.
 */
public interface PreparedStatementCreator {

	/**
	 * 在此连接中创建语句. 允许实现使用PreparedStatements. JdbcTemplate将关闭创建的语句.
	 * 
	 * @param con 用于创建语句的Connection
	 * 
	 * @return 准备好的声明
	 * @throws SQLException 没有必要捕获可能在此方法的实现中抛出的SQLException.
	 * JdbcTemplate类将处理它们.
	 */
	PreparedStatement createPreparedStatement(Connection con) throws SQLException;

}
