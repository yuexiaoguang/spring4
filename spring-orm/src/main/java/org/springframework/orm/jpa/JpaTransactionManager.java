package org.springframework.orm.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}实现,
 * 用于单个 JPA {@link javax.persistence.EntityManagerFactory}.
 * 将JPA EntityManager从指定的工厂绑定到线程, 可能允许每个工厂使用一个线程绑定的EntityManager.
 * {@link SharedEntityManagerCreator}和{@code @PersistenceContext}知道线程绑定的实体管理器, 并自动参与此类事务.
 * 支持此事务管理机制的JPA访问代码需要使用其中任何一个.
 *
 * <p>此事务管理器适用于使用单个JPA EntityManagerFactory进行事务数据访问的应用程序.
 * JTA (通常通过{@link org.springframework.transaction.jta.JtaTransactionManager}) 是访问同一事务中的多个事务资源所必需的.
 * 请注意, 需要相应地配置JPA提供者, 以使其参与JTA事务.
 *
 * <p>此事务管理器还支持事务中的直接DataSource访问 (i.e. 使用相同DataSource的纯JDBC代码).
 * 这允许混合访问JPA的服务和使用普通JDBC的服务 (不知道JPA)!
 * 应用程序代码需要遵循与
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}相同的简单连接查找模式
 * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * 或通过
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
 * 请注意, 这需要配置特定于供应商的{@link JpaDialect}.
 *
 * <p>Note: 为了能够为普通的JDBC代码注册DataSource的Connection, 这个实例需要知道 DataSource ({@link #setDataSource}).
 * 给定的DataSource显然应该与给定的EntityManagerFactory使用的数据源匹配.
 * 此事务管理器将自动检测用作EntityManagerFactory的连接工厂的DataSource, 因此通常不需要显式指定"dataSource"属性.
 *
 * <p>此事务管理器通过JDBC 3.0 Savepoints支持嵌套事务.
 * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"}标志默认为{@code false},
 * 因为嵌套事务只适用于JDBC连接, 而不适用于JPA EntityManager及其缓存的实体对象和相关上下文.
 * 如果要对参与JPA事务的JDBC访问代码使用嵌套事务 (假设JDBC驱动程序支持Savepoints),
 * 可以手动将标志设置为{@code true}.
 * <i>请注意, JPA本身不支持嵌套事务! 因此, 不要指望JPA访问代码在语义上参与嵌套事务.</i>
 */
@SuppressWarnings("serial")
public class JpaTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	private EntityManagerFactory entityManagerFactory;

	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();

	private DataSource dataSource;

	private JpaDialect jpaDialect = new DefaultJpaDialect();


	/**
	 * <p>必须将EntityManagerFactory设置为能够使用它.
	 */
	public JpaTransactionManager() {
		setNestedTransactionAllowed(true);
	}

	/**
	 * @param emf 要为其管理事务的EntityManagerFactory
	 */
	public JpaTransactionManager(EntityManagerFactory emf) {
		this();
		this.entityManagerFactory = emf;
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的EntityManagerFactory.
	 * <p>或者, 指定目标EntityManagerFactory的持久化单元名称.
	 * 默认情况下, 将通过在BeanFactory中查找EntityManagerFactory类型的单个唯一bean来检索默认的EntityManagerFactory.
	 */
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * 返回此实例应为其管理事务的EntityManagerFactory.
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * 设置管理其事务的持久化单元的名称.
	 * <p>这是通过直接引用指定EntityManagerFactory的替代方法, 通过其持久化单元名称来解析它.
	 * 如果未指定EntityManagerFactory且没有指定持久化单元名称,
	 * 则将通过查找EntityManagerFactory类型的单个唯一bean来检索默认的EntityManagerFactory.
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回管理其事务的持久化单元的名称.
	 */
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定JPA属性, 传递到{@code EntityManagerFactory.createEntityManager(Map)}.
	 * <p>可以使用String "value" (通过PropertiesEditor解析)或XML bean定义中的"props"元素填充.
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 指定JPA属性, 以传递到{@code EntityManagerFactory.createEntityManager(Map)}.
	 * <p>可以在XML bean定义中使用"map" 或"props"元素填充.
	 */
	public void setJpaPropertyMap(Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许将对JPA属性的Map访问权限传递给持久化提供者, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"jpaPropertyMap[myKey]".
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * 设置此实例应为其管理事务的JDBC DataSource.
	 * DataSource应该与JPA EntityManagerFactory使用的数据源匹配:
	 * 例如, 可以为两者指定相同的JNDI数据源.
	 * <p>如果EntityManagerFactory使用已知的DataSource作为其连接工厂, 则将自动检测DataSource:
	 * 仍然可以显式指定DataSource, 但在这种情况下不需要.
	 * <p>将通过DataSourceUtils或JdbcTemplate直接访问此DataSource的应用程序代码, 提供此DataSource的事务JDBC连接.
	 * Connection将从JPA EntityManager中获取.
	 * <p>请注意, 需要为特定JPA实现使用JPA方言, 以允许将JPA事务公开为JDBC事务.
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
	 * 设置用于此事务管理器的JPA方言.
	 * 用于特定于供应商的事务管理和JDBC连接公开.
	 * <p>如果EntityManagerFactory使用已知的JpaDialect, 它将被自动检测:
	 * 仍然可以显式指定DataSource, 但在这种情况下不需要.
	 * <p>方言对象可用于检索底层JDBC连接, 从而允许将JPA事务公开为JDBC事务.
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = (jpaDialect != null ? jpaDialect : new DefaultJpaDialect());
	}

	/**
	 * 返回用于此事务管理器的JPA方言.
	 */
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * 如果没有显式设置, 则通过持久化单元名称检索EntityManagerFactory.
	 * 如果未指定持久化单元, 则回退到默认的EntityManagerFactory bean.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}

	/**
	 * 实时初始化JPA方言, 如果没有设置, 则为指定的EntityManagerFactory创建默认方言.
	 * 自动检测EntityManagerFactory的DataSource.
	 */
	@Override
	public void afterPropertiesSet() {
		if (getEntityManagerFactory() == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		if (getEntityManagerFactory() instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) getEntityManagerFactory();
			DataSource dataSource = emfInfo.getDataSource();
			if (dataSource != null) {
				setDataSource(dataSource);
			}
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				setJpaDialect(jpaDialect);
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return getEntityManagerFactory();
	}

	@Override
	protected Object doGetTransaction() {
		JpaTransactionObject txObject = new JpaTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		EntityManagerHolder emHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.getResource(getEntityManagerFactory());
		if (emHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound EntityManager [" + emHolder.getEntityManager() +
						"] for JPA transaction");
			}
			txObject.setEntityManagerHolder(emHolder, false);
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
		return ((JpaTransactionObject) transaction).hasTransaction();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! JpaTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single JpaTransactionManager for all transactions " +
					"on a single DataSource, no matter whether JPA or JDBC access.");
		}

		try {
			if (txObject.getEntityManagerHolder() == null ||
					txObject.getEntityManagerHolder().isSynchronizedWithTransaction()) {
				EntityManager newEm = createEntityManagerForTransaction();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new EntityManager [" + newEm + "] for JPA transaction");
				}
				txObject.setEntityManagerHolder(new EntityManagerHolder(newEm), true);
			}

			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();

			// 委托给JpaDialect, 开始实际事务.
			final int timeoutToUse = determineTimeout(definition);
			Object transactionData = getJpaDialect().beginTransaction(em,
					new DelegatingTransactionDefinition(definition) {
						@Override
						public int getTimeout() {
							return timeoutToUse;
						}
					});
			txObject.setTransactionData(transactionData);

			// 注册事务超时.
			if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getEntityManagerHolder().setTimeoutInSeconds(timeoutToUse);
			}

			// 如果设置, 则为DataSource注册JPA EntityManager的JDBC连接.
			if (getDataSource() != null) {
				ConnectionHandle conHandle = getJpaDialect().getJdbcConnection(em, definition.isReadOnly());
				if (conHandle != null) {
					ConnectionHolder conHolder = new ConnectionHolder(conHandle);
					if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
						conHolder.setTimeoutInSeconds(timeoutToUse);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Exposing JPA transaction as JDBC transaction [" +
								conHolder.getConnectionHandle() + "]");
					}
					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
					txObject.setConnectionHolder(conHolder);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Not exposing JPA transaction [" + em + "] as JDBC transaction because " +
								"JpaDialect [" + getJpaDialect() + "] does not support JDBC Connection retrieval");
					}
				}
			}

			// 将实体管理器持有者绑定到线程.
			if (txObject.isNewEntityManagerHolder()) {
				TransactionSynchronizationManager.bindResource(
						getEntityManagerFactory(), txObject.getEntityManagerHolder());
			}
			txObject.getEntityManagerHolder().setSynchronizedWithTransaction(true);
		}

		catch (TransactionException ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw ex;
		}
		catch (Throwable ex) {
			closeEntityManagerAfterFailedBegin(txObject);
			throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", ex);
		}
	}

	/**
	 * 创建一个用于事务的JPA EntityManager.
	 * <p>默认实现检查EntityManagerFactory是否是Spring代理, 并首先解包它.
	 */
	protected EntityManager createEntityManagerForTransaction() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf instanceof EntityManagerFactoryInfo) {
			emf = ((EntityManagerFactoryInfo) emf).getNativeEntityManagerFactory();
		}
		Map<String, Object> properties = getJpaPropertyMap();
		return (!CollectionUtils.isEmpty(properties) ?
				emf.createEntityManager(properties) : emf.createEntityManager());
	}

	/**
	 * 关闭当前事务的EntityManager.
	 * 在事务开始尝试失败后调用.
	 * 
	 * @param txObject 当前事务
	 */
	protected void closeEntityManagerAfterFailedBegin(JpaTransactionObject txObject) {
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			try {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}
			}
			catch (Throwable ex) {
				logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
			}
			finally {
				EntityManagerFactoryUtils.closeEntityManager(em);
			}
			txObject.setEntityManagerHolder(null, false);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
		txObject.setEntityManagerHolder(null, false);
		EntityManagerHolder entityManagerHolder = (EntityManagerHolder)
				TransactionSynchronizationManager.unbindResource(getEntityManagerFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(entityManagerHolder, connectionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(
				getEntityManagerFactory(), resourcesHolder.getEntityManagerHolder());
		if (getDataSource() != null && resourcesHolder.getConnectionHolder() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	/**
	 * 此实现返回"true":
	 * JPA提交将正确处理已在全局级别标记为仅回滚的事务.
	 */
	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			tx.commit();
		}
		catch (RollbackException ex) {
			if (ex.getCause() instanceof RuntimeException) {
				DataAccessException dex = getJpaDialect().translateExceptionIfPossible((RuntimeException) ex.getCause());
				if (dex != null) {
					throw dex;
				}
			}
			throw new TransactionSystemException("Could not commit JPA transaction", ex);
		}
		catch (RuntimeException ex) {
			// 可能未能刷新更改到数据库.
			throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "]");
		}
		try {
			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		catch (PersistenceException ex) {
			throw new TransactionSystemException("Could not roll back JPA transaction", ex);
		}
		finally {
			if (!txObject.isNewEntityManagerHolder()) {
				// 清除EntityManager中所有挂起的插入/更新/删除.
				// 必要的预绑定EntityManager, 以避免不一致的状态.
				txObject.getEntityManagerHolder().getEntityManager().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting JPA transaction on EntityManager [" +
					txObject.getEntityManagerHolder().getEntityManager() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JpaTransactionObject txObject = (JpaTransactionObject) transaction;

		// 如果仍然存在, 则从线程中删除实体管理器持有者.
		// (可能已被EntityManagerFactoryUtils删除, 以便用非同步的EntityManager替换它).
		if (txObject.isNewEntityManagerHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(getEntityManagerFactory());
		}
		txObject.getEntityManagerHolder().clear();

		// 如果公开, 则从线程中删除JDBC连接器.
		if (txObject.hasConnectionHolder()) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
			try {
				getJpaDialect().releaseJdbcConnection(txObject.getConnectionHolder().getConnectionHandle(),
						txObject.getEntityManagerHolder().getEntityManager());
			}
			catch (Exception ex) {
				// 只需记录它, 以保持与事务相关的异常.
				logger.error("Could not close JDBC connection after transaction", ex);
			}
		}

		getJpaDialect().cleanupTransaction(txObject.getTransactionData());

		// 从线程中删除实体管理器持有者.
		if (txObject.isNewEntityManagerHolder()) {
			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing JPA EntityManager [" + em + "] after transaction");
			}
			EntityManagerFactoryUtils.closeEntityManager(em);
		}
		else {
			logger.debug("Not closing pre-bound JPA EntityManager after transaction");
		}
	}


	/**
	 * JPA事务对象, 表示EntityManagerHolder.
	 * 由JpaTransactionManager用作事务对象.
	 */
	private class JpaTransactionObject extends JdbcTransactionObjectSupport {

		private EntityManagerHolder entityManagerHolder;

		private boolean newEntityManagerHolder;

		private Object transactionData;

		public void setEntityManagerHolder(
				EntityManagerHolder entityManagerHolder, boolean newEntityManagerHolder) {
			this.entityManagerHolder = entityManagerHolder;
			this.newEntityManagerHolder = newEntityManagerHolder;
		}

		public EntityManagerHolder getEntityManagerHolder() {
			return this.entityManagerHolder;
		}

		public boolean isNewEntityManagerHolder() {
			return this.newEntityManagerHolder;
		}

		public boolean hasTransaction() {
			return (this.entityManagerHolder != null && this.entityManagerHolder.isTransactionActive());
		}

		public void setTransactionData(Object transactionData) {
			this.transactionData = transactionData;
			this.entityManagerHolder.setTransactionActive(true);
			if (transactionData instanceof SavepointManager) {
				this.entityManagerHolder.setSavepointManager((SavepointManager) transactionData);
			}
		}

		public Object getTransactionData() {
			return this.transactionData;
		}

		public void setRollbackOnly() {
			EntityTransaction tx = this.entityManagerHolder.getEntityManager().getTransaction();
			if (tx.isActive()) {
				tx.setRollbackOnly();
			}
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			EntityTransaction tx = this.entityManagerHolder.getEntityManager().getTransaction();
			return tx.getRollbackOnly();
		}

		@Override
		public void flush() {
			try {
				this.entityManagerHolder.getEntityManager().flush();
			}
			catch (RuntimeException ex) {
				throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
			}
		}

		@Override
		public Object createSavepoint() throws TransactionException {
			return getSavepointManager().createSavepoint();
		}

		@Override
		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().rollbackToSavepoint(savepoint);
		}

		@Override
		public void releaseSavepoint(Object savepoint) throws TransactionException {
			getSavepointManager().releaseSavepoint(savepoint);
		}

		private SavepointManager getSavepointManager() {
			if (!isSavepointAllowed()) {
				throw new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions");
			}
			SavepointManager savepointManager = getEntityManagerHolder().getSavepointManager();
			if (savepointManager == null) {
				throw new NestedTransactionNotSupportedException(
						"JpaDialect does not support savepoints - check your JPA provider's capabilities");
			}
			return savepointManager;
		}
	}


	/**
	 * 暂停资源的持有者.
	 * 由{@code doSuspend}和{@code doResume}在内部使用.
	 */
	private static class SuspendedResourcesHolder {

		private final EntityManagerHolder entityManagerHolder;

		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(EntityManagerHolder emHolder, ConnectionHolder conHolder) {
			this.entityManagerHolder = emHolder;
			this.connectionHolder = conHolder;
		}

		private EntityManagerHolder getEntityManagerHolder() {
			return this.entityManagerHolder;
		}

		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}
}
