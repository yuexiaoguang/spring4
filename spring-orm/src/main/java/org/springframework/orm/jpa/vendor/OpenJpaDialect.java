package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.LogFactory;
import org.apache.openjpa.persistence.FetchPlan;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.jdbc.IsolationLevel;
import org.apache.openjpa.persistence.jdbc.JDBCFetchPlan;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Apache OpenJPA的{@link org.springframework.orm.jpa.JpaDialect}实现.
 * 针对OpenJPA 2.2开发和测试.
 */
@SuppressWarnings("serial")
public class OpenJpaDialect extends DefaultJpaDialect {

	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
			throws PersistenceException, SQLException, TransactionException {

		OpenJPAEntityManager openJpaEntityManager = getOpenJPAEntityManager(entityManager);

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			// 将自定义隔离级别传递给OpenJPA的JDBCFetchPlan配置
			FetchPlan fetchPlan = openJpaEntityManager.getFetchPlan();
			if (fetchPlan instanceof JDBCFetchPlan) {
				IsolationLevel isolation = IsolationLevel.fromConnectionConstant(definition.getIsolationLevel());
				((JDBCFetchPlan) fetchPlan).setIsolation(isolation);
			}
		}

		entityManager.getTransaction().begin();

		if (!definition.isReadOnly()) {
			// 与EclipseLink一样, 确保实时启动逻辑事务, 以便使用连接的其他参与者(例如JdbcTemplate)在事务中运行.
			openJpaEntityManager.beginStore();
		}

		// OpenJPA保存点处理的自定义实现
		return new OpenJpaTransactionData(openJpaEntityManager);
	}

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly)
			throws PersistenceException, SQLException {

		return new OpenJpaConnectionHandle(getOpenJPAEntityManager(entityManager));
	}

	/**
	 * 返回特定于OpenJPA的{@code EntityManager}变体.
	 * 
	 * @param em 通用{@code EntityManager}实例
	 * 
	 * @return 特定于OpenJPA的{@code EntityManager}变体
	 */
	protected OpenJPAEntityManager getOpenJPAEntityManager(EntityManager em) {
		return OpenJPAPersistence.cast(em);
	}


	/**
	 * 从{@code beginTransaction}公开的事务数据对象, 实现了{@link SavepointManager}接口.
	 */
	private static class OpenJpaTransactionData implements SavepointManager {

		private final OpenJPAEntityManager entityManager;

		private int savepointCounter = 0;

		public OpenJpaTransactionData(OpenJPAEntityManager entityManager) {
			this.entityManager = entityManager;
		}

		@Override
		public Object createSavepoint() throws TransactionException {
			this.savepointCounter++;
			String savepointName = ConnectionHolder.SAVEPOINT_NAME_PREFIX + this.savepointCounter;
			this.entityManager.setSavepoint(savepointName);
			return savepointName;
		}

		@Override
		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			this.entityManager.rollbackToSavepoint((String) savepoint);
		}

		@Override
		public void releaseSavepoint(Object savepoint) throws TransactionException {
			try {
				this.entityManager.releaseSavepoint((String) savepoint);
			}
			catch (Throwable ex) {
				LogFactory.getLog(OpenJpaTransactionData.class).debug(
						"Could not explicitly release OpenJPA savepoint", ex);
			}
		}
	}


	/**
	 * {@link ConnectionHandle}实现, 为每个{@code getConnection}调用获取一个新的OpenJPA提供的连接, 并关闭{@code releaseConnection}上的连接.
	 * 这是必要的, 因为OpenJPA要求在继续EntityManager工作之前关闭提取的连接.
	 */
	private static class OpenJpaConnectionHandle implements ConnectionHandle {

		private final OpenJPAEntityManager entityManager;

		public OpenJpaConnectionHandle(OpenJPAEntityManager entityManager) {
			this.entityManager = entityManager;
		}

		@Override
		public Connection getConnection() {
			return (Connection) this.entityManager.getConnection();
		}

		@Override
		public void releaseConnection(Connection con) {
			JdbcUtils.closeConnection(con);
		}
	}
}
