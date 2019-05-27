package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 用于管理JMS {@link javax.jms.ConnectionFactory}的Helper类, 特别是用于获取给定ConnectionFactory的事务JMS资源.
 *
 * <p>主要供框架内部使用.
 * 由{@link org.springframework.jms.core.JmsTemplate}
 * 和{@link org.springframework.jms.listener.DefaultMessageListenerContainer}使用.
 */
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * 释放给定的Connection, 停止它并最终关闭它.
	 * <p>检查{@link SmartConnectionFactory#shouldStop}.
	 * 这本质上是{@link org.springframework.jms.support.JmsUtils#closeConnection}的更复杂版本.
	 * 
	 * @param con 要释放的Connection (如果这是{@code null}, 则将忽略该调用)
	 * @param cf 从中获取Connection的ConnectionFactory (may be {@code null})
	 * @param started 是否可能已由应用程序启动Connection
	 */
	public static void releaseConnection(Connection con, ConnectionFactory cf, boolean started) {
		if (con == null) {
			return;
		}
		if (started && cf instanceof SmartConnectionFactory && ((SmartConnectionFactory) cf).shouldStop(con)) {
			try {
				con.stop();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection before closing it", ex);
			}
		}
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.debug("Could not close JMS Connection", ex);
		}
	}

	/**
	 * 返回给定Session的最里面的目标Session.
	 * 如果给定的Session是代理, 它将被解包, 直到找到非代理会话. 否则, 传入的Session将按原样返回.
	 * 
	 * @param session 要解包的Session代理
	 * 
	 * @return 最里面的目标Session, 或传入的值
	 */
	public static Session getTargetSession(Session session) {
		Session sessionToUse = session;
		while (sessionToUse instanceof SessionProxy) {
			sessionToUse = ((SessionProxy) sessionToUse).getTargetSession();
		}
		return sessionToUse;
	}



	/**
	 * 确定给定的JMS Session是否是事务性的, 即由Spring的事务工具绑定到当前线程.
	 * 
	 * @param session 要检查的JMS Session
	 * @param cf Session源自的JMS ConnectionFactory
	 * 
	 * @return Session是否是事务性的
	 */
	public static boolean isSessionTransactional(Session session, ConnectionFactory cf) {
		if (session == null || cf == null) {
			return false;
		}
		JmsResourceHolder resourceHolder = (JmsResourceHolder) TransactionSynchronizationManager.getResource(cf);
		return (resourceHolder != null && resourceHolder.containsSession(session));
	}


	/**
	 * 获取与当前事务同步的JMS会话.
	 * 
	 * @param cf 要获取Session的ConnectionFactory
	 * @param existingCon 要获取Session的现有的JMS Connection (may be {@code null})
	 * @param synchedLocalTransactionAllowed 是否允许与Spring管理的事务同步的本地JMS事务
	 * (例如, 主事务可能是特定DataSource的基于JDBC的事务), JMS事务在主事务之后立即提交.
	 * 如果不允许, 给定的ConnectionFactory需要处理事务登记.
	 * 
	 * @return 事务Session, 或{@code null}
	 * @throws JMSException JMS错误
	 */
	public static Session getTransactionalSession(final ConnectionFactory cf,
			final Connection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(Session.class, existingCon);
			}
			@Override
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection());
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return con.createSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务同步的JMS QueueSession.
	 * <p>主要用于 JMS 1.0.2 API.
	 * 
	 * @param cf 要获取Session的ConnectionFactory
	 * @param existingCon 要获取Session的现有JMS Connection (may be {@code null})
	 * @param synchedLocalTransactionAllowed 是否允许与Spring管理的事务同步的本地JMS事务
	 * (例如, 主事务可能是特定DataSource的基于JDBC的事务), JMS事务在主事务之后立即提交.
	 * 如果不允许, 给定的ConnectionFactory需要处理事务登记.
	 * 
	 * @return 事务Session, 或{@code null}
	 * @throws JMSException JMS失败
	 */
	public static QueueSession getTransactionalQueueSession(final QueueConnectionFactory cf,
			final QueueConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (QueueSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(QueueSession.class, existingCon);
			}
			@Override
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(QueueConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createQueueConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((QueueConnection) con).createQueueSession(synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务同步的JMS TopicSession.
	 * <p>主要用于 JMS 1.0.2 API.
	 * 
	 * @param cf 要获取Session的ConnectionFactory
	 * @param existingCon 要获取Session的现有JMS Connection (may be {@code null})
	 * @param synchedLocalTransactionAllowed 是否允许与Spring管理的事务同步的本地JMS事务
	 * (例如, 主事务可能是特定DataSource的基于JDBC的事务), JMS事务在主事务之后立即提交.
	 * 如果不允许, 给定的ConnectionFactory需要处理事务登记.
	 * 
	 * @return 事务Session, 或{@code null}
	 * @throws JMSException JMS失败
	 */
	public static TopicSession getTransactionalTopicSession(final TopicConnectionFactory cf,
			final TopicConnection existingCon, final boolean synchedLocalTransactionAllowed)
			throws JMSException {

		return (TopicSession) doGetTransactionalSession(cf, new ResourceFactory() {
			@Override
			public Session getSession(JmsResourceHolder holder) {
				return holder.getSession(TopicSession.class, existingCon);
			}
			@Override
			public Connection getConnection(JmsResourceHolder holder) {
				return (existingCon != null ? existingCon : holder.getConnection(TopicConnection.class));
			}
			@Override
			public Connection createConnection() throws JMSException {
				return cf.createTopicConnection();
			}
			@Override
			public Session createSession(Connection con) throws JMSException {
				return ((TopicConnection) con).createTopicSession(
						synchedLocalTransactionAllowed, Session.AUTO_ACKNOWLEDGE);
			}
			@Override
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		}, true);
	}

	/**
	 * 获取与当前事务同步的JMS会话.
	 * <p>假设会话将用于接收消息, 此{@code doGetTransactionalSession}变体始终启动底层JMS连接.
	 * 
	 * @param connectionFactory 要绑定的JMS ConnectionFactory (用作TransactionSynchronizationManager键)
	 * @param resourceFactory 用于提取或创建JMS资源的ResourceFactory
	 * 
	 * @return 事务Session, 或{@code null}
	 * @throws JMSException JMS失败
	 */
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory) throws JMSException {

		return doGetTransactionalSession(connectionFactory, resourceFactory, true);
	}

	/**
	 * 获取与当前事务同步的JMS会话.
	 * 
	 * @param connectionFactory 要绑定的JMS ConnectionFactory (用作TransactionSynchronizationManager键)
	 * @param resourceFactory 用于提取或创建JMS资源的ResourceFactory
	 * @param startConnection 是否应启动底层JMS连接方法以允许接收消息.
	 * 请注意, 之前可能已经启动了重用的连接, 即使此标志为{@code false}.
	 * 
	 * @return 事务Session, 或{@code null}
	 * @throws JMSException JMS失败
	 */
	public static Session doGetTransactionalSession(
			ConnectionFactory connectionFactory, ResourceFactory resourceFactory, boolean startConnection)
			throws JMSException {

		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
		if (resourceHolder != null) {
			Session session = resourceFactory.getSession(resourceHolder);
			if (session != null) {
				if (startConnection) {
					Connection con = resourceFactory.getConnection(resourceHolder);
					if (con != null) {
						con.start();
					}
				}
				return session;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JmsResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JmsResourceHolder(connectionFactory);
		}
		Connection con = resourceFactory.getConnection(resourceHolderToUse);
		Session session = null;
		try {
			boolean isExistingCon = (con != null);
			if (!isExistingCon) {
				con = resourceFactory.createConnection();
				resourceHolderToUse.addConnection(con);
			}
			session = resourceFactory.createSession(con);
			resourceHolderToUse.addSession(session, con);
			if (startConnection) {
				con.start();
			}
		}
		catch (JMSException ex) {
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
			throw ex;
		}
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JmsResourceSynchronization(resourceHolderToUse, connectionFactory,
							resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolderToUse);
		}
		return session;
	}


	/**
	 * 用于资源创建的回调接口.
	 * 作为{@code doGetTransactionalSession}方法的参数.
	 */
	public interface ResourceFactory {

		/**
		 * 从给定的JmsResourceHolder中获取适当的Session.
		 * 
		 * @param holder the JmsResourceHolder
		 * 
		 * @return 从持有者获取的适当会话, 或{@code null}
		 */
		Session getSession(JmsResourceHolder holder);

		/**
		 * 从给定的JmsResourceHolder获取适当的Connection.
		 * 
		 * @param holder the JmsResourceHolder
		 * 
		 * @return 从持有者获取的适当连接, 或{@code null}
		 */
		Connection getConnection(JmsResourceHolder holder);

		/**
		 * 创建新的JMS连接以向JmsResourceHolder注册.
		 * 
		 * @return 新的JMS Connection
		 * @throws JMSException 如果由JMS API方法抛出
		 */
		Connection createConnection() throws JMSException;

		/**
		 * 创建新的JMS Session以向JmsResourceHolder注册.
		 * 
		 * @param con 要为其创建会话的JMS连接
		 * 
		 * @return 新的JMS Session
		 * @throws JMSException 如果由JMS API方法抛出
		 */
		Session createSession(Connection con) throws JMSException;

		/**
		 * 返回是否允许与Spring管理的事务同步的本地JMS事务
		 * (例如, 主事务可能是特定DataSource的基于JDBC的事务), JMS事务在主事务之后立即提交.
		 * 
		 * @return 是否允许同步本地JMS事务
		 */
		boolean isSynchedLocalTransactionAllowed();
	}


	/**
	 * 在非本机JMS事务结束时资源清理的回调 (e.g. 在参与JtaTransactionManager事务时).
	 */
	private static class JmsResourceSynchronization extends ResourceHolderSynchronization<JmsResourceHolder, Object> {

		private final boolean transacted;

		public JmsResourceSynchronization(JmsResourceHolder resourceHolder, Object resourceKey, boolean transacted) {
			super(resourceHolder, resourceKey);
			this.transacted = transacted;
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return !this.transacted;
		}

		@Override
		protected void processResourceAfterCommit(JmsResourceHolder resourceHolder) {
			try {
				resourceHolder.commitAll();
			}
			catch (JMSException ex) {
				throw new SynchedLocalTransactionFailedException("Local JMS transaction failed to commit", ex);
			}
		}

		@Override
		protected void releaseResource(JmsResourceHolder resourceHolder, Object resourceKey) {
			resourceHolder.closeAll();
		}
	}
}
