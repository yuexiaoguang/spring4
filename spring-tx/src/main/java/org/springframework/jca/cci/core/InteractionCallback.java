package org.springframework.jca.cci.core;

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.Interaction;

import org.springframework.dao.DataAccessException;

/**
 * 用于在CCI Interaction上运行的代码的通用回调接口.
 * 允许在单个Interaction上执行任意数量的操作, 例如单个执行调用或具有不同参数的重复执行调用.
 *
 * <p>这对于委托希望Interaction工作并抛出ResourceException的现有数据访问代码特别有用.
 * 对于新编写的代码, 强烈建议使用CciTemplate更具体的{@code execute}变体.
 */
public interface InteractionCallback<T> {

	/**
	 * 由{@code CciTemplate.execute}通过活动的CCI Interaction进行调用.
	 * 不需要关心激活或关闭交互, 或处理事务.
	 * <p>如果在没有线程绑定的CCI事务 (由CciLocalTransactionManager启动)的情况下调用,
	 * 则代码将简单地在CCI Interaction上以其事务语义执行.
	 * 如果CciTemplate配置为使用支持JTA的ConnectionFactory, 那么如果JTA事务处于活动状态, 则CCI Interaction和回调代码将是事务性的.
	 * <p>允许返回在回调中创建的结果对象, i.e. 域对象或域对象的集合.
	 * 请注意, 对单步操作有特殊支持: 请参阅{@code CciTemplate.execute}变体.
	 * 抛出的RuntimeException被视为应用程序异常: 它会传播到模板的调用者.
	 * 
	 * @param interaction 活动的CCI Interaction
	 * @param connectionFactory 创建Connection的CCI ConnectionFactory (允许访问RecordFactory和ResourceAdapterMetaData)
	 * 
	 * @return 结果对象, 或{@code null}
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 * @throws SQLException 如果由ResultSet方法抛出, 则自动转换为DataAccessException
	 * @throws DataAccessException 自定义异常
	 */
	T doInInteraction(Interaction interaction, ConnectionFactory connectionFactory)
			throws ResourceException, SQLException, DataAccessException;

}
