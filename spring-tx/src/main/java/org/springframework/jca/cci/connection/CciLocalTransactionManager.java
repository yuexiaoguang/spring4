package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.LocalTransactionException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 管理单个CCI ConnectionFactory的本地事务的
 * {@link org.springframework.transaction.PlatformTransactionManager}实现.
 * 将CCI连接从指定的ConnectionFactory绑定到线程, 可能允许每个ConnectionFactory一个线程绑定连接.
 *
 * <p>需要应用程序代码来通过{@link ConnectionFactoryUtils#getConnection(ConnectionFactory)}
 * 而不是标准的Java EE风格的{@link ConnectionFactory#getConnection()}调用来检索CCI连接.
 * 像{@link org.springframework.jca.cci.core.CciTemplate}这样的Spring类隐式使用这个策略.
 * 如果不与此事务管理器结合使用, {@link ConnectionFactoryUtils}查找策略的行为与本机DataSource查找完全相同; 因此它可以以便携方式使用.
 *
 * <p>或者, 可以允许应用程序代码使用标准Java EE查找模式{@link ConnectionFactory#getConnection()},
 * 例如, 对于根本不了解Spring的遗留代码.
 * 在这种情况下, 为目标ConnectionFactory定义一个{@link TransactionAwareConnectionFactoryProxy},
 * 它将自动参与Spring管理的事务.
 */
@SuppressWarnings("serial")
public class CciLocalTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	private ConnectionFactory connectionFactory;


	/**
	 * 必须将ConnectionFactory设置为能够使用它.
	 */
	public CciLocalTransactionManager() {
	}

	/**
	 * @param connectionFactory 管理本地事务的CCI ConnectionFactory
	 */
	public CciLocalTransactionManager(ConnectionFactory connectionFactory) {
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应管理本地事务的CCI ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory cf) {
		if (cf instanceof TransactionAwareConnectionFactoryProxy) {
			// 如果是TransactionAwareConnectionFactoryProxy, 需要为其底层目标ConnectionFactory执行事务,
			// 否则JMS访问代码将看不到正确公开的事务 (i.e. 目标ConnectionFactory的事务).
			this.connectionFactory = ((TransactionAwareConnectionFactoryProxy) cf).getTargetConnectionFactory();
		}
		else {
			this.connectionFactory = cf;
		}
	}

	/**
	 * 返回此实例管理本地事务的CCI ConnectionFactory.
	 */
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	@Override
	public Object getResourceFactory() {
		return getConnectionFactory();
	}

	@Override
	protected Object doGetTransaction() {
		CciLocalTransactionObject txObject = new CciLocalTransactionObject();
		ConnectionHolder conHolder =
				(ConnectionHolder) TransactionSynchronizationManager.getResource(getConnectionFactory());
		txObject.setConnectionHolder(conHolder);
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		// 将预绑定连接视为事务.
		return txObject.hasConnectionHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		Connection con = null;

		try {
			con = getConnectionFactory().getConnection();
			if (logger.isDebugEnabled()) {
				logger.debug("Acquired Connection [" + con + "] for local CCI transaction");
			}

			txObject.setConnectionHolder(new ConnectionHolder(con));
			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);

			con.getLocalTransaction().begin();
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
			}
			TransactionSynchronizationManager.bindResource(getConnectionFactory(), txObject.getConnectionHolder());
		}
		catch (NotSupportedException ex) {
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
			throw new CannotCreateTransactionException("CCI Connection does not support local transactions", ex);
		}
		catch (LocalTransactionException ex) {
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
			throw new CannotCreateTransactionException("Could not begin local CCI transaction", ex);
		}
		catch (Throwable ex) {
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
			throw new TransactionSystemException("Unexpected failure on begin of CCI local transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		txObject.setConnectionHolder(null);
		return TransactionSynchronizationManager.unbindResource(getConnectionFactory());
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		ConnectionHolder conHolder = (ConnectionHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(getConnectionFactory(), conHolder);
	}

	protected boolean isRollbackOnly(Object transaction) throws TransactionException {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;
		return txObject.getConnectionHolder().isRollbackOnly();
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Committing CCI local transaction on Connection [" + con + "]");
		}
		try {
			con.getLocalTransaction().commit();
		}
		catch (LocalTransactionException ex) {
			throw new TransactionSystemException("Could not commit CCI local transaction", ex);
		}
		catch (ResourceException ex) {
			throw new TransactionSystemException("Unexpected failure on commit of CCI local transaction", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		Connection con = txObject.getConnectionHolder().getConnection();
		if (status.isDebug()) {
			logger.debug("Rolling back CCI local transaction on Connection [" + con + "]");
		}
		try {
			con.getLocalTransaction().rollback();
		}
		catch (LocalTransactionException ex) {
			throw new TransactionSystemException("Could not roll back CCI local transaction", ex);
		}
		catch (ResourceException ex) {
			throw new TransactionSystemException("Unexpected failure on rollback of CCI local transaction", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting CCI local transaction [" + txObject.getConnectionHolder().getConnection() +
					"] rollback-only");
		}
		txObject.getConnectionHolder().setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		CciLocalTransactionObject txObject = (CciLocalTransactionObject) transaction;

		// 从线程删除连接保存器.
		TransactionSynchronizationManager.unbindResource(getConnectionFactory());
		txObject.getConnectionHolder().clear();

		Connection con = txObject.getConnectionHolder().getConnection();
		if (logger.isDebugEnabled()) {
			logger.debug("Releasing CCI Connection [" + con + "] after transaction");
		}
		ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
	}


	/**
	 * CCI本地事务对象, 表示ConnectionHolder.
	 * 由CciLocalTransactionManager用作事务对象.
	 */
	private static class CciLocalTransactionObject {

		private ConnectionHolder connectionHolder;

		public void setConnectionHolder(ConnectionHolder connectionHolder) {
			this.connectionHolder = connectionHolder;
		}

		public ConnectionHolder getConnectionHolder() {
			return this.connectionHolder;
		}

		public boolean hasConnectionHolder() {
			return (this.connectionHolder != null);
		}
	}
}
