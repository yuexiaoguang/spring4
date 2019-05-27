package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * 用于在PreparedStatement上运行的代码的通用回调接口.
 * 允许在单个PreparedStatement上执行任意数量的操作,
 * 例如单个{@code executeUpdate}调用或具有不同参数的重复{@code executeUpdate}调用.
 *
 * <p>由JdbcTemplate内部使用, 但对应用程序代码也很有用.
 * 请注意, 传入的PreparedStatement可以由框架或自定义PreparedStatementCreator创建.
 * 但是, 后者几乎不需要, 因为大多数自定义回调操作都会执行更新, 在这种情况下标准的PreparedStatement就可以了.
 * 自定义操作将始终自行设置参数值, 因此也不需要PreparedStatementCreator功能.
 */
public interface PreparedStatementCallback<T> {

	/**
	 * 由{@code JdbcTemplate.execute}使用活动的JDBC PreparedStatement调用.
	 * 不需要关心关闭Statement或Connection, 或关于处理事务:
	 * 这将全部由Spring的JdbcTemplate处理.
	 * <p><b>NOTE:</b> 打开的任何ResultSet应该在回调实现中的finally块中关闭.
	 * Spring将在返回的回调后关闭Statement对象, 但这并不一定意味着将关闭ResultSet资源:
	 * Statement对象可能会被连接池汇集, {@code close}调用只会将对象返回到池中, 但不会物理关闭资源.
	 * <p>如果在没有线程绑定的JDBC事务 (由DataSourceTransactionManager启动)的情况下调用,
	 * 则代码将简单地在JDBC连接上以其事务语义执行.
	 * 如果JdbcTemplate配置为使用支持JTA的DataSource, 那么如果JTA事务处于活动状态, 则JDBC连接以及回调代码将是事务性的.
	 * <p>允许返回在回调中创建的结果对象, i.e. 域对象或域对象的集合.
	 * 请注意, 对单步操作有特殊支持: 请参阅JdbcTemplate.queryForObject等.
	 * 抛出的RuntimeException被视为应用程序异常, 它会传播到模板的调用者.
	 * 
	 * @param ps 活动的JDBC PreparedStatement
	 * 
	 * @return 结果对象, 或{@code null}
	 * @throws SQLException 如果由JDBC方法抛出, 则由SQLExceptionTranslator自动转换为DataAccessException
	 * @throws DataAccessException 如果是自定义异常
	 */
	T doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;

}
