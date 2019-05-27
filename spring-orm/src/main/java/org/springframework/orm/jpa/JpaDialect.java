package org.springframework.orm.jpa;

import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * SPI策略, 封装标准JPA 2.0不提供的某些功能, 例如访问底层JDBC连接.
 * 此策略主要用于JPA提供者的独立使用; 使用JTA事务运行时, 其大多数功能都不相关.
 *
 * <p>通常, 建议从{@link DefaultJpaDialect}派生而不是直接实现此接口.
 * 这允许继承DefaultJpaDialect的常见行为 (现在和将来), 只覆盖特定的钩子以插入具体的特定于供应商的行为.
 */
public interface JpaDialect extends PersistenceExceptionTranslator {

	/**
	 * 开始给定的JPA事务, 应用给定Spring事务定义指定的语义 (特别是隔离级别和超时).
	 * 在事务开始时由JpaTransactionManager调用.
	 * <p>实现可以配置JPA Transaction对象, 然后调用{@code begin}, 或者调用一个特殊的采用隔离级别的begin方法.
	 * <p>实现可以将只读标志应用为刷新模式.
	 * 在这种情况下, 可以返回一个事务数据对象， 该对象保存先前的刷新模式 (可能还有其他数据), 以便在{@code cleanupTransaction}中重置.
	 * 在开始事务之前, 它还可以将只读标志和隔离级别应用于底层JDBC连接.
	 * <p>实现还可以使用传入的TransactionDefinition公开的Spring事务名称, 来优化特定的数据访问用例 (有效地使用当前事务名称作为用例标识符).
	 * <p>如果持久化提供者支持, 此方法还允许公开保存点功能,
	 * 通过返回实现Spring的{@link org.springframework.transaction.SavepointManager}接口的Object.
	 * {@link JpaTransactionManager}将使用此功能.
	 * 
	 * @param entityManager 开始JPA事务的EntityManager
	 * @param definition 定义语义的Spring事务定义
	 * 
	 * @return 保存事务数据的任意对象 (传递给{@link #cleanupTransaction}).
	 * 可以实现{@link org.springframework.transaction.SavepointManager}接口.
	 * @throws javax.persistence.PersistenceException 如果被JPA方法抛出
	 * @throws java.sql.SQLException 如果被JDBC方法抛出
	 * @throws org.springframework.transaction.TransactionException 如果参数无效
	 */
	Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException;

	/**
	 * 准备JPA事务, 应用指定的语义.
	 * 在JTA事务或本地加入的事务中登记EntityManager时 (e.g. 在将未同步的EntityManager升级为同步的事务之后),
	 * 由EntityManagerFactoryUtils调用.
	 * <p>实现可以将只读标志应用为刷新模式.
	 * 在这种情况下, 可以返回一个事务数据对象, 该对象保存先前的刷新模式 (可能还有其他数据), 以便在{@code cleanupTransaction}中重置.
	 * <p>实现还可以使用Spring事务名称来优化特定的数据访问用例 (有效地使用当前事务名称作为用例标识符).
	 * 
	 * @param entityManager 要开始JPA事务的EntityManager
	 * @param readOnly 事务是否应该是只读的
	 * @param name 事务的名称
	 * 
	 * @return 保存事务数据的任意对象 (传递给{@link #cleanupTransaction}).
	 * @throws javax.persistence.PersistenceException 如果被JPA方法抛出
	 */
	Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException;

	/**
	 * 通过给定的事务数据清理事务.
	 * 在事务清理时由JpaTransactionManager和EntityManagerFactoryUtils调用.
	 * <p>例如, 实现可以重置底层JDBC连接的只读标志和隔离级别.
	 * 此外, 可以在此重置暴露的数据访问用例.
	 * 
	 * @param transactionData 保存事务数据的任意对象 (由 beginTransaction 或 prepareTransaction返回)
	 */
	void cleanupTransaction(Object transactionData);

	/**
	 * 如果访问关系数据库, 则检索给定JPA EntityManager使用的JDBC连接.
	 * 如果实际需要访问底层JDBC连接, 通常在活动JPA事务中 (例如, 通过JpaTransactionManager), 将调用此方法.
	 * 当不再需要时, 返回的句柄将被传递到{@code releaseJdbcConnection}方法.
	 * <p>这个策略是必要的, 因为JPA没有提供检索底层JDBC连接的标准方法 (由于JPA实现可能根本不能与关系数据库一起使用).
	 * <p>鼓励实现返回一个未包装的Connection对象, 即Connection从连接池中获取它.
	 * 这使得应用程序代码更容易获得底层本机JDBC Connection, 如OracleConnection, 这有时是LOB处理等所必需的.
	 * 假设调用代码知道如何正确处理返回的Connection对象.
	 * <p>在一个简单的情况下, 返回的Connection将使用EntityManager自动关闭, 或者可以通过Connection对象本身释放,
	 * 实现可以返回仅包含Connection的SimpleConnectionHandle.
	 * 如果{@code releaseJdbcConnection}中需要其他对象, 则实现应该使用引用其他对象的特殊句柄.
	 * 
	 * @param entityManager 当前的JPA EntityManager
	 * @param readOnly 是否仅出于只读目的需要Connection
	 * 
	 * @return Connection句柄, 传递给{@code releaseJdbcConnection}, 或{@code null} 如果不能检索JDBC连接
	 * @throws javax.persistence.PersistenceException 如果被JPA方法抛出
	 * @throws java.sql.SQLException 如果被JDBC方法抛出
	 */
	ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException;

	/**
	 * 释放最初通过{@code getJdbcConnection}检索的给定JDBC连接.
	 * 在任何情况下都应该调用它, 以允许正确释放检索到的Connection句柄.
	 * <p>如果在JPA事务完成或EntityManager关闭时, 隐式关闭{@code getJdbcConnection}返回的Connection, 则实现可能根本不执行任何操作.
	 * 
	 * @param conHandle 要释放的JDBC Connection句柄
	 * @param entityManager 当前JPA EntityManager
	 * 
	 * @throws javax.persistence.PersistenceException 如果被JPA方法抛出
	 * @throws java.sql.SQLException 如果被JDBC方法抛出
	 */
	void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager entityManager)
			throws PersistenceException, SQLException;

}
