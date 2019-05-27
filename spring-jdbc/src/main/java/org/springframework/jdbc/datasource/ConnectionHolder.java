package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * 保存Connection, 包装JDBC连接.
 * {@link DataSourceTransactionManager}将此类的实例绑定到线程, 用于特定的DataSource.
 *
 * <p>从基类继承对嵌套JDBC事务和引用计数功能的仅回滚支持.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class ConnectionHolder extends ResourceHolderSupport {

	public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";


	private ConnectionHandle connectionHandle;

	private Connection currentConnection;

	private boolean transactionActive = false;

	private Boolean savepointsSupported;

	private int savepointCounter = 0;


	/**
	 * @param connectionHandle 要保存的ConnectionHandle
	 */
	public ConnectionHolder(ConnectionHandle connectionHandle) {
		Assert.notNull(connectionHandle, "ConnectionHandle must not be null");
		this.connectionHandle = connectionHandle;
	}

	/**
	 * 为给定的JDBC Connection创建一个新的ConnectionHolder,
	 * 并使用{@link SimpleConnectionHandle}包装它, 假设没有正在进行的事务.
	 * 
	 * @param connection 要保存的JDBC连接
	 */
	public ConnectionHolder(Connection connection) {
		this.connectionHandle = new SimpleConnectionHandle(connection);
	}

	/**
	 * 为给定的JDBC Connection创建一个新的ConnectionHolder, 并使用{@link SimpleConnectionHandle}包装它.
	 * 
	 * @param connection 要保存的JDBC连接
	 * @param transactionActive 是否给定Connection涉及正在进行的事务
	 */
	public ConnectionHolder(Connection connection, boolean transactionActive) {
		this(connection);
		this.transactionActive = transactionActive;
	}


	/**
	 * 返回此ConnectionHolder持有的ConnectionHandle.
	 */
	public ConnectionHandle getConnectionHandle() {
		return this.connectionHandle;
	}

	/**
	 * 返回此持有者当前是否有连接.
	 */
	protected boolean hasConnection() {
		return (this.connectionHandle != null);
	}

	/**
	 * 设置此holder是否表示由JDBC管理的活动事务.
	 */
	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	/**
	 * 返回此持有者是否表示由JDBC管理的活动事务.
	 */
	protected boolean isTransactionActive() {
		return this.transactionActive;
	}


	/**
	 * 使用给定的Connection覆盖现有的Connection句柄.
	 * 如果给定{@code null}, 重置句柄.
	 * <p>用于在挂起时释放连接 (使用{@code null}参数), 并在恢复时设置新连接.
	 */
	protected void setConnection(Connection connection) {
		if (this.currentConnection != null) {
			this.connectionHandle.releaseConnection(this.currentConnection);
			this.currentConnection = null;
		}
		if (connection != null) {
			this.connectionHandle = new SimpleConnectionHandle(connection);
		}
		else {
			this.connectionHandle = null;
		}
	}

	/**
	 * 返回此ConnectionHolder持有的当前连接.
	 * <p>在ConnectionHolder上调用{@code released}之前, 这将是相同的Connection,
	 * 它将重置保持的Connection, 按需获取新的Connection.
	 */
	public Connection getConnection() {
		Assert.notNull(this.connectionHandle, "Active Connection is required");
		if (this.currentConnection == null) {
			this.currentConnection = this.connectionHandle.getConnection();
		}
		return this.currentConnection;
	}

	/**
	 * 返回是否支持JDBC 3.0 Savepoints.
	 * 缓存此ConnectionHolder生命周期的标志.
	 * 
	 * @throws SQLException 如果由JDBC驱动程序抛出
	 */
	public boolean supportsSavepoints() throws SQLException {
		if (this.savepointsSupported == null) {
			this.savepointsSupported = getConnection().getMetaData().supportsSavepoints();
		}
		return this.savepointsSupported;
	}

	/**
	 * 为当前Connection创建新的JDBC 3.0 Savepoint, 使用唯一的生成的保存点名称.
	 * 
	 * @return 新的Savepoint
	 * @throws SQLException 如果由JDBC驱动程序抛出
	 */
	public Savepoint createSavepoint() throws SQLException {
		this.savepointCounter++;
		return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
	}

	/**
	 * 释放此ConnectionHolder持有的当前Connection.
	 * <p>这对于期望"连接借用"的ConnectionHandles是必需的, 其中每个返回的Connection仅被临时租用并且需要在数据操作完成后返回,
	 * 以使Connection可用于同一事务中的其他操作.
	 * 例如, JDO 2.0 DataStoreConnections就是这种情况.
	 */
	@Override
	public void released() {
		super.released();
		if (!isOpen() && this.currentConnection != null) {
			this.connectionHandle.releaseConnection(this.currentConnection);
			this.currentConnection = null;
		}
	}


	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
		this.savepointsSupported = null;
		this.savepointCounter = 0;
	}
}
