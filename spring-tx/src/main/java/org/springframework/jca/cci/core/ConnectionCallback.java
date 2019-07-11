package org.springframework.jca.cci.core;

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

import org.springframework.dao.DataAccessException;

/**
 * 用于在CCI连接上运行的代码的通用回调接口.
 * 允许使用任何类型和数量的Interaction在单个Connection上执行任意数量的操作.
 *
 * <p>这对于委托给希望Connection工作并抛出ResourceException的现有数据访问代码特别有用.
 * 对于新编写的代码, 强烈建议使用CciTemplate更具体的{@code execute}变体.
 */
public interface ConnectionCallback<T> {

	/**
	 * 由{@code CciTemplate.execute}通过活动的CCI连接调用.
	 * 无需关心激活或关闭Connection, 还是处理事务.
	 * <p>如果在没有线程绑定的CCI事务 (由CciLocalTransactionManager启动)的情况下调用,
	 * 则代码将简单地在CCI连接上以其事务语义执行.
	 * 如果CciTemplate配置为使用支持JTA的ConnectionFactory,
	 * 那么如果JTA事务处于活动状态, 则CCI连接以及回调代码将是事务性的.
	 * <p>允许返回在回调中创建的结果对象, i.e. 域对象或域对象的集合.
	 * 请注意, 对单步操作有特殊支持: 请参阅{@code CciTemplate.execute}变体.
	 * 抛出的RuntimeException被视为应用程序异常: 它会传播到模板的调用者.
	 * 
	 * @param connection 活动的CCI Connection
	 * @param connectionFactory 创建Connection的CCI ConnectionFactory (允许访问RecordFactory和ResourceAdapterMetaData)
	 * 
	 * @return 结果对象, 或{@code null}
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 * @throws SQLException 如果由ResultSet方法抛出, 则自动转换为DataAccessException
	 * @throws DataAccessException 自定义异常
	 */
	T doInConnection(Connection connection, ConnectionFactory connectionFactory)
			throws ResourceException, SQLException, DataAccessException;

}
