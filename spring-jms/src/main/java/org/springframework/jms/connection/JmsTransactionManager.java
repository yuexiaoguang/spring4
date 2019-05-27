package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionRolledBackException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}实现,
 * 用于单个JMS {@link javax.jms.ConnectionFactory}.
 * 将JMS连接/会话对从指定的ConnectionFactory绑定到线程, 可能允许每个ConnectionFactory一个线程绑定的Session.
 *
 * <p>此本地策略是在JTA事务中执行JMS操作的替代方法.
 * 它的优点是它能够在任何环境中工作, 例如独立应用程序或测试套件, 任何消息代理都可以作为目标.
 * 但是, 此策略<i>不</i>能够提供XA事务, 例如为了在消息传递和数据库访问之间共享事务.
 * XA事务需要完整的JTA/XA设置, 通常使用Spring的{@link org.springframework.transaction.jta.JtaTransactionManager}作为策略.
 *
 * <p>需要应用程序代码通过{@link ConnectionFactoryUtils#getTransactionalSession}检索事务性JMS会话,
 * 而不是标准的Java EE样式{@link ConnectionFactory#createConnection()}调用以及后续的会话创建.
 * Spring的{@link org.springframework.jms.core.JmsTemplate}将自动检测线程绑定的Session并自动参与其中.
 *
 * <p>或者, 可以允许应用程序代码使用ConnectionFactory上的标准Java EE样式查找模式, 例如, 对于根本不了解Spring的遗留代码.
 * 在这种情况下, 为目标ConnectionFactory定义一个{@link TransactionAwareConnectionFactoryProxy},
 * 它将自动参与Spring管理的事务.
 *
 * <p><b>强烈建议使用{@link CachingConnectionFactory}作为此事务管理器的目标.</b>
 * CachingConnectionFactory对所有JMS访问使用单个JMS连接, 以避免重复创建连接以及维护Sessions的缓存的开销.
 * 然后, 每个事务将共享相同的JMS连接, 同时仍使用其自己的单独JMS会话.
 *
 * <p>由于缺乏资源重用，使用<i>原始</i>目标ConnectionFactory不仅仅是效率低下.
 * 当JMS驱动程序在{@code Session.commit()}之前,
 * 不接受{@code MessageProducer.close()}调用和/或 {@code MessageConsumer.close()}调用时,
 * 它也可能导致奇怪的效果, 后者应该提交通过生产者句柄发送并通过消费者句柄接收的所有消息.
 * 作为一种安全的通用解决方案, 始终将{@link CachingConnectionFactory}传递到此事务管理器的
 * {@link #setConnectionFactory "connectionFactory"}属性中.
 *
 * <p>默认情况下, 事务同步处于关闭状态, 因为此管理器可能与基于数据存储的Spring事务管理器一起使用,
 * 例如JDBC {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}, 它具有更强的同步需求.
 */
@SuppressWarnings("serial")
public class JmsTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, InitializingBean {

	private ConnectionFactory connectionFactory;


	/**
	 * <p>Note: 必须在使用实例之前设置ConnectionFactory.
	 * 此构造函数可用于通过BeanFactory准备JmsTemplate, 通常通过setConnectionFactory设置ConnectionFactory.
	 * <p>默认情况下关闭事务同步, 因为此管理器可能与基于数据存储的Spring事务管理器, 例如DataSourceTransactionManager, 后者具有更强的同步需求.
	 * 在任何时间点, 只允许一个管理器推动同步.
	 */
	public JmsTransactionManager() {
		setTransactionSynchronization(SYNCHRONIZATION_NEVER);
	}

	/**
	 * @param connectionFactory 从中获取连接的ConnectionFactory
	 */
	public JmsTransactionManager(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置此实例应管理其事务的JMS ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory cf) {
		if (cf instanceof TransactionAwareConnectionFactoryProxy) {
			// 如果得到TransactionAwareConnectionFactoryProxy, 需要为其底层目标ConnectionFactory执行事务,
			// 否则JMS访问代码将看不到正确公开的事务 (i.e. 目标ConnectionFactory的事务).
			this.connectionFactory = ((TransactionAwareConnectionFactoryProxy) cf).getTargetConnectionFactory();
		}
		else {
			this.connectionFactory = cf;
		}
	}

	/**
	 * 返回此实例应管理其事务的JMS ConnectionFactory.
	 */
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 确保已设置ConnectionFactory.
	 */
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
		JmsTransactionObject txObject = new JmsTransactionObject();
		txObject.setResourceHolder(
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(getConnectionFactory()));
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		return txObject.hasResourceHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException("JMS does not support an isolation level concept");
		}

		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		Connection con = null;
		Session session = null;
		try {
			con = createConnection();
			session = createSession(con);
			if (logger.isDebugEnabled()) {
				logger.debug("Created JMS transaction on Session [" + session + "] from Connection [" + con + "]");
			}
			txObject.setResourceHolder(new JmsResourceHolder(getConnectionFactory(), con, session));
			txObject.getResourceHolder().setSynchronizedWithTransaction(true);
			int timeout = determineTimeout(definition);
			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
				txObject.getResourceHolder().setTimeoutInSeconds(timeout);
			}
			TransactionSynchronizationManager.bindResource(getConnectionFactory(), txObject.getResourceHolder());
		}
		catch (Throwable ex) {
			if (session != null) {
				try {
					session.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw new CannotCreateTransactionException("Could not create JMS transaction", ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		txObject.setResourceHolder(null);
		return TransactionSynchronizationManager.unbindResource(getConnectionFactory());
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		TransactionSynchronizationManager.bindResource(getConnectionFactory(), suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getSession();
		try {
			if (status.isDebug()) {
				logger.debug("Committing JMS transaction on Session [" + session + "]");
			}
			session.commit();
		}
		catch (TransactionRolledBackException ex) {
			throw new UnexpectedRollbackException("JMS transaction rolled back", ex);
		}
		catch (JMSException ex) {
			throw new TransactionSystemException("Could not commit JMS transaction", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		Session session = txObject.getResourceHolder().getSession();
		try {
			if (status.isDebug()) {
				logger.debug("Rolling back JMS transaction on Session [" + session + "]");
			}
			session.rollback();
		}
		catch (JMSException ex) {
			throw new TransactionSystemException("Could not roll back JMS transaction", ex);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		JmsTransactionObject txObject = (JmsTransactionObject) status.getTransaction();
		txObject.getResourceHolder().setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		JmsTransactionObject txObject = (JmsTransactionObject) transaction;
		TransactionSynchronizationManager.unbindResource(getConnectionFactory());
		txObject.getResourceHolder().closeAll();
		txObject.getResourceHolder().clear();
	}


	/**
	 * 通过此模板的ConnectionFactory创建JMS连接.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @return 新的JMS Connection
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	/**
	 * 为给定的Connection创建JMS会话.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param con 要创建会话的JMS连接
	 * 
	 * @return 新的JMS Session
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(true, Session.AUTO_ACKNOWLEDGE);
	}


	/**
	 * JMS事务对象, 表示JmsResourceHolder.
	 * 由JmsTransactionManager用作事务对象.
	 */
	private static class JmsTransactionObject implements SmartTransactionObject {

		private JmsResourceHolder resourceHolder;

		public void setResourceHolder(JmsResourceHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		public JmsResourceHolder getResourceHolder() {
			return this.resourceHolder;
		}

		public boolean hasResourceHolder() {
			return (this.resourceHolder != null);
		}

		@Override
		public boolean isRollbackOnly() {
			return this.resourceHolder.isRollbackOnly();
		}

		@Override
		public void flush() {
			// no-op
		}
	}
}
