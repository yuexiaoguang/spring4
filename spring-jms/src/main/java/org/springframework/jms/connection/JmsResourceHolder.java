package org.springframework.jms.connection;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TransactionInProgressException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JMS资源持有者, 包装JMS连接和JMS会话.
 * 对于给定的JMS ConnectionFactory, JmsTransactionManager将此类的实例绑定到线程.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class JmsResourceHolder extends ResourceHolderSupport {

	private static final Log logger = LogFactory.getLog(JmsResourceHolder.class);

	private ConnectionFactory connectionFactory;

	private boolean frozen = false;

	private final List<Connection> connections = new LinkedList<Connection>();

	private final List<Session> sessions = new LinkedList<Session>();

	private final Map<Connection, List<Session>> sessionsPerConnection =
			new HashMap<Connection, List<Session>>();


	/**
	 * 创建一个新的JmsResourceHolder, 它可以为要添加的资源打开.
	 */
	public JmsResourceHolder() {
	}

	/**
	 * 创建一个新的JmsResourceHolder, 它可以为要添加的资源打开.
	 * 
	 * @param connectionFactory 与此资源持有者关联的JMS ConnectionFactory (may be {@code null})
	 */
	public JmsResourceHolder(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 为给定的JMS会话创建一个新的JmsResourceHolder.
	 * 
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(Session session) {
		addSession(session);
		this.frozen = true;
	}

	/**
	 * 为给定的JMS资源创建一个新的JmsResourceHolder.
	 * 
	 * @param connection the JMS Connection
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(Connection connection, Session session) {
		addConnection(connection);
		addSession(session, connection);
		this.frozen = true;
	}

	/**
	 * 为给定的JMS资源创建一个新的JmsResourceHolder.
	 * 
	 * @param connectionFactory 与此资源持有者关联的JMS ConnectionFactory (may be {@code null})
	 * @param connection the JMS Connection
	 * @param session the JMS Session
	 */
	public JmsResourceHolder(ConnectionFactory connectionFactory, Connection connection, Session session) {
		this.connectionFactory = connectionFactory;
		addConnection(connection);
		addSession(session, connection);
		this.frozen = true;
	}


	public final boolean isFrozen() {
		return this.frozen;
	}

	public final void addConnection(Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Connection because JmsResourceHolder is frozen");
		Assert.notNull(connection, "Connection must not be null");
		if (!this.connections.contains(connection)) {
			this.connections.add(connection);
		}
	}

	public final void addSession(Session session) {
		addSession(session, null);
	}

	public final void addSession(Session session, Connection connection) {
		Assert.isTrue(!this.frozen, "Cannot add Session because JmsResourceHolder is frozen");
		Assert.notNull(session, "Session must not be null");
		if (!this.sessions.contains(session)) {
			this.sessions.add(session);
			if (connection != null) {
				List<Session> sessions = this.sessionsPerConnection.get(connection);
				if (sessions == null) {
					sessions = new LinkedList<Session>();
					this.sessionsPerConnection.put(connection, sessions);
				}
				sessions.add(session);
			}
		}
	}

	public boolean containsSession(Session session) {
		return this.sessions.contains(session);
	}


	public Connection getConnection() {
		return (!this.connections.isEmpty() ? this.connections.get(0) : null);
	}

	public Connection getConnection(Class<? extends Connection> connectionType) {
		return CollectionUtils.findValueOfType(this.connections, connectionType);
	}

	public Session getSession() {
		return (!this.sessions.isEmpty() ? this.sessions.get(0) : null);
	}

	public Session getSession(Class<? extends Session> sessionType) {
		return getSession(sessionType, null);
	}

	public Session getSession(Class<? extends Session> sessionType, Connection connection) {
		List<Session> sessions = (connection != null ? this.sessionsPerConnection.get(connection) : this.sessions);
		return CollectionUtils.findValueOfType(sessions, sessionType);
	}


	public void commitAll() throws JMSException {
		for (Session session : this.sessions) {
			try {
				session.commit();
			}
			catch (TransactionInProgressException ex) {
				// Ignore -> 只有在JTA事务的情况下才会发生.
			}
			catch (javax.jms.IllegalStateException ex) {
				if (this.connectionFactory != null) {
					try {
						Method getDataSourceMethod = this.connectionFactory.getClass().getMethod("getDataSource");
						Object ds = ReflectionUtils.invokeMethod(getDataSourceMethod, this.connectionFactory);
						while (ds != null) {
							if (TransactionSynchronizationManager.hasResource(ds)) {
								// 来自共享底层JDBC Connection的IllegalStateException, 它通常首先被提交, e.g. with Oracle AQ --> ignore
								return;
							}
							try {
								// 检查装饰的DataSource和Spring的DelegatingDataSource
								Method getTargetDataSourceMethod = ds.getClass().getMethod("getTargetDataSource");
								ds = ReflectionUtils.invokeMethod(getTargetDataSourceMethod, ds);
							}
							catch (NoSuchMethodException nsme) {
								ds = null;
							}
						}
					}
					catch (Throwable ex2) {
						if (logger.isDebugEnabled()) {
							logger.debug("No working getDataSource method found on ConnectionFactory: " + ex2);
						}
						// 未运行的getDataSource方法 - 无法执行DataSource事务检查
					}
				}
				throw ex;
			}
		}
	}

	public void closeAll() {
		for (Session session : this.sessions) {
			try {
				session.close();
			}
			catch (Throwable ex) {
				logger.debug("Could not close synchronized JMS Session after transaction", ex);
			}
		}
		for (Connection con : this.connections) {
			ConnectionFactoryUtils.releaseConnection(con, this.connectionFactory, true);
		}
		this.connections.clear();
		this.sessions.clear();
		this.sessionsPerConnection.clear();
	}

}
