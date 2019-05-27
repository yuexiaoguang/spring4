package org.springframework.orm.jpa;

import java.io.Serializable;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * {@link JpaDialect}接口的默认实现.
 * 由{@link JpaTransactionManager}用作默认方言.
 *
 * <p>只需在{@link #beginTransaction}中开始标准JPA事务, 并通过{@link EntityManagerFactoryUtils}执行标准异常转换.
 *
 * <p><b>NOTE: 从Spring 4.0开始, Spring的JPA支持需要JPA 2.0或更高版本.</b>
 */
@SuppressWarnings("serial")
public class DefaultJpaDialect implements JpaDialect, Serializable {

	/**
	 * 此实现调用标准JPA {@code Transaction.begin}方法.
	 * 如果设置了非默认隔离级别, 则抛出InvalidIsolationLevelException.
	 * <p>此实现不返回任何事务数据Object, 因为没有为标准JPA事务保留状态.
	 * 因此, 子类不必关心此实现的返回值 ({@code null}), 并可以自由返回自己的事务数据对象.
	 */
	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(getClass().getSimpleName() +
					" does not support custom isolation levels due to limitations in standard JPA. " +
					"Specific arrangements may be implemented in custom JpaDialect variants.");
		}
		entityManager.getTransaction().begin();
		return null;
	}

	@Override
	public Object prepareTransaction(EntityManager entityManager, boolean readOnly, String name)
			throws PersistenceException {

		return null;
	}

	/**
	 * 此实现不执行任何操作, 因为默认的{@code beginTransaction}实现不需要任何清理.
	 */
	@Override
	public void cleanupTransaction(Object transactionData) {
	}

	/**
	 * 此实现始终返回 {@code null}, 表示不能提供JDBC连接.
	 */
	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return null;
	}

	/**
	 * 假设使用EntityManager隐式关闭Connection, 此实现不执行任何操作.
	 * <p>如果JPA实现返回一个Connection句柄, 它希望应用程序在使用后关闭,
	 * 那么dialect实现需要在这里调用{@code Connection.close()} (或其他类似效果的方法).
	 */
	@Override
	public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager em)
			throws PersistenceException, SQLException {
	}


	//-----------------------------------------------------------------------------------
	// Hook for exception translation (used by JpaTransactionManager)
	//-----------------------------------------------------------------------------------

	/**
	 * 此时现委托给EntityManagerFactoryUtils.
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
	}

}
