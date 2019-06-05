package org.springframework.orm.hibernate5;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 单个Hibernate的{@link SessionFactory}的{@link org.springframework.transaction.PlatformTransactionManager}实现.
 * 将Hibernate会话从指定的工厂绑定到线程, 可能允许每个工厂一个线程绑定的Session.
 * 需要支持此事务处理机制的Hibernate访问代码需要{@code SessionFactory.getCurrentSession()},
 * 使用{@link SpringSessionContext}配置SessionFactory.
 *
 * <p>支持自定义隔离级别, 以及作为Hibernate事务超时应用的超时.
 *
 * <p>此事务管理器适用于使用单个Hibernate SessionFactory进行事务数据访问的应用程序,
 * 但它也支持事务中的直接DataSource访问 (i.e. 使用相同DataSource的纯JDBC代码).
 * 这允许混合访问Hibernate的服务和使用普通JDBC的服务 (不知道 Hibernate)!
 * 应用程序代码需要遵循与{@link org.springframework.jdbc.datasource.DataSourceTransactionManager}相同的简单连接查找模式
 * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
 * 或通过
 * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
 *
 * <p>Note: 为了能够为普通的JDBC代码注册DataSource的Connection, 这个实例需要知道DataSource ({@link #setDataSource}).
 * 给定的DataSource显然应该与给定的SessionFactory使用的数据源匹配.
 *
 * <p>JTA (通常通过{@link org.springframework.transaction.jta.JtaTransactionManager})是访问同一事务中的多个事务资源所必需的.
 * Hibernate使用的DataSource需要在这种情况下启用JTA (请参阅容器设置).
 *
 * <p>此事务管理器通过JDBC 3.0 Savepoints支持嵌套事务.
 * {@link #setNestedTransactionAllowed} "nestedTransactionAllowed"}标志默认为"false",
 * 因为嵌套事务只适用于JDBC连接, 而不适用于Hibernate会话及其缓存的实体对象和相关上下文.
 * 如果要对参与Hibernate事务的JDBC访问代码使用嵌套事务 (假设JDBC驱动程序支持Savepoints),
 * 则可以手动将标志设置为"true".
 * <i>请注意, Hibernate本身不支持嵌套事务! 因此, 不要指望Hibernate访问代码在语义上参与嵌套事务.</i>
 */
@SuppressWarnings("serial")
public class HibernateTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	private SessionFactory sessionFactory;

	private DataSource dataSource;

	private boolean autodetectDataSource = true;

	private boolean prepareConnection = true;

	private boolean allowResultAccessAfterCompletion = false;

	private boolean hibernateManagedSession = false;

	private Object entityInterceptor;

	/**
	 * 只需要entityInterceptorBeanName.
	 */
	private BeanFactory beanFactory;


	/**
	 * 必须设置SessionFactory才能使用它.
	 */
	public HibernateTransactionManager() {
	}

	/**
	 * @param sessionFactory 要为其管理事务的SessionFactory
	 */
	public HibernateTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应为其管理事务的SessionFactory.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回此实例应为其管理事务的SessionFactory.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * 设置此实例应为其管理事务的JDBC DataSource.
	 * DataSource应该与Hibernate SessionFactory使用的数据源匹配:
	 * 例如, 可以为两者指定相同的JNDI数据源.
	 * <p>如果使用LocalDataSourceConnectionProvider配置了SessionFactory,
	 * i.e. 通过Spring的使用指定的"dataSource"的LocalSessionFactoryBean, 将自动检测DataSource:
	 * 仍然可以显式指定DataSource, 但在这种情况下不需要.
	 * <p>此DataSource的事务JDBC连接将提供给通过DataSourceUtils或JdbcTemplate直接访问此DataSource的应用程序代码.
	 * 将从Hibernate Session中获取的Connection.
	 * <p>此处指定的DataSource应该是用于管理事务的目标DataSource, 而不是TransactionAwareDataSourceProxy.
	 * 只有数据访问代码可以使用TransactionAwareDataSourceProxy, 而事务管理器需要底层目标DataSource.
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
	 * 设置是否自动检测Hibernate SessionFactory使用的JDBC DataSource,
	 * 如果通过LocalSessionFactoryBean的{@code setDataSource}设置.
	 * 默认"true".
	 * <p>可以关闭以故意忽略可用的DataSource, 以便不将Hibernate事务公开为该DataSource的JDBC事务.
	 */
	public void setAutodetectDataSource(boolean autodetectDataSource) {
		this.autodetectDataSource = autodetectDataSource;
	}

	/**
	 * 设置是否准备事务性Hibernate会话的底层JDBC连接, 即是否将特定于事务的隔离级别和/或事务的只读标志应用于底层JDBC连接.
	 * <p>默认"true".
	 * 如果关闭此标志, 事务管理器将不再支持每个事务的隔离级别.
	 * 对于只读事务, 它也不会调用{@code Connection.setReadOnly(true)}.
	 * 如果关闭此标志, 则事务后不需要清除JDBC连接, 因为不会修改任何连接设置.
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.prepareConnection = prepareConnection;
	}

	/**
	 * 设置在完成后是否允许结果访问, 通常通过Hibernate的ScrollableResults机制.
	 * <p>默认 "false".
	 * 打开此标志会强制底层JDBC连接上的过度提交可保持性 (如果打开{@link #prepareConnection "prepareConnection"}),
	 * 并跳过完成时断开连接步骤.
	 */
	public void setAllowResultAccessAfterCompletion(boolean allowResultAccessAfterCompletion) {
		this.allowResultAccessAfterCompletion = allowResultAccessAfterCompletion;
	}

	/**
	 * 设置是否在Hibernate管理的Session而不是Spring管理的Session上操作,
	 * 即是否通过Hibernate的{@link SessionFactory#getCurrentSession()}获取Session,
	 * 而不是{@link SessionFactory#openSession()}获取Session (在它之前使用Spring {@link TransactionSynchronizationManager}检查).
	 * <p>默认"false", i.e. 使用Spring管理的Session:
	 * 获取当前线程绑定的Session (e.g. 在可用的Open-Session-in-View场景中), 否则为当前事务创建新的Session.
	 * <p>将此标志切换为 "true"以强制使用Hibernate管理的会话.
	 * 请注意, 这需要{@link SessionFactory#getCurrentSession()}在调用Spring管理的事务时始终返回正确的Session;
	 * 如果{@code getCurrentSession()}调用失败, 则事务开始将失败.
	 * <p>此模式通常与自定义Hibernate {@link org.hibernate.context.spi.CurrentSessionContext}实现结合使用,
	 * 该实现将Sessions存储在Spring的TransactionSynchronizationManager以外的位置.
	 * 它也可以与Spring的Open-Session-in-View支持结合使用 (使用Spring的默认{@link SpringSessionContext}),
	 * 在这种情况下它与Spring管理的Session模式略有不同:
	 * 在这种情况下, 预绑定会话将<i>不会</i>接收{@code clear()}调用 (在回滚时)或{@code disconnect()}调用 (在事务完成时);
	 * 这相当于自定义CurrentSessionContext实现 (如果需要).
	 */
	public void setHibernateManagedSession(boolean hibernateManagedSession) {
		this.hibernateManagedSession = hibernateManagedSession;
	}

	/**
	 * 设置Hibernate实体拦截器的bean名称, 允许在写入和读取数据库之前检查和更改属性值.
	 * 将应用于此事务管理器创建的任何新会话.
	 * <p>需要知道bean工厂, 才能在会话创建时将bean名称解析为拦截器实例.
	 * 通常用于原型拦截器, i.e. 每个会话的新拦截器实例.
	 * <p>也可以用于共享拦截器实例, 但建议在这种情况下直接设置拦截器引用.
	 * 
	 * @param entityInterceptorBeanName bean工厂中实体拦截器的名称
	 */
	public void setEntityInterceptorBeanName(String entityInterceptorBeanName) {
		this.entityInterceptor = entityInterceptorBeanName;
	}

	/**
	 * 设置一个Hibernate实体拦截器, 允许在写入和读取数据库之前检查和更改属性值.
	 * 将应用于此事务管理器创建的任何新会话.
	 * <p>这样的拦截器既可以在SessionFactory级别设置, i.e. 在LocalSessionFactoryBean上,
	 * 也可以在Session级别设置, i.e. 在HibernateTransactionManager上.
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * 返回当前的Hibernate实体拦截器, 或{@code null}.
	 * 通过bean工厂解析实体拦截器bean名称.
	 * 
	 * @throws IllegalStateException 如果指定了bean名称但没有设置bean工厂
	 * @throws BeansException 如果通过bean工厂解析bean名称失败
	 */
	public Interceptor getEntityInterceptor() throws IllegalStateException, BeansException {
		if (this.entityInterceptor instanceof Interceptor) {
			return (Interceptor) entityInterceptor;
		}
		else if (this.entityInterceptor instanceof String) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("Cannot get entity interceptor via bean name if no bean factory set");
			}
			String beanName = (String) this.entityInterceptor;
			return this.beanFactory.getBean(beanName, Interceptor.class);
		}
		else {
			return null;
		}
	}

	/**
	 * 只需要知道bean工厂来解析实体拦截器bean名称.
	 * 不需要为任何其他操作模式设置它.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
		if (this.entityInterceptor instanceof String && this.beanFactory == null) {
			throw new IllegalArgumentException("Property 'beanFactory' is required for 'entityInterceptorBeanName'");
		}

		// 检查SessionFactory的DataSource.
		if (this.autodetectDataSource && getDataSource() == null) {
			DataSource sfds = SessionFactoryUtils.getDataSource(getSessionFactory());
			if (sfds != null) {
				// 使用SessionFactory的DataSource将事务公开给JDBC代码.
				if (logger.isInfoEnabled()) {
					logger.info("Using DataSource [" + sfds +
							"] of Hibernate SessionFactory for HibernateTransactionManager");
				}
				setDataSource(sfds);
			}
		}
	}


	@Override
	public Object getResourceFactory() {
		return getSessionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		HibernateTransactionObject txObject = new HibernateTransactionObject();
		txObject.setSavepointAllowed(isNestedTransactionAllowed());

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() + "] for Hibernate transaction");
			}
			txObject.setSessionHolder(sessionHolder);
		}
		else if (this.hibernateManagedSession) {
			try {
				Session session = this.sessionFactory.getCurrentSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Found Hibernate-managed Session [" + session + "] for Spring-managed transaction");
				}
				txObject.setExistingSession(session);
			}
			catch (HibernateException ex) {
				throw new DataAccessResourceFailureException(
						"Could not obtain Hibernate-managed Session for Spring-managed transaction", ex);
			}
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
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		return (txObject.hasSpringManagedTransaction() ||
				(this.hibernateManagedSession && txObject.hasHibernateManagedTransaction()));
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
			throw new IllegalTransactionStateException(
					"Pre-bound JDBC Connection found! HibernateTransactionManager does not support " +
					"running within DataSourceTransactionManager if told to manage the DataSource itself. " +
					"It is recommended to use a single HibernateTransactionManager for all transactions " +
					"on a single DataSource, no matter whether Hibernate or JDBC access.");
		}

		Session session = null;

		try {
			if (txObject.getSessionHolder() == null || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Interceptor entityInterceptor = getEntityInterceptor();
				Session newSession = (entityInterceptor != null ?
						getSessionFactory().withOptions().interceptor(entityInterceptor).openSession() :
						getSessionFactory().openSession());
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + newSession + "] for Hibernate transaction");
				}
				txObject.setSession(newSession);
			}

			session = txObject.getSessionHolder().getSession();

			if (this.prepareConnection && isSameConnectionForEntireSession(session)) {
				// 允许更改JDBC连接的事务设置.
				if (logger.isDebugEnabled()) {
					logger.debug("Preparing JDBC Connection of Hibernate Session [" + session + "]");
				}
				Connection con = ((SessionImplementor) session).connection();
				Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
				txObject.setPreviousIsolationLevel(previousIsolationLevel);
				if (this.allowResultAccessAfterCompletion && !txObject.isNewSession()) {
					int currentHoldability = con.getHoldability();
					if (currentHoldability != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
						txObject.setPreviousHoldability(currentHoldability);
						con.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
					}
				}
			}
			else {
				// 不允许更改JDBC连接的事务设置.
				if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
					// 应该设置一个特定的隔离级别, 但不允许...
					throw new InvalidIsolationLevelException(
							"HibernateTransactionManager is not allowed to support custom isolation levels: " +
							"make sure that its 'prepareConnection' flag is on (the default) and that the " +
							"Hibernate connection release mode is set to 'on_close' (the default for JDBC).");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Not preparing JDBC Connection of Hibernate Session [" + session + "]");
				}
			}

			if (definition.isReadOnly() && txObject.isNewSession()) {
				// 如果此事务的新会话, 则设置为MANUAL.
				session.setFlushMode(FlushMode.MANUAL);
			}

			if (!definition.isReadOnly() && !txObject.isNewSession()) {
				// 对于非只读事务, 需要AUTO或COMMIT.
				FlushMode flushMode = SessionFactoryUtils.getFlushMode(session);
				if (FlushMode.MANUAL.equals(flushMode)) {
					session.setFlushMode(FlushMode.AUTO);
					txObject.getSessionHolder().setPreviousFlushMode(flushMode);
				}
			}

			Transaction hibTx;

			// 注册事务超时.
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				// 在Hibernate 3.1+上使用Hibernate自己的事务超时机制
				// 适用于所有语句, 也适用于插入, 更新和删除!
				hibTx = session.getTransaction();
				hibTx.setTimeout(timeout);
				hibTx.begin();
			}
			else {
				// 在没有指定超时的情况下打开普通的Hibernate事务.
				hibTx = session.beginTransaction();
			}

			// 将Hibernate事务添加到会话持有者.
			txObject.getSessionHolder().setTransaction(hibTx);

			// 如果设置, 请为DataSource注册Hibernate Session的JDBC连接.
			if (getDataSource() != null) {
				Connection con = ((SessionImplementor) session).connection();
				ConnectionHolder conHolder = new ConnectionHolder(con);
				if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
					conHolder.setTimeoutInSeconds(timeout);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Exposing Hibernate transaction as JDBC transaction [" + con + "]");
				}
				TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
				txObject.setConnectionHolder(conHolder);
			}

			// 将会话持有者绑定到线程.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(getSessionFactory(), txObject.getSessionHolder());
			}
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);
		}

		catch (Throwable ex) {
			if (txObject.isNewSession()) {
				try {
					if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
						session.getTransaction().rollback();
					}
				}
				catch (Throwable ex2) {
					logger.debug("Could not rollback Session after failed transaction begin", ex);
				}
				finally {
					SessionFactoryUtils.closeSession(session);
					txObject.setSessionHolder(null);
				}
			}
			throw new CannotCreateTransactionException("Could not open Hibernate Session for transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;
		txObject.setSessionHolder(null);
		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
		txObject.setConnectionHolder(null);
		ConnectionHolder connectionHolder = null;
		if (getDataSource() != null) {
			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
		}
		return new SuspendedResourcesHolder(sessionHolder, connectionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// 从活动事务同步中运行的非事务代码 -> 可以安全地删除, 将在事务完成时关闭.
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
		TransactionSynchronizationManager.bindResource(getSessionFactory(), resourcesHolder.getSessionHolder());
		if (getDataSource() != null) {
			TransactionSynchronizationManager.bindResource(getDataSource(), resourcesHolder.getConnectionHolder());
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		try {
			txObject.getSessionHolder().getTransaction().commit();
		}
		catch (org.hibernate.TransactionException ex) {
			// 可以从提交调用到底层JDBC连接
			throw new TransactionSystemException("Could not commit Hibernate transaction", ex);
		}
		catch (HibernateException ex) {
			// 可能无法刷新对数据库的更改
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}
		try {
			txObject.getSessionHolder().getTransaction().rollback();
		}
		catch (org.hibernate.TransactionException ex) {
			throw new TransactionSystemException("Could not roll back Hibernate transaction", ex);
		}
		catch (HibernateException ex) {
			// 不应该真的发生, 因为回滚不会导致刷新.
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
		finally {
			if (!txObject.isNewSession() && !this.hibernateManagedSession) {
				// 清除会话中的所有挂起的插入/更新/删除.
				// 必要的预绑定会话, 以避免不一致的状态.
				txObject.getSessionHolder().getSession().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting Hibernate transaction on Session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		txObject.setRollbackOnly();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void doCleanupAfterCompletion(Object transaction) {
		HibernateTransactionObject txObject = (HibernateTransactionObject) transaction;

		// 从线程中删除会话持有者.
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}

		// 如果公开, 则从线程中删除JDBC连接器.
		if (getDataSource() != null) {
			TransactionSynchronizationManager.unbindResource(getDataSource());
		}

		Session session = txObject.getSessionHolder().getSession();
		if (this.prepareConnection && isPhysicallyConnected(session)) {
			// 正在使用连接释放模式"on_close":
			// 可以在这里重置JDBC连接的隔离级别和/或只读标志.
			// 否则, 需要依靠连接池来执行适当的清理.
			try {
				Connection con = ((SessionImplementor) session).connection();
				Integer previousHoldability = txObject.getPreviousHoldability();
				if (previousHoldability != null) {
					con.setHoldability(previousHoldability);
				}
				DataSourceUtils.resetConnectionAfterTransaction(con, txObject.getPreviousIsolationLevel());
			}
			catch (HibernateException ex) {
				logger.debug("Could not access JDBC Connection of Hibernate Session", ex);
			}
			catch (Throwable ex) {
				logger.debug("Could not reset JDBC Connection after transaction", ex);
			}
		}

		if (txObject.isNewSession()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Hibernate Session [" + session + "] after transaction");
			}
			SessionFactoryUtils.closeSession(session);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Not closing pre-bound Hibernate Session [" + session + "] after transaction");
			}
			if (txObject.getSessionHolder().getPreviousFlushMode() != null) {
				session.setFlushMode(txObject.getSessionHolder().getPreviousFlushMode());
			}
			if (!this.allowResultAccessAfterCompletion && !this.hibernateManagedSession) {
				disconnectOnCompletion(session);
			}
		}
		txObject.getSessionHolder().clear();
	}

	/**
	 * 在事务完成时断开预先存在的Hibernate会话, 返回其数据库连接, 但保留其实体状态.
	 * <p>默认实现调用{@link Session#disconnect()}.
	 * 子类可以使用no-op或微调断开逻辑来覆盖它.
	 * 
	 * @param session 要断开连接的Hibernate Session
	 */
	protected void disconnectOnCompletion(Session session) {
		session.disconnect();
	}

	/**
	 * 返回给定的Hibernate会话是否始终保持相同的JDBC连接.
	 * 这用于检查事务管理器是否可以安全地准备和清理用于事务的JDBC连接.
	 * <p>默认实现检查会话的连接释放模式是否为 "on_close".
	 * 
	 * @param session 要检查的Hibernate Session
	 */
	@SuppressWarnings("deprecation")
	protected boolean isSameConnectionForEntireSession(Session session) {
		if (!(session instanceof SessionImplementor)) {
			// The best we can do is to assume we're safe.
			return true;
		}
		ConnectionReleaseMode releaseMode =
				((SessionImplementor) session).getJdbcCoordinator().getConnectionReleaseMode();
		return ConnectionReleaseMode.ON_CLOSE.equals(releaseMode);
	}

	/**
	 * 确定给定的Session是否 (仍然) 物理连接到数据库, 即在内部保持活动的JDBC连接.
	 * 
	 * @param session 要检查的Hibernate Session
	 */
	protected boolean isPhysicallyConnected(Session session) {
		if (!(session instanceof SessionImplementor)) {
			// The best we can do is to check whether we're logically connected.
			return session.isConnected();
		}
		return ((SessionImplementor) session).getJdbcCoordinator().getLogicalConnection().isPhysicallyConnected();
	}


	/**
	 * 将给定的HibernateException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * <p>会自动将指定的SQLExceptionTranslator应用于Hibernate JDBCException, 否则依赖于Hibernate的默认转换.
	 * 
	 * @param ex 发生的HibernateException
	 * 
	 * @return 相应的DataAccessException
	 */
	protected DataAccessException convertHibernateAccessException(HibernateException ex) {
		return SessionFactoryUtils.convertHibernateAccessException(ex);
	}


	/**
	 * Hibernate事务对象, 代表SessionHolder.
	 * 由HibernateTransactionManager用作事务对象.
	 */
	private class HibernateTransactionObject extends JdbcTransactionObjectSupport {

		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private boolean newSession;

		private Integer previousHoldability;

		public void setSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = true;
		}

		public void setExistingSession(Session session) {
			this.sessionHolder = new SessionHolder(session);
			this.newSessionHolder = true;
			this.newSession = false;
		}

		public void setSessionHolder(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = false;
			this.newSession = false;
		}

		public SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		public boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		public boolean isNewSession() {
			return this.newSession;
		}

		public void setPreviousHoldability(Integer previousHoldability) {
			this.previousHoldability = previousHoldability;
		}

		public Integer getPreviousHoldability() {
			return this.previousHoldability;
		}

		public boolean hasSpringManagedTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.getTransaction() != null);
		}

		public boolean hasHibernateManagedTransaction() {
			return (this.sessionHolder != null &&
					this.sessionHolder.getSession().getTransaction().getStatus() == TransactionStatus.ACTIVE);
		}

		public void setRollbackOnly() {
			this.sessionHolder.setRollbackOnly();
			if (hasConnectionHolder()) {
				getConnectionHolder().setRollbackOnly();
			}
		}

		@Override
		public boolean isRollbackOnly() {
			return this.sessionHolder.isRollbackOnly() ||
					(hasConnectionHolder() && getConnectionHolder().isRollbackOnly());
		}

		@Override
		public void flush() {
			try {
				this.sessionHolder.getSession().flush();
			}
			catch (HibernateException ex) {
				throw convertHibernateAccessException(ex);
			}
			catch (PersistenceException ex) {
				if (ex.getCause() instanceof HibernateException) {
					throw convertHibernateAccessException((HibernateException) ex.getCause());
				}
				throw ex;
			}
		}
	}


	/**
	 * 暂停资源的保存器.
	 * 由{@code doSuspend}和{@code doResume}在内部使用.
	 */
	private static class SuspendedResourcesHolder {

		private final SessionHolder sessionHolder;

		private final ConnectionHolder connectionHolder;

		private SuspendedResourcesHolder(SessionHolder sessionHolder, ConnectionHolder conHolder) {
			this.sessionHolder = sessionHolder;
			this.connectionHolder = conHolder;
		}

		private SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		private ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}
	}
}
