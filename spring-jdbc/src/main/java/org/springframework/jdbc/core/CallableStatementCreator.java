package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JdbcTemplate类使用的三个核心回调接口之一.
 * 此接口在给定连接的情况下创建CallableStatement, 由JdbcTemplate类提供.
 * 实现负责提供SQL和任何必要的参数.
 *
 * <p>实现<i>不</i>需要关注可能从他们尝试的操作中抛出的SQLExceptions.
 * JdbcTemplate类将适当地捕获和处理SQLExceptions.
 *
 * <p>如果PreparedStatementCreator能够提供它用于PreparedStatement创建的SQL, 它还应该实现SqlProvider接口.
 * 如果出现异常, 这可以提供更好的上下文信息.
 */
public interface CallableStatementCreator {

	/**
	 * 在此连接中创建可调用语句. 允许实现使用CallableStatements.
	 * 
	 * @param con 用于创建语句的连接
	 * 
	 * @return 一个可调用的语句
	 * @throws SQLException 没有必要捕获可能在此方法的实现中抛出的SQLException.
	 * JdbcTemplate类将处理它们.
	 */
	CallableStatement createCallableStatement(Connection con) throws SQLException;

}
