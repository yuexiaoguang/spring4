package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 提供从{@link javax.sql.DataSource}获取JDBC连接的静态方法.
 * 包括对Spring管理的事务连接的特殊支持,
 * e.g. 由{@link DataSourceTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}管理.
 *
 * <p>由Spring的{@link org.springframework.jdbc.core.JdbcTemplate}内部使用,
 * Spring的JDBC操作对象和JDBC {@link DataSourceTransactionManager}.
 * 也可以直接在应用程序代码中使用.
 */
public abstract class DataSourceUtils {

	/**
	 * 清理JDBC连接的TransactionSynchronization对象的顺序值.
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

	private static final Log logger = LogFactory.getLog(DataSourceUtils.class);


	/**
	 * 从给定的DataSource获取连接.
	 * 将SQLExceptions转换为未受检的通用数据访问异常的Spring层次结构, 简化调用代码并使任何抛出的异常更有意义.
	 * <p>知道绑定到当前线程的相应Connection, 例如使用{@link DataSourceTransactionManager}时.
	 * 如果事务同步处于活动状态, 则将Connection连接到线程,
	 * e.g. 在{@link org.springframework.transaction.jta.JtaTransactionManager JTA}事务中运行时.
	 * 
	 * @param dataSource 从中获取Connection的DataSource
	 * 
	 * @return 来自给定DataSource的JDBC连接
	 * @throws org.springframework.jdbc.CannotGetJdbcConnectionException 如果尝试获取连接失败
	 */
	public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
		try {
			return doGetConnection(dataSource);
		}
		catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
		}
	}

	/**
	 * 实际从给定的DataSource获取JDBC连接.
	 * 与{@link #getConnection}相同, 但抛出原始的SQLException.
	 * <p>知道绑定到当前线程的相应Connection, 例如使用{@link DataSourceTransactionManager}时.
	 * 如果事务同步处于活动状态, 则会将Connection绑定到线程 (e.g. 如果在JTA事务中).
	 * <p>由{@link TransactionAwareDataSourceProxy}直接访问.
	 * 
	 * @param dataSource 从中获取Connection的DataSource
	 * 
	 * @return 来自给定DataSource的JDBC连接
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static Connection doGetConnection(DataSource dataSource) throws SQLException {
		Assert.notNull(dataSource, "No DataSource specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasConnection()) {
				logger.debug("Fetching resumed JDBC Connection from DataSource");
				conHolder.setConnection(dataSource.getConnection());
			}
			return conHolder.getConnection();
		}
		// 否则要么没有持有者, 要么在这里没有空线程持有者.

		logger.debug("Fetching JDBC Connection from DataSource");
		Connection con = dataSource.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for JDBC Connection");
			// 对事务中的其他JDBC操作使用相同的Connection.
			// 在事务完成时, 将通过同步删除线程绑定对象.
			ConnectionHolder holderToUse = conHolder;
			if (holderToUse == null) {
				holderToUse = new ConnectionHolder(con);
			}
			else {
				holderToUse.setConnection(con);
			}
			holderToUse.requested();
			TransactionSynchronizationManager.registerSynchronization(
					new ConnectionSynchronization(holderToUse, dataSource));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != conHolder) {
				TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
			}
		}

		return con;
	}

	/**
	 * 使用给定的事务语义准备给定的Connection.
	 * 
	 * @param con 要准备的Connection
	 * @param definition 要应用的事务定义
	 * 
	 * @return 先前的隔离级别
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static Integer prepareConnectionForTransaction(Connection con, TransactionDefinition definition)
			throws SQLException {

		Assert.notNull(con, "No Connection specified");

		// Set read-only flag.
		if (definition != null && definition.isReadOnly()) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting JDBC Connection [" + con + "] read-only");
				}
				con.setReadOnly(true);
			}
			catch (SQLException ex) {
				Throwable exToCheck = ex;
				while (exToCheck != null) {
					if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
						// 假设这是一个连接超时, 否则会丢失: e.g. 从JDBC 4.0
						throw ex;
					}
					exToCheck = exToCheck.getCause();
				}
				// "不支持只读" SQLException -> 忽略, 无论如何它只是一个提示
				logger.debug("Could not set JDBC Connection read-only", ex);
			}
			catch (RuntimeException ex) {
				Throwable exToCheck = ex;
				while (exToCheck != null) {
					if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
						// 假设这是一个连接超时, 否则会丢失: e.g. 从 Hibernate
						throw ex;
					}
					exToCheck = exToCheck.getCause();
				}
				// "不支持只读" UnsupportedOperationException -> 忽略, 无论如何它只是一个提示
				logger.debug("Could not set JDBC Connection read-only", ex);
			}
		}

		// 应用特定的隔离级别.
		Integer previousIsolationLevel = null;
		if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			if (logger.isDebugEnabled()) {
				logger.debug("Changing isolation level of JDBC Connection [" + con + "] to " +
						definition.getIsolationLevel());
			}
			int currentIsolation = con.getTransactionIsolation();
			if (currentIsolation != definition.getIsolationLevel()) {
				previousIsolationLevel = currentIsolation;
				con.setTransactionIsolation(definition.getIsolationLevel());
			}
		}

		return previousIsolationLevel;
	}

	/**
	 * 在事务之后重置给定的连接, 关于只读标志和隔离级别.
	 * 
	 * @param con 要重置的Connection
	 * @param previousIsolationLevel 要恢复的隔离级别
	 */
	public static void resetConnectionAfterTransaction(Connection con, Integer previousIsolationLevel) {
		Assert.notNull(con, "No Connection specified");
		try {
			// 如果事务更改, 则将事务隔离重置为先前值.
			if (previousIsolationLevel != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting isolation level of JDBC Connection [" +
							con + "] to " + previousIsolationLevel);
				}
				con.setTransactionIsolation(previousIsolationLevel);
			}

			// Reset read-only flag.
			if (con.isReadOnly()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
				}
				con.setReadOnly(false);
			}
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}
	}

	/**
	 * 确定给定的JDBC连接是否是事务性的, 即由Spring的事务工具绑定到当前线程.
	 * 
	 * @param con 要检查的Connection
	 * @param dataSource 从中获取Connection的DataSource (may be {@code null})
	 * 
	 * @return Connection是否是事务性的
	 */
	public static boolean isConnectionTransactional(Connection con, DataSource dataSource) {
		if (dataSource == null) {
			return false;
		}
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		return (conHolder != null && connectionEquals(conHolder, con));
	}

	/**
	 * 将当前事务超时应用于给定的JDBC Statement对象.
	 * 
	 * @param stmt JDBC Statement对象
	 * @param dataSource 从中获取Connection的DataSource
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static void applyTransactionTimeout(Statement stmt, DataSource dataSource) throws SQLException {
		applyTimeout(stmt, dataSource, -1);
	}

	/**
	 * 将指定的超时 - 由当前事务超时覆盖 - 应用到给定的JDBC Statement对象.
	 * 
	 * @param stmt JDBC Statement对象
	 * @param dataSource 从中获取Connection的DataSource
	 * @param timeout 要应用的超时 (或0表示事务之外没有超时)
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static void applyTimeout(Statement stmt, DataSource dataSource, int timeout) throws SQLException {
		Assert.notNull(stmt, "No Statement specified");
		ConnectionHolder holder = null;
		if (dataSource != null) {
			holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		}
		if (holder != null && holder.hasTimeout()) {
			// 剩余的事务超时会覆盖指定的值.
			stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
		}
		else if (timeout >= 0) {
			// 没有当前事务超时 -> 应用指定值.
			stmt.setQueryTimeout(timeout);
		}
	}

	/**
	 * 关闭从给定DataSource获取的给定Connection, 如果它不是外部管理的 (即, 不绑定到该线程).
	 * 
	 * @param con 要关闭的连接 (如果这是{@code null}, 则将忽略该调用)
	 * @param dataSource 从中获取Connection的DataSource (may be {@code null})
	 */
	public static void releaseConnection(Connection con, DataSource dataSource) {
		try {
			doReleaseConnection(con, dataSource);
		}
		catch (SQLException ex) {
			logger.debug("Could not close JDBC Connection", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing JDBC Connection", ex);
		}
	}

	/**
	 * 实际关闭从给定DataSource获取的给定Connection.
	 * 与{@link #releaseConnection}相同, 但抛出原始的SQLException.
	 * <p>由{@link TransactionAwareDataSourceProxy}直接访问.
	 * 
	 * @param con 要关闭的连接 (如果这是{@code null}, 则将忽略该调用)
	 * @param dataSource 从中获取Connection的DataSource (may be {@code null})
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static void doReleaseConnection(Connection con, DataSource dataSource) throws SQLException {
		if (con == null) {
			return;
		}
		if (dataSource != null) {
			ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
			if (conHolder != null && connectionEquals(conHolder, con)) {
				// 这是事务性连接: 不要关闭它.
				conHolder.released();
				return;
			}
		}
		logger.debug("Returning JDBC Connection to DataSource");
		doCloseConnection(con, dataSource);
	}

	/**
	 * 关闭连接, 除非{@link SmartDataSource}不希望这样做.
	 * 
	 * @param con 要关闭的连接
	 * @param dataSource 从中获取Connection的DataSource
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	public static void doCloseConnection(Connection con, DataSource dataSource) throws SQLException {
		if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
			con.close();
		}
	}

	/**
	 * 确定给定的两个Connections是否相等, 在代理的情况下询问目标Connection.
	 * 用于检测相等性, 即使用户在保持的原始目标连接是代理时传入了原始目标连接.
	 * 
	 * @param conHolder 保存的连接的ConnectionHolder (可能是代理)
	 * @param passedInCon 用户传入的连接 (可能是没有代理的目标连接)
	 * 
	 * @return 给定的连接是否相等
	 */
	private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
		if (!conHolder.hasConnection()) {
			return false;
		}
		Connection heldCon = conHolder.getConnection();
		// 显式检查身份: 对于没有正确实现"equals"的连接句柄, 例如Commons DBCP公开的那些句柄.
		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
				getTargetConnection(heldCon).equals(passedInCon));
	}

	/**
	 * 返回给定Connection的最内层目标Connection.
	 * 如果给定的Connection是代理, 它将被解包, 直到找到非代理连接.
	 * 否则, 传入的Connection将按原样返回.
	 * 
	 * @param con 解包的Connection代理
	 * 
	 * @return 最里面的目标Connection; 如果没有代理, 则传入一个
	 */
	public static Connection getTargetConnection(Connection con) {
		Connection conToUse = con;
		while (conToUse instanceof ConnectionProxy) {
			conToUse = ((ConnectionProxy) conToUse).getTargetConnection();
		}
		return conToUse;
	}

	/**
	 * 确定要用于给定DataSource的连接同步顺序.
	 * 降低了DataSource所具有的每个嵌套级别, 通过DelegatingDataSource嵌套级别进行检查.
	 * 
	 * @param dataSource 要检查的DataSource
	 * 
	 * @return 要使用的连接同步顺序
	 */
	private static int getConnectionSynchronizationOrder(DataSource dataSource) {
		int order = CONNECTION_SYNCHRONIZATION_ORDER;
		DataSource currDs = dataSource;
		while (currDs instanceof DelegatingDataSource) {
			order--;
			currDs = ((DelegatingDataSource) currDs).getTargetDataSource();
		}
		return order;
	}


	/**
	 * 在非本机JDBC事务结束时, 资源清理的回调 (e.g. 参与JtaTransactionManager事务时).
	 */
	private static class ConnectionSynchronization extends TransactionSynchronizationAdapter {

		private final ConnectionHolder connectionHolder;

		private final DataSource dataSource;

		private int order;

		private boolean holderActive = true;

		public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
			this.connectionHolder = connectionHolder;
			this.dataSource = dataSource;
			this.order = getConnectionSynchronizationOrder(dataSource);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
					// 如果应用程序不再保留句柄, 则在挂起时释放连接. 
					// 如果应用程序在恢复后再次访问ConnectionHolder, 将获取一个新连接, 假设它将参与同一事务.
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					this.connectionHolder.setConnection(null);
				}
			}
		}

		@Override
		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
			}
		}

		@Override
		public void beforeCompletion() {
			// 如果不再打开持有者, 尽早释放连接
			// (也就是说, Hibernate Session之类的另一个资源没有使用它, 它通过事务同步进行自己的清理),
			// 避免在事务完成之前期望关闭调用的严格JTA实现的问题.
			if (!this.connectionHolder.isOpen()) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			// 如果还没有在beforeCompletion中关闭Connection, 请立即关闭它. 
			// 在此期间, 持有者可能已经用于其他清理, 例如通过Hibernate Session.
			if (this.holderActive) {
				// 线程绑定的ConnectionHolder可能不再可用, 因为afterCompletion可能从另一个线程调用.
				TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					// 重置ConnectionHolder: 它可能仍然绑定到线程.
					this.connectionHolder.setConnection(null);
				}
			}
			this.connectionHolder.reset();
		}
	}
}
