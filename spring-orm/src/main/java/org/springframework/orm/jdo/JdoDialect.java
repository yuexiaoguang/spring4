package org.springframework.orm.jdo;

import java.sql.SQLException;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * SPI策略, 允许自定义与特定JDO提供者的集成, 特别是关于事务管理和异常转换.
 * 为特定的JDO提供商实现, 例如 JPOX, Kodo, Lido, Versant Open Access.
 *
 * <p>JDO 3.0为此处涉及的大多数功能定义了标准方法.
 * 因此, Spring的{@link DefaultJdoDialect}默认使用相应的JDO 3.0方法, 必要时以特定于供应商的方式覆盖.
 * 特殊事务语义和更复杂的异常转换仍然需要{@link DefaultJdoDialect}的特定于供应商的子类.
 *
 * <p>通常, 建议从{@link DefaultJdoDialect}派生, 而不是直接实现此接口.
 * 这允许继承{@link DefaultJdoDialect}的常见行为(现在和将来), 只覆盖特定的钩子以插入具体的供应商特定行为.
 */
public interface JdoDialect {

	//-------------------------------------------------------------------------
	// Hooks for transaction management (used by JdoTransactionManager)
	//-------------------------------------------------------------------------

	/**
	 * 开始给定的JDO事务, 应用给定Spring事务定义指定的语义 (特别是隔离级别和超时).
	 * 由JdoTransactionManager在事务开始时调用.
	 * <p>实现可以配置JDO Transaction对象, 然后调用{@code begin}, 或调用一个特殊的begin方法, 例如采用隔离级别.
	 * <p>在开始事务之前, 实现还可以将只读标志和隔离级别应用于底层JDBC连接.
	 * 在这种情况下, 可以返回一个事务数据对象, 该对象保存先前的隔离级别 (可能还有其他数据), 以便在{@code cleanupTransaction}中重置.
	 * <p>实现还可以使用传入的TransactionDefinition公开的Spring事务名称, 来优化特定的数据访问用例 (有效地使用当前事务名称作为用例标识符).
	 * 
	 * @param transaction 要开始的JDO事务
	 * @param definition 定义语义的Spring事务定义
	 * 
	 * @return 保存事务数据的任意对象 (传递给cleanupTransaction)
	 * @throws JDOException 如果由JDO方法抛出
	 * @throws SQLException 如果被JDBC方法抛出
	 * @throws TransactionException 如果参数无效
	 */
	Object beginTransaction(Transaction transaction, TransactionDefinition definition)
			throws JDOException, SQLException, TransactionException;

	/**
	 * 通过给定的事务数据清理事务.
	 * 由JdoTransactionManager在事务清理上调用.
	 * <p>例如, 实现可以重置底层JDBC连接的只读标志和隔离级别.
	 * 此外, 可以在此重置暴露的数据访问用例.
	 * 
	 * @param transactionData 保存事务数据的任意对象 (由beginTransaction返回)
	 */
	void cleanupTransaction(Object transactionData);

	/**
	 * 如果访问关系数据库, 则检索给定JDO PersistenceManager在其下使用的JDBC连接.
	 * 如果实际需要访问底层JDBC连接, 通常在活动的JDO事务中 (例如, 通过JdoTransactionManager), 将调用此方法.
	 * 当不再需要时, 返回的句柄将被传递到{@code releaseJdbcConnection}方法.
	 * <p>鼓励实现返回一个未包装的Connection对象, 即从连接池中获取Connection.
	 * 这使得应用程序代码更容易获得底层本机JDBC连接, 如OracleConnection, 这有时是LOB处理等所必需的.
	 * 假设调用代码知道如何正确处理返回的Connection对象.
	 * <p>在一个简单的情况下, 返回的Connection将使用PersistenceManager自动关闭,
	 * 或者可以通过Connection对象本身释放, 实现可以返回一个只包含Connection的SimpleConnectionHandle.
	 * 如果{@code releaseJdbcConnection}中需要其他对象, 则实现应该使用引用其他对象的特殊句柄.
	 * 
	 * @param pm 当前的JDO PersistenceManager
	 * @param readOnly 是否仅出于只读目的需要Connection
	 * 
	 * @return JDBC Connection的句柄, 传递到{@code releaseJdbcConnection}, 或{@code null} 如果不能检索JDBC连接
	 * @throws JDOException 如果由JDO方法抛出
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	ConnectionHandle getJdbcConnection(PersistenceManager pm, boolean readOnly)
			throws JDOException, SQLException;

	/**
	 * 释放最初通过{@code getJdbcConnection}检索的给定JDBC连接.
	 * 在任何情况下都应该调用它, 以允许正确释放检索到的Connection句柄.
	 * <p>如果在JDO事务完成或PersistenceManager关闭时, 隐式关闭{@code getJdbcConnection}返回的Connection,
	 * 则实现可能根本不执行任何操作.
	 * 
	 * @param conHandle 要释放的JDBC Connection句柄
	 * @param pm 当前的JDO PersistenceManager
	 * 
	 * @throws JDOException 如果由JDO方法抛出
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
			throws JDOException, SQLException;


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JdoTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * 将给定的JDOException转换为Spring的通用DataAccessException层次结构中的相应异常.
	 * 如果不能做更具体的事情, 实现应该应用PersistenceManagerFactoryUtils的标准异常转换.
	 * <p>特别重要的是正确转换为DataIntegrityViolationException, 例如在约束违规时.
	 * 遗憾的是, 标准JDO不允许对此进行便携式检测.
	 * <p>可以使用SQLExceptionTranslator以特定于数据库的方式转换底层SQLExceptions.
	 * 
	 * @param ex 要抛出的JDOException
	 * 
	 * @return 相应的DataAccessException (不能是{@code null})
	 */
	DataAccessException translateException(JDOException ex);

}
