package org.springframework.orm.jdo;

import java.sql.Connection;
import java.sql.SQLException;
import javax.jdo.Constants;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link JdoDialect}接口的默认实现.
 * 从Spring 4.0开始, 专为JDO 3.0而设计 (或者更确切地说, 是JDO 3.0之外的语义).
 * 由{@link JdoTransactionManager}用作默认方言.
 *
 * <p>只需在{@code beginTransaction}中开始一个标准的JDO事务.
 * 在{@code getJdbcConnection}上返回JDO DataStoreConnection的句柄.
 * 在{@code flush}上调用相应的JDO PersistenceManager操作.
 * 如果适用, 使用Spring SQLExceptionTranslator进行异常转换.
 *
 * <p>请注意, 即使使用JDO 3.0, 特定于供应商的子类仍然是特殊事务语义和更复杂的异常转换所必需的.
 * 此外, 鼓励特定于供应商的子类在{@code getJdbcConnection}上公开本机JDBC连接, 而不是JDO 3.0的包装器句柄.
 *
 * <p>此类还实现了PersistenceExceptionTranslator接口, 由Spring的PersistenceExceptionTranslationPostProcessor自动检测,
 * 用于基于AOP的Spring DataAccessExceptions的本机异常转换.
 * 因此，标准DefaultJdoDialect bean的存在自动使PersistenceExceptionTranslationPostProcessor能够转换JDO异常.
 */
public class DefaultJdoDialect implements JdoDialect, PersistenceExceptionTranslator {

	private SQLExceptionTranslator jdbcExceptionTranslator;


	public DefaultJdoDialect() {
	}

	/**
	 * @param connectionFactory JDO PersistenceManagerFactory的连接工厂, 用于初始化默认的JDBC异常转换器
	 */
	public DefaultJdoDialect(Object connectionFactory) {
		this.jdbcExceptionTranslator = PersistenceManagerFactoryUtils.newJdbcExceptionTranslator(connectionFactory);
	}

	/**
	 * 设置此方言的JDBC异常转换器.
	 * <p>如果指定, 应用于JDOException 的任何SQLException的根本原因.
	 * 默认是依赖于JDO提供者的本机异常转换.
	 * 
	 * @param jdbcExceptionTranslator 异常转换器
	 */
	public void setJdbcExceptionTranslator(SQLExceptionTranslator jdbcExceptionTranslator) {
		this.jdbcExceptionTranslator = jdbcExceptionTranslator;
	}

	/**
	 * 返回此方言的JDBC异常转换器.
	 */
	public SQLExceptionTranslator getJdbcExceptionTranslator() {
		return this.jdbcExceptionTranslator;
	}


	//-------------------------------------------------------------------------
	// Hooks for transaction management (used by JdoTransactionManager)
	//-------------------------------------------------------------------------

	/**
	 * 此实现调用标准JDO {@link Transaction#begin()}方法,
	 * 并在必要时调用{@link Transaction#setIsolationLevel(String)}.
	 */
	@Override
	public Object beginTransaction(Transaction transaction, TransactionDefinition definition)
			throws JDOException, SQLException, TransactionException {

		String jdoIsolationLevel = getJdoIsolationLevel(definition);
		if (jdoIsolationLevel != null) {
			transaction.setIsolationLevel(jdoIsolationLevel);
		}
		transaction.begin();
		return null;
	}

	/**
	 * 确定用于给定Spring事务定义的JDO隔离级别String.
	 * 
	 * @param definition Spring事务定义
	 * 
	 * @return 相应的JDO隔离级别String, 或{@code null}表示不应显式设置隔离级别
	 */
	protected String getJdoIsolationLevel(TransactionDefinition definition) {
		switch (definition.getIsolationLevel()) {
			case TransactionDefinition.ISOLATION_SERIALIZABLE:
				return Constants.TX_SERIALIZABLE;
			case TransactionDefinition.ISOLATION_REPEATABLE_READ:
				return Constants.TX_REPEATABLE_READ;
			case TransactionDefinition.ISOLATION_READ_COMMITTED:
				return Constants.TX_READ_COMMITTED;
			case TransactionDefinition.ISOLATION_READ_UNCOMMITTED:
				return Constants.TX_READ_UNCOMMITTED;
			default:
				return null;
		}
	}

	/**
	 * 此实现不执行任何操作, 因为默认的beginTransaction实现不需要任何清理.
	 */
	@Override
	public void cleanupTransaction(Object transactionData) {
	}

	/**
	 * 此实现返回JDO的DataStoreConnectionHandle.
	 * <p><b>NOTE:</b> JDO DataStoreConnection始终是包装器, 而不是本机JDBC Connection.
	 * 如果需要访问本机JDBC连接 (或连接池句柄, 通过Spring NativeJdbcExtractor解包),
	 * 覆盖此方法以通过相应的特定于供应商的机制返回本机连接.
	 * <p>JDO DataStoreConnection仅从PersistenceManager "借用":
	 * 它需要尽早返回.
	 * 实际上, JDO要求在继续PersistenceManager工作之前关闭已获取的Connection.
	 * 因此, 暴露的ConnectionHandle在每个JDBC数据访问操作结束时 (即{@code DataSourceUtils.releaseConnection}),
	 * 实时地释放其JDBC Connection.
	 */
	@Override
	public ConnectionHandle getJdbcConnection(PersistenceManager pm, boolean readOnly)
			throws JDOException, SQLException {

		return new DataStoreConnectionHandle(pm);
	}

	/**
	 * 假设使用PersistenceManager隐式关闭Connection, 此实现不执行任何操作.
	 * <p>如果JDO提供者返回它希望应用程序关闭的Connection句柄, 则方言需要在此处调用{@code Connection.close}.
	 */
	@Override
	public void releaseJdbcConnection(ConnectionHandle conHandle, PersistenceManager pm)
			throws JDOException, SQLException {
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JdoTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * PersistenceExceptionTranslator接口的实现, 由Spring的PersistenceExceptionTranslationPostProcessor自动检测.
	 * <p>如果它是JDOException, 则使用此JdoDialect转换异常.
	 * 否则返回{@code null}以指示未知异常.
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof JDOException) {
			return translateException((JDOException) ex);
		}
		return null;
	}

	/**
	 * 此实现委托给PersistenceManagerFactoryUtils.
	 */
	@Override
	public DataAccessException translateException(JDOException ex) {
		if (getJdbcExceptionTranslator() != null && ex.getCause() instanceof SQLException) {
			return getJdbcExceptionTranslator().translate("JDO operation: " + ex.getMessage(),
					extractSqlStringFromException(ex), (SQLException) ex.getCause());
		}
		return PersistenceManagerFactoryUtils.convertJdoAccessException(ex);
	}

	/**
	 * 用于从给定异常中提取SQL String的模板方法.
	 * <p>默认实现总是返回{@code null}.
	 * 可以在子类中重写以提取特定于供应商的异常类的SQL字符串.
	 * 
	 * @param ex JDOException, 包含SQLException
	 * 
	 * @return SQL字符串, 或{@code null}
	 */
	protected String extractSqlStringFromException(JDOException ex) {
		return null;
	}


	/**
	 * ConnectionHandle实现，为每个{@code getConnection}调用获取一个新的JDO DataStoreConnection,
	 * 并在{@code releaseConnection}上关闭Connection.
	 * 这是必要的，因为JDO要求在继续PersistenceManager工作之前关闭提取的Connection.
	 */
	private static class DataStoreConnectionHandle implements ConnectionHandle {

		private final PersistenceManager persistenceManager;

		public DataStoreConnectionHandle(PersistenceManager persistenceManager) {
			this.persistenceManager = persistenceManager;
		}

		@Override
		public Connection getConnection() {
			return (Connection) this.persistenceManager.getDataStoreConnection();
		}

		@Override
		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}

}
