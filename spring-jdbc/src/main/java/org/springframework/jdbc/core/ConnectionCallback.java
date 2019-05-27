package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * 用于在JDBC连接上操作的代码的通用回调接口.
 * 允许使用任何类型和数量的语句在单个Connection上执行任意数量的操作.
 *
 * <p>这对于委托给希望Connection工作并抛出SQLException的现有数据访问代码特别有用.
 * 对于新编写的代码, 强烈建议使用JdbcTemplate的更具体的操作, 例如{@code query}或{@code update}变体.
 */
public interface ConnectionCallback<T> {

	/**
	 * 由{@code JdbcTemplate.execute}使用活动的JDBC连接调用.
	 * 无需关心激活或关闭Connection, 或处理事务.
	 * <p>如果在没有线程绑定的JDBC事务 (由DataSourceTransactionManager启动)的情况下调用,
	 * 则代码将简单地在JDBC连接上以其事务语义执行.
	 * 如果JdbcTemplate配置为使用支持JTA的DataSource,
	 * 那么如果JTA事务处于活动状态, 则JDBC Connection和回调代码将是事务性的.
	 * <p>允许返回在回调中创建的结果对象, i.e. 域对象或域对象的集合.
	 * 请注意, 对单步操作有特殊支持: see {@code JdbcTemplate.queryForObject} etc.
	 * 抛出的RuntimeException被视为应用程序异常: 它会传播到模板的调用者.
	 * 
	 * @param con 活动的JDBC连接
	 * 
	 * @return 结果对象, 或{@code null}
	 * @throws SQLException 如果由JDBC方法抛出, 则由SQLExceptionTranslator自动转换为DataAccessException
	 * @throws DataAccessException 如果是自定义异常
	 */
	T doInConnection(Connection con) throws SQLException, DataAccessException;

}
