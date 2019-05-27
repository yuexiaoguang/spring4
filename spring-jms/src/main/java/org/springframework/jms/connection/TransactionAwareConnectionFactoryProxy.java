package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
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
import javax.jms.TransactionInProgressException;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 目标JMS {@link javax.jms.ConnectionFactory}的代理, 增加对Spring管理的事务的感知.
 * 类似于Java EE应用程序服务器提供的事务性JNDI ConnectionFactory.
 *
 * <p>应该仍然不知道Spring的JMS支持的消息代码可以使用此代理无缝地参与Spring管理的事务.
 * 请注意, 事务管理器, 例如{@link JmsTransactionManager}, 仍然需要使用底层ConnectionFactory, <i>而不是</i>使用此代理.
 *
 * <p><b>确保TransactionAwareConnectionFactoryProxy是ConnectionFactory代理/适配器链的最外层ConnectionFactory.</b>
 * TransactionAwareConnectionFactoryProxy 可以直接委托给目标工厂或某些中间适配器,
 * 如{@link UserCredentialsConnectionFactoryAdapter}.
 *
 * <p>委托给{@link ConnectionFactoryUtils}自动参与线程绑定事务, 例如由{@link JmsTransactionManager}管理.
 * 对返回的Sessions的{@code createSession}调用和{@code close}调用将在事务中正常运行, 即始终在事务性Session上工作.
 * 如果不在事务中, 则应用正常的ConnectionFactory行为.
 *
 * <p>请注意, 事务性JMS会话将基于每个连接进行注册.
 * 要跨事务共享相同的JMS会话, 确保使用相同的JMS Connection句柄 - 通过重用句柄或通过在下面配置{@link SingleConnectionFactory}.
 *
 * <p>返回的事务性Session代理将实现{@link SessionProxy}接口以允许访问底层目标会话.
 * 这仅用于访问特定于供应商的Session API或用于测试目的 (e.g. 执行手动事务控制).
 * 出于典型应用目的, 只需使用标准JMS会话接口即可.
 */
public class TransactionAwareConnectionFactoryProxy
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {

	private ConnectionFactory targetConnectionFactory;

	private boolean synchedLocalTransactionAllowed = false;


	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * @param targetConnectionFactory 目标ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
	}


	/**
	 * 设置此ConnectionFactory应委托给的目标ConnectionFactory.
	 */
	public final void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此ConnectionFactory应委托给的目标ConnectionFactory.
	 */
	protected ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * 设置是否允许与Spring管理的事务同步的本地JMS事务 (例如, 主事务可能是特定DataSource的基于JDBC的事务),
	 * JMS事务在主事务之后立即提交.
	 * 如果不允许, 给定的ConnectionFactory需要处理事务登记.
	 * <p>默认"false": 如果不在包含底层JMS ConnectionFactory的托管事务中, 则将返回标准Session.
	 * 打开此标志以允许参与任何Spring管理的事务, 并使本地JMS事务与主事务同步.
	 */
	public void setSynchedLocalTransactionAllowed(boolean synchedLocalTransactionAllowed) {
		this.synchedLocalTransactionAllowed = synchedLocalTransactionAllowed;
	}

	/**
	 * 返回是否允许与Spring管理的事务同步的本地JMS事务.
	 */
	protected boolean isSynchedLocalTransactionAllowed() {
		return this.synchedLocalTransactionAllowed;
	}


	@Override
	public Connection createConnection() throws JMSException {
		Connection targetConnection = this.targetConnectionFactory.createConnection();
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		Connection targetConnection = this.targetConnectionFactory.createConnection(username, password);
		return getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		if (!(this.targetConnectionFactory instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection =
				((QueueConnectionFactory) this.targetConnectionFactory).createQueueConnection();
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		if (!(this.targetConnectionFactory instanceof QueueConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no QueueConnectionFactory");
		}
		QueueConnection targetConnection =
				((QueueConnectionFactory) this.targetConnectionFactory).createQueueConnection(username, password);
		return (QueueConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		if (!(this.targetConnectionFactory instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection =
				((TopicConnectionFactory) this.targetConnectionFactory).createTopicConnection();
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		if (!(this.targetConnectionFactory instanceof TopicConnectionFactory)) {
			throw new javax.jms.IllegalStateException("'targetConnectionFactory' is no TopicConnectionFactory");
		}
		TopicConnection targetConnection =
				((TopicConnectionFactory) this.targetConnectionFactory).createTopicConnection(username, password);
		return (TopicConnection) getTransactionAwareConnectionProxy(targetConnection);
	}


	/**
	 * 使用代理来包装给定的Connection, 该代理将每个方法调用委托给它, 但以事务感知方式处理Session查找.
	 * 
	 * @param target 要包装的原始Connection
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<Class<?>>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new TransactionAwareConnectionInvocationHandler(target));
	}


	/**
	 * 为底层Connection公开事务性会话的调用处理器.
	 */
	private class TransactionAwareConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		public TransactionAwareConnectionInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自ConnectionProxy接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (Session.class == method.getReturnType()) {
				Session session = ConnectionFactoryUtils.getTransactionalSession(
						getTargetConnectionFactory(), this.target, isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (QueueSession.class == method.getReturnType()) {
				QueueSession session = ConnectionFactoryUtils.getTransactionalQueueSession(
						(QueueConnectionFactory) getTargetConnectionFactory(), (QueueConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}
			else if (TopicSession.class == method.getReturnType()) {
				TopicSession session = ConnectionFactoryUtils.getTransactionalTopicSession(
						(TopicConnectionFactory) getTargetConnectionFactory(), (TopicConnection) this.target,
						isSynchedLocalTransactionAllowed());
				if (session != null) {
					return getCloseSuppressingSessionProxy(session);
				}
			}

			// 在目标Connection上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private Session getCloseSuppressingSessionProxy(Session target) {
			List<Class<?>> classes = new ArrayList<Class<?>>(3);
			classes.add(SessionProxy.class);
			if (target instanceof QueueSession) {
				classes.add(QueueSession.class);
			}
			if (target instanceof TopicSession) {
				classes.add(TopicSession.class);
			}
			return (Session) Proxy.newProxyInstance(SessionProxy.class.getClassLoader(),
					ClassUtils.toClassArray(classes), new CloseSuppressingSessionInvocationHandler(target));
		}
	}


	/**
	 * 禁止事务性JMS会话的关闭调用的调用处理器.
	 */
	private static class CloseSuppressingSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingSessionInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 在SessionProxy接口上调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("commit")) {
				throw new TransactionInProgressException("Commit call not allowed within a managed transaction");
			}
			else if (method.getName().equals("rollback")) {
				throw new TransactionInProgressException("Rollback call not allowed within a managed transaction");
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 不在事务中关闭.
				return null;
			}
			else if (method.getName().equals("getTargetSession")) {
				// 处理getTargetSession 方法: 返回底层Session.
				return this.target;
			}

			// 在目标Session上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
