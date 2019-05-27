package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * 单个JDBC {@link javax.sql.DataSource}的{@link org.springframework.transaction.PlatformTransactionManager}实现.
 * 只要安装程序使用{@code javax.sql.DataSource}作为其{@code Connection}工厂机制,
 * 此类就可以在任何具有JDBC驱动程序的环境中工作.
 * 将JDBC连接从指定的DataSource绑定到当前线程, 可能允许每个DataSource一个线程绑定的Connection.
 *
 * <p><b>Note: 此事务管理器操作的DataSource需要返回独立的Connection.</b>
 * Connection可能来自池 (典型情况), 但DataSource不能返回线程范围/请求范围的连接等.
 * 此事务管理器将根据指定的传播行为将Connections与线程绑定事务本身关联.
 * 它假定即使在正在进行的事务中也可以获得单独的独立连接.
 *
 * <p>需要应用程序代码检索JDBC连接, 通过{@link DataSourceUtils#getConnection(DataSource)},
 * 而不是标准的Java EE风格{@link DataSource#getConnection()}调用.
 * 诸如{@link org.springframework.jdbc.core.JdbcTemplate}之类的Spring类隐式使用此策略.
 * 如果不与此事务管理器结合使用, {@link DataSourceUtils}查找策略的行为与本机DataSource查找完全相同;
 * 因此它可以以便携方式使用.
 *
 * <p>或者, 可以允许应用程序代码使用标准的Java EE样式查找模式{@link DataSource#getConnection()},
 * 例如, 对于根本不了解Spring的遗留代码.
 * 在这种情况下, 为目标DataSource定义一个{@link TransactionAwareDataSourceProxy},
 * 并将该代理DataSource传递给DAO, 它们在访问时将自动参与Spring管理的事务.
 *
 * <p>支持自定义隔离级别, 以及作为适当的JDBC语句超时应用的超时.
 * 要支持后者, 应用程序代码必须使用{@link org.springframework.jdbc.core.JdbcTemplate},
 * 为每个创建的JDBC语句调用{@link DataSourceUtils#applyTransactionTimeout},
 * 或者通过{@link TransactionAwareDataSourceProxy}, 它将自动创建超时感知的JDBC Connection和 Statement.
 *
 * <p>考虑为目标DataSource定义{@link LazyConnectionDataSourceProxy}, 将此事务管理器和DAO指向它.
 * 这将导致"空"事务的优化处理, i.e. 没有执行任何JDBC语句的事务.
 * 在执行Statement之前, LazyConnectionDataSourceProxy不会从目标DataSource获取实际的JDBC连接,
 * 而是延迟地将指定的事务设置应用于目标Connection.
 *
 * <p>此事务管理器通过JDBC 3.0 {@link java.sql.Savepoint}机制支持嵌套事务.
 * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"}标志默认为"true",
 * 因为嵌套事务对支持保存点的JDBC驱动程序 (例如Oracle JDBC驱动程序)没有限制.
 *
 * <p>此事务管理器可用作单个资源案例中{@link org.springframework.transaction.jta.JtaTransactionManager}的替代,
 * 因为它不需要支持JTA的容器, 通常与本地定义的JDBC DataSource (e.g. an Apache Commons DBCP连接池)结合使用.
 * 在本地策略和JTA环境之间切换只是配置问题!
 *
 * <p>从4.3.4开始, 此事务管理器触发已注册的事务同步的刷新回调 (如果同步通常处于活动状态),
 * 假设资源在底层JDBC {@code Connection}上运行.
 * 这允许类似于{@code JtaTransactionManager}的设置, 特别是关于延迟注册的ORM资源 (e.g. a Hibernate {@code Session}).
 */
@SuppressWarnings("serial")
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	private DataSource dataSource;

	private boolean enforceReadOnly = false;


	/**
	 * 必须将DataSource设置为能够使用它.
	 */
	public DataSourceTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	public DataSourceTransactionManager(DataSource dataSource) {
		this();
		setDataSource(dataSource);
		afterPropertiesSet();
	}

	/**
	 * 设置此实例应管理其事务的JDBC DataSource.
	 * <p>这通常是本地定义的DataSource, 例如Apache Commons DBCP连接池.
	 * 或者, 也可以为从JNDI获取的非XA J2EE DataSource驱动事务. 对于XA DataSource, 使用JtaTransactionManager.
	 * <p>此处指定的DataSource应该是用于管理事务的目标DataSource, 而不是TransactionAwareDataSourceProxy.
	 * 只有数据访问代码可以与TransactionAwareDataSourceProxy一起使用, 而事务管理器需要处理底层目标DataSource.
	 * 如果传入了TransactionAwareDataSourceProxy, 它将被解包以提取其目标DataSource.
	 * <p><b>这里传入的DataSource需要返回独立的Connections.</b>
	 * Connections可能来自池 (典型情况), 但DataSource不能返回线程范围/请求范围的连接等.
	 */
	public void setDataSource(DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// 如果得到了TransactionAwareDataSourceProxy, 需要为其底层目标DataSource执行事务,
			// 否则数据访问代码将无法看到正确公开的事务 (i.e. 目标DataSource的事务).
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * 返回此实例管理其事务的JDBC DataSource.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 指定是否通过事务连接上的显式语句强制事务的只读性质 (由{@link TransactionDefinition#isReadOnly()}指示):
	 * Oracle, MySQL 和 Postgres所理解的"SET TRANSACTION READ ONLY".
	 * <p>确切的处理, 包括在连接上执行的任何SQL语句, 都可以通过{@link #prepareTransactionalConnection}进行自定义.
	 * <p>这种只读处理模式超出了{@link Connection#setReadOnly}提示, 默认情况下Spring适用.
	 * 与标准JDBC提示相反, "SET TRANSACTION READ ONLY"强制执行类似隔离级别的连接模式, 其中严格禁止数据操作语句.
	 * 此外, 在Oracle上, 此只读模式为整个事务提供读取一致性.
	 * <p>请注意, 即使对于{@code Connection.setReadOnly(true)}旧的Oracle JDBC驱动程序(9i, 10g)也用于强制执行此只读模式.
	 * 但是, 对于最近的驱动程序, 需要明确应用这种强有力的强制执行, e.g. 通过这个标志.
	 */
	public void setEnforceReadOnly(boolean enforceReadOnly) {
		this.enforceReadOnly = enforceReadOnly;
	}

	/**
	 * 返回是否通过事务连接上的显式语句强制执行事务的只读性质.
	 */
	public boolean isEnforceReadOnly() {
		return this.enforceReadOnly;
	}

	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
	}


	@Override
	public Object getResourceFactory() {
		return getDataSource();
	}

	@Override
	protected Object doGetTransaction() {
		DataSourceTransactionObject txObject = new DataSourceTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());
		ConnectionHolder conHolder =
				(ConnectionHolder) TransactionSynchronizationManager.getResource(this.dataSource);
		txObject.setConnectionHolder(conHolder, false);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
	}

	/**
	 * 此实现设置隔离级别, 但忽略超时.
	 */
	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		Connection con = null;

		try {
			if (!txObject.hasConnectionHolder() ||
					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
				Connection newCon = this.dataSource.getConnection();
				if (logger.isDebugEnabled()) {
					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
				}
				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
			}

			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
			con = txObject.getConnectionHolder().getConnection();

			Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
			txObject.setPreviousIsolationLevel(previousIsolationLevel);

			// 必要时切换到手动提交. 这在某些JDBC驱动程序中非常昂贵, 因此不希望不必要地执行此操作
			// (例如, 如果已经显式配置连接池来设置它).
			if (con.getAutoCommit()) {
				txObject.setMustRestoreAutoCommit(true);
				if (logger.isDebugEnabled()) {
					logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
				}
				con.setAutoCommit(false);
			}

			prepareTransactionalConnection(con, definition);
			txObject.getConnectionHolder().setTransactionActive(true);

			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
			}

			// 将连接保存器绑定到线程上.
			if (txObject.isNewConnectionHolder()) {
				TransactionSynchronizationManager.bindResource(getDataSource(), txObject.getConnectionHolder());
			}
		}

		catch (Throwable ex) {
			if (txObject.isNewConnectionHolder()) {
				DataSourceUtils.releaseConnection(con, this.dataSource);
				txObject.setConnectionHolder(null, false);
			}
			throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(this.dataSource);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(this.dataSource, suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing JDBC transaction on Connection [" + con + "]");
		}
		try {
			con.commit();
		}
		catch (SQLException ex) {
			throw new TransactionSystemException("Could not commit JDBC transaction", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
		}
		try {
			con.rollback();
		}
		catch (SQLException ex) {
			throw new TransactionSystemException("Could not roll back JDBC transaction", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() +
					"] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

		// 如果公开, 从线程上删除连接保存器.
		if (txObject.isNewConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(this.dataSource);
		}

		// 重置连接.
		Connection con = txObject.getConnectionHolder().getConnection();
		try {
			if (txObject.isMustRestoreAutoCommit()) {
				con.setAutoCommit(true);
			}
			DataSourceUtils.resetConnectionAfterTransaction(con, txObject.getPreviousIsolationLevel());
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}

		if (txObject.isNewConnectionHolder()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
			}
			DataSourceUtils.releaseConnection(con, this.dataSource);
		}

		txObject.getConnectionHolder().clear();
	}


	/**
	 * 在事务开始后立即准备事务{@code Connection}.
	 * <p>如果{@link #setEnforceReadOnly "enforceReadOnly"}标志设置为{@code true}并且事务定义指示只读事务,
	 * 则默认实现执行"SET TRANSACTION READ ONLY"语句.
	 * <p>Oracle, MySQL 和 Postgres可以理解"SET TRANSACTION READ ONLY", 也可以与其他数据库一起使用.
	 * 可以相应地覆盖此方法.
	 * 
	 * @param con 事务JDBC连接
	 * @param definition 当前的事务定义
	 * 
	 * @throws SQLException 如果被JDBC API抛出
	 */
	protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
			throws SQLException {

		if (isEnforceReadOnly() && definition.isReadOnly()) {
			Statement stmt = con.createStatement();
			try {
				stmt.executeUpdate("SET TRANSACTION READ ONLY");
			}
			finally {
				stmt.close();
			}
		}
	}


	/**
	 * DataSource事务对象, 表示ConnectionHolder.
	 * 由DataSourceTransactionManager用作事务对象.
	 */
	private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {

		private boolean newConnectionHolder;

		private boolean mustRestoreAutoCommit;

		public void setConnectionHolder(ConnectionHolder connectionHolder, boolean newConnectionHolder) {
			super.setConnectionHolder(connectionHolder);
			this.newConnectionHolder = newConnectionHolder;
		}

		public boolean isNewConnectionHolder() {
			return this.newConnectionHolder;
		}

		public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
			this.mustRestoreAutoCommit = mustRestoreAutoCommit;
		}

		public boolean isMustRestoreAutoCommit() {
			return this.mustRestoreAutoCommit;
		}

		public void setRollbackOnly() {
			getConnectionHolder().setRollbackOnly();
		}

		@Override
		public boolean isRollbackOnly() {
			return getConnectionHolder().isRollbackOnly();
		}

		@Override
		public void flush() {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationUtils.triggerFlush();
			}
		}
	}
}
