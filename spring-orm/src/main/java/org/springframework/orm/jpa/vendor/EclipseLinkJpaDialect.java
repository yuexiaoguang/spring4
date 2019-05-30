package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.eclipse.persistence.sessions.UnitOfWork;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Eclipse持久化服务(EclipseLink)的{@link org.springframework.orm.jpa.JpaDialect}实现.
 * 针对EclipseLink 2.4开发和测试.
 *
 * <p>默认情况下, 此类使用实时JDBC连接获取实时EclipseLink事务, 以用于非只读事务.
 * 这允许在同一事务中混合JDBC和JPA/EclipseLink操作, 并对其影响进行交叉可见性.
 * 如果不需要, 请将"lazyDatabaseTransaction"标志设置为{@code true}, 或者始终将所有受影响的事务声明为只读.
 * 从Spring 4.1.2开始, 这将可靠地避免实时的JDBC连接检索, 从而使EclipseLink保持在共享缓存模式.
 */
@SuppressWarnings("serial")
public class EclipseLinkJpaDialect extends DefaultJpaDialect {

	private boolean lazyDatabaseTransaction = false;


	/**
	 * 设置是否在Spring管理的EclipseLink事务中延迟地启动数据库资源事务.
	 * <p>默认情况下, 只读事务是延迟启动的, 但是会实时启动常规的非只读事务.
	 * 这允许在整个EclipseLink事务中重用相同的JDBC连接, 以便在同一DataSource上使用JDBC访问代码实现强制隔离和一致可见性.
	 * <p>切换为"true"以强制执行延迟数据库事务, 即使对于非只读事务也是如此,
	 * 允许访问EclipseLink的共享缓存并遵循EclipseLink的连接模式配置, 假设JDBC级别的隔离和可见性不太重要.
	 */
	public void setLazyDatabaseTransaction(boolean lazyDatabaseTransaction) {
		this.lazyDatabaseTransaction = lazyDatabaseTransaction;
	}


	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			// 将自定义隔离级别传递给EclipseLink的DatabaseLogin配置 (自Spring 4.1.2起)
			UnitOfWork uow = entityManager.unwrap(UnitOfWork.class);
			uow.getLogin().setTransactionIsolation(definition.getIsolationLevel());
		}

		entityManager.getTransaction().begin();

		if (!definition.isReadOnly() && !this.lazyDatabaseTransaction) {
			// 开始实时事务以强制EclipseLink获取JDBC连接, 以便Spring可以使用JDBC和EclipseLink管理事务.
			entityManager.unwrap(UnitOfWork.class).beginEarlyTransaction();
		}

		return null;
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		// 从Spring 4.1.2开始, 使用自定义ConnectionHandle来延迟检索底层连接 (允许在EclipseLink EntityManager中开始延迟内部事务)
		return new EclipseLinkConnectionHandle(entityManager);
	}


	/**
	 * {@link ConnectionHandle}实现在第一次{@code getConnection}调用时, 延迟地获取EclipseLink提供的连接
	 * -  如果没有应用程序代码请求JDBC连接, 这可能永远不会出现.
	 * 这有助于推迟早期事务开始, 即在EclipseLink EntityManager中获取JDBC连接.
	 */
	private static class EclipseLinkConnectionHandle implements ConnectionHandle {

		private final EntityManager entityManager;

		private Connection connection;

		public EclipseLinkConnectionHandle(EntityManager entityManager) {
			this.entityManager = entityManager;
		}

		@Override
		public Connection getConnection() {
			if (this.connection == null) {
				this.connection = this.entityManager.unwrap(Connection.class);
			}
			return this.connection;
		}

		@Override
		public void releaseConnection(Connection con) {
		}
	}

}
