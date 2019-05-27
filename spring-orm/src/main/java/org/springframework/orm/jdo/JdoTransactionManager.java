package org.springframework.orm.jdo;

import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}实现,
 * 用于单个JDO {@link javax.jdo.PersistenceManagerFactory}.
 * 将JDO PersistenceManager从指定的工厂绑定到线程, 可能允许每个工厂使用一个线程绑定的PersistenceManager.
 * {@link PersistenceManagerFactoryUtils} 和{@link org.springframework.orm.jdo.support.SpringPersistenceManagerProxyBean}
 * 了解线程绑定持久性管理器并自动参与此类事务.
 * 支持此事务管理机制的JDO访问代码需要使用其中任何一个(或通过{@link TransactionAwarePersistenceManagerFactoryProxy}.
 *
 * <p>此事务管理器适用于使用单个JDO PersistenceManagerFactory进行事务数据访问的应用程序.
 * JTA (通常通过{@link org.springframework.transaction.jta.JtaTransactionManager}) 是访问同一事务中的多个事务资源所必需的.
 * 请注意, 需要相应地配置JDO提供程序, 以使其参与JTA事务.
 *
 * <p>此事务管理器还支持事务中的直接DataSource访问 (i.e. 使用相同DataSource的纯JDBC代码).
 * 这允许混合访问JDO的服务和使用普通JDBC的服务 (不知道JDO)!
 * 应用程序代码需要遵循与{@link org.springframework.jdbc.datasource.DataSourceTransactionManager}相同的简单连接查找模式
 * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * 或者通过
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
 *
 * <p>Note: 为了能够为普通的JDBC代码注册DataSource的Connection, 这个实例需要知道DataSource ({@link #setDataSource}).
 * 给定的DataSource显然应该与给定的PersistenceManagerFactory使用的数据源匹配.
 * 此事务管理器将自动检测充当PersistenceManagerFactory的"connectionFactory"的DataSource,
 * 因此通常不需要显式指定"dataSource"属性.
 *
 * <p>此事务管理器通过JDBC 3.0 Savepoints支持嵌套事务.
 * {@link #setNestedTransactionAllowed} "nestedTransactionAllowed"}标志默认为 "false",
 * 因为嵌套事务只适用于JDBC连接, 而不适用于JDO PersistenceManager及其缓存的实体对象和相关上下文.
 * 如果要将嵌套事务用于参与JDO事务的JDBC访问代码(如果JDBC驱动程序支持Savepoints),
 * 则可以手动将标志设置为"true".
 * <i>请注意, JDO本身不支持嵌套事务! 因此, 不要指望JDO访问代码在语义上参与嵌套事务.</i>
 */
@SuppressWarnings("serial")
public class JdoTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	private PersistenceManagerFactory persistenceManagerFactory;

	private DataSource dataSource;

	private boolean autodetectDataSource = true;

	private JdoDialect jdoDialect;


	/**
	 * 必须设置PersistenceManagerFactory才能使用它.
	 */
	public JdoTransactionManager() {
	}

	/**
	 * @param pmf 用于管理事务的PersistenceManagerFactory
	 */
	public JdoTransactionManager(PersistenceManagerFactory pmf) {
		this.persistenceManagerFactory = pmf;
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的PersistenceManagerFactory.
	 * <p>这里指定的PersistenceManagerFactory应该是目标PersistenceManagerFactory来管理事务,
	 * 而不是TransactionAwarePersistenceManagerFactoryProxy.
	 * 只有数据访问代码可以与TransactionAwarePersistenceManagerFactoryProxy一起使用,
	 * 而事务管理器需要处理底层目标PersistenceManagerFactory.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		this.persistenceManagerFactory = pmf;
	}

	/**
	 * 返回此实例应为其管理事务的PersistenceManagerFactory.
	 */
	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return this.persistenceManagerFactory;
	}

	/**
	 * 设置此实例应为其管理事务的JDBC DataSource.
	 * DataSource应该与JDO PersistenceManagerFactory使用的数据源匹配:
	 * 例如, 可以为两者指定相同的JNDI数据源.
	 * <p>如果PersistenceManagerFactory使用DataSource作为连接工厂, 将自动检测DataSource:
	 * 仍然可以显式指定DataSource, 但在这种情况下不需要.
	 * <p>此DataSource的事务JDBC连接将提供给通过DataSourceUtils或JdbcTemplate直接访问此DataSource的应用程序代码.
	 * Connection将从JDO PersistenceManager中获取.
	 * <p>请注意, 需要为特定JDO提供者使用JDO方言, 以允许将JDO事务公开为JDBC事务.
	 * <p>此处指定的DataSource应该是用于管理事务的目标DataSource, 而不是TransactionAwareDataSourceProxy.
	 * 只有数据访问代码可以与TransactionAwareDataSourceProxy一起使用, 而事务管理器需要处理底层目标DataSource.
	 * 如果传入了TransactionAwareDataSourceProxy, 它将被解包以提取其目标DataSource.
	 */
	public void setDataSource(DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// 如果是TransactionAwareDataSourceProxy, 需要为其底层目标DataSource执行事务,
			// 否则数据访问代码将看不到正确公开的事务 (i.e. 目标DataSource的事务).
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		}
		else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * 返回此实例为其管理事务的JDBC DataSource.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 设置是否自动检测JDO PersistenceManagerFactory使用的JDBC DataSource, 由{@code getConnectionFactory()}方法返回.
	 * 默认 "true".
	 * <p>可以关闭以故意忽略可用的DataSource, 以便不将JDO事务公开为该DataSource的JDBC事务.
	 */
	public void setAutodetectDataSource(boolean autodetectDataSource) {
		this.autodetectDataSource = autodetectDataSource;
	}

	/**
	 * 设置用于此事务管理器的JDO方言.
	 * <p>方言对象可用于检索底层JDBC连接, 从而允许将JDO事务公开为JDBC事务.
	 */
	public void setJdoDialect(JdoDialect jdoDialect) {
		this.jdoDialect = jdoDialect;
	}

	/**
	 * 返回用于此事务管理器的JDO方言.
	 * <p>如果未设置, 则为指定的PersistenceManagerFactory创建默认值.
	 */
	public JdoDialect getJdoDialect() {
		if (this.jdoDialect == null) {
			this.jdoDialect = new DefaultJdoDialect();
		}
		return this.jdoDialect;
	}

	/**
	 * 实时地初始化JDO方言, 如果没有设置, 则为指定的PersistenceManagerFactory创建默认方言.
	 * 自动检测PersistenceManagerFactory的DataSource.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getPersistenceManagerFactory() == null) {
			throw new IllegalArgumentException("Property 'persistenceManagerFactory' is required");
		}
		// 如果没有明确指定, 则构建默认的JdoDialect.
		if (this.jdoDialect == null) {
			this.jdoDialect = new DefaultJdoDialect(getPersistenceManagerFactory().getConnectionFactory());
		}

		// 检查DataSource是否为连接工厂.
		if (this.autodetectDataSource && getDataSource() == null) {
			Object pmfcf = getPersistenceManagerFactory().getConnectionFactory();
			if (pmfcf instanceof DataSource) {
				// 使用PersistenceManagerFactory的DataSource将事务公开给JDBC代码.
				this.dataSource = (DataSource) pmfcf;
				if (logger.isInfoEnabled()) {
					logger.info("Using DataSource [" + this.dataSource +
							"] of JDO PersistenceManagerFactory for JdoTransactionManager");
				}
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return getPersistenceManagerFactory();
	}

	@Override
	protected Object doGetTransaction() {
		JdoTransactionObject txObject = new JdoTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		PersistenceManagerHolder pmHolder = (PersistenceManagerHolder)
				TransactionSynchronizationManager.getResource(getPersistenceManagerFactory());
		if (pmHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound PersistenceManager [" +
						pmHolder.getPersistenceManager() + "] for JDO transaction");
			}
			txObject.setPersistenceManagerHolder(pmHolder, false);
		}

		if (getDataSource() != null) {
			ConnectionHolder conHolder = (ConnectionHolder)
					TransactionSynchronizationManager.getResource(getDataSource());
			txObject.setConnectionHolder(conHolder);
		}

		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((JdoTransactionObject) transaction).hasTransaction();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JdoTransactionObject txObject = (JdoTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! JdoTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single JdoTransactionManager for all transactions " +
					"on a single DataSource, no matter whether JDO or JDBC access.");
		}

		PersistenceManager pm;

		try {
			if (txObject.getPersistenceManagerHolder() == null ||
					txObject.getPersistenceManagerHolder().isSynchronizedWithTransaction()) {
				PersistenceManager newPm = getPersistenceManagerFactory().getPersistenceManager();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new PersistenceManager [" + newPm + "] for JDO transaction");
				}
				txObject.setPersistenceManagerHolder(new PersistenceManagerHolder(newPm), true);
			}

			pm = txObject.getPersistenceManagerHolder().getPersistenceManager();

			// 委托给JdoDialect开始实际事务.
			final int timeoutToUse = determineTimeout(definition);
			Object transactionData = getJdoDialect().beginTransaction(pm.currentTransaction(),
					new DelegatingTransactionDefinition(definition) {
						@Override
						public int getTimeout() {
							return timeoutToUse;
						}
					});
			txObject.setTransactionData(transactionData);

			// 注册事务超时.
			if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getPersistenceManagerHolder().setTimeoutInSeconds(timeoutToUse);
			}

			// 如果设置, 请为DataSource注册JDO PersistenceManager的JDBC连接.
			if (getDataSource() != null) {
				ConnectionHandle conHandle = getJdoDialect().getJdbcConnection(pm, definition.isReadOnly());
				if (conHandle != null) {
					ConnectionHolder conHolder = new ConnectionHolder(conHandle);
					if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
						conHolder.setTimeoutInSeconds(timeoutToUse);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing JDO transaction as JDBC transaction [" +
								conHolder.getConnectionHandle() + "]");
					}
					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
					txObject.setConnectionHolder(conHolder);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not exposing JDO transaction [" + pm + "] as JDBC transaction because " +
								"JdoDialect [" + getJdoDialect() + "] does not support JDBC Connection retrieval");
					}
				}
			}

			// 将持久性管理器持有者绑定到线程.
			if (txObject.isNewPersistenceManagerHolder()) {
				TransactionSynchronizationManager.bindResource(
						getPersistenceManagerFactory(), txObject.getPersistenceManagerHolder());
			}
			txObject.getPersistenceManagerHolder().setSynchronizedWithTransaction(true);
		}

		catch (TransactionException ex) {
			closePersistenceManagerAfterFailedBegin(txObject);
			throw ex;
		}
		catch (Throwable ex) {
			closePersistenceManagerAfterFailedBegin(txObject);
			throw new CannotCreateTransactionException("Could not open JDO PersistenceManager for transaction", ex);
		}
	}

	/**
	 * 关闭当前事务的EntityManager.
	 * 在事务开始尝试失败后调用.
	 * 
	 * @param txObject 当前事务
	 */
	protected void closePersistenceManagerAfterFailedBegin(JdoTransactionObject txObject) {
		if (txObject.isNewPersistenceManagerHolder()) {
			PersistenceManager pm = txObject.getPersistenceManagerHolder().getPersistenceManager();
			try {
				if (pm.currentTransaction().isActive()) {
					pm.currentTransaction().rollback();
				}
			}
			catch (Throwable ex) {
				logger.debug("Could not rollback PersistenceManager after failed transaction begin", ex);
			}
			finally {
				PersistenceManagerFactoryUtils.releasePersistenceManager(pm, getPersistenceManagerFactory());
			}
			txObject.setPersistenceManagerHolder(null, false);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JdoTransactionObject txObject = (JdoTransactionObject) transaction;
		txObject.setPersistenceManagerHolder(null, false);
		PersistenceManagerHolder persistenceManagerHolder = (PersistenceManagerHolder)
				TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(persistenceManagerHolder, connectionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(
				getPersistenceManagerFactory(), resourcesHolder.getPersistenceManagerHolder());
		if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	/**
	 * 此实现返回"true": JDO提交将正确处理已在全局级别标记为仅回滚的事务.
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JdoTransactionObject txObject = (JdoTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing JDO transaction on PersistenceManager [" +
					txObject.getPersistenceManagerHolder().getPersistenceManager() + "]");
		}
		try {
			Transaction tx = txObject.getPersistenceManagerHolder().getPersistenceManager().currentTransaction();
			tx.commit();
		}
		catch (JDOException ex) {
			// 可能未能刷新数据库更改.
			throw convertJdoAccessException(ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JdoTransactionObject txObject = (JdoTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back JDO transaction on PersistenceManager [" +
					txObject.getPersistenceManagerHolder().getPersistenceManager() + "]");
		}
		try {
			Transaction tx = txObject.getPersistenceManagerHolder().getPersistenceManager().currentTransaction();
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		catch (JDOException ex) {
			throw new TransactionSystemException("Could not roll back JDO transaction", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JdoTransactionObject txObject = (JdoTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JDO transaction on PersistenceManager [" +
					txObject.getPersistenceManagerHolder().getPersistenceManager() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JdoTransactionObject txObject = (JdoTransactionObject) transaction;

		// 从线程中删除持久性管理器持有者.
		if (txObject.isNewPersistenceManagerHolder()) {
			TransactionSynchronizationManager.unbindResource(getPersistenceManagerFactory());
		}
		txObject.getPersistenceManagerHolder().clear();

		// 如果公开, 则从线程中删除JDBC连接器.
		if (txObject.hasConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
			try {
				getJdoDialect().releaseJdbcConnection(txObject.getConnectionHolder().getConnectionHandle(),
						txObject.getPersistenceManagerHolder().getPersistenceManager());
			}
			catch (Throwable ex) {
				// 只需记录它, 以保持与事务相关的异常.
				logger.debug("Could not release JDBC connection after transaction", ex);
			}
		}

		getJdoDialect().cleanupTransaction(txObject.getTransactionData());

		if (txObject.isNewPersistenceManagerHolder()) {
			PersistenceManager pm = txObject.getPersistenceManagerHolder().getPersistenceManager();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing JDO PersistenceManager [" + pm + "] after transaction");
			}
			PersistenceManagerFactoryUtils.releasePersistenceManager(pm, getPersistenceManagerFactory());
		}
		else {
			logger.debug("Not closing pre-bound JDO PersistenceManager after transaction");
		}
	}

	/**
	 * 将给定的JDOException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * <p>默认实现委托给JdoDialect.
	 * 可以在子类中重写.
	 * 
	 * @param ex 发生的JDOException
	 * 
	 * @return 相应的DataAccessException实例
	 */
	protected DataAccessException convertJdoAccessException(JDOException ex) {
		return getJdoDialect().translateException(ex);
	}


	/**
	 * JDO事务对象, 表示PersistenceManagerHolder.
	 * 由JdoTransactionManager用作事务对象.
	 */
	private class JdoTransactionObject extends JdbcTransactionObjectSupport {

		private PersistenceManagerHolder persistenceManagerHolder;

		private boolean newPersistenceManagerHolder;

		private Object transactionData;

		public void setPersistenceManagerHolder(
				PersistenceManagerHolder persistenceManagerHolder, boolean newPersistenceManagerHolder) {
			this.persistenceManagerHolder = persistenceManagerHolder;
			this.newPersistenceManagerHolder = newPersistenceManagerHolder;
		}

		public PersistenceManagerHolder getPersistenceManagerHolder() {
			return this.persistenceManagerHolder;
		}

		public boolean isNewPersistenceManagerHolder() {
			return this.newPersistenceManagerHolder;
		}

		public boolean hasTransaction() {
			return (this.persistenceManagerHolder != null && this.persistenceManagerHolder.isTransactionActive());
		}

		public void setTransactionData(Object transactionData) {
			this.transactionData = transactionData;
			this.persistenceManagerHolder.setTransactionActive(true);
		}

		public Object getTransactionData() {
			return this.transactionData;
		}

		public void setRollbackOnly() {
			Transaction tx = this.persistenceManagerHolder.getPersistenceManager().currentTransaction();
			if (tx.isActive()) {
				tx.setRollbackOnly();
			}
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			Transaction tx = this.persistenceManagerHolder.getPersistenceManager().currentTransaction();
			return tx.getRollbackOnly();
		}

		@Override
		public void flush() {
			try {
				this.persistenceManagerHolder.getPersistenceManager().flush();
			}
			catch (JDOException ex) {
				throw convertJdoAccessException(ex);
			}
		}
	}


	/**
	 * 暂停的资源的保存器.
	 * 由{@code doSuspend}和{@code doResume}在内部使用.
	 */
	private static class SuspendedResourcesHolder {

		private final PersistenceManagerHolder persistenceManagerHolder;

		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(PersistenceManagerHolder pmHolder, ConnectionHolder conHolder) {
			this.persistenceManagerHolder = pmHolder;
			this.connectionHolder = conHolder;
		}

		private PersistenceManagerHolder getPersistenceManagerHolder() {
			return this.persistenceManagerHolder;
		}

		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}

}
