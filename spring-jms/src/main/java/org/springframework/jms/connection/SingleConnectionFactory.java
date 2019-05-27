package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JMS ConnectionFactory适配器, 它从所有{@link #createConnection()}调用返回相同的Connection,
 * 并忽略对{@link javax.jms.Connection#close()}的调用.
 * 根据JMS Connection模型, 这是完全线程安全的 (与JDBC相反). 如果出现异常, 可以自动恢复共享连接.
 *
 * <p>可以直接传入特定的JMS连接, 也可以让工厂通过给定的目标ConnectionFactory延迟地创建一个Connection.
 * 该工厂通常使用JMS 1.1以及 JMS 1.0.2 API.
 *
 * <p>请注意, 使用JMS 1.0.2 API时, 此ConnectionFactory将根据运行时使用的JMS API方法切换到队列/主题模式:
 * {@code createQueueConnection}和{@code createTopicConnection}将分别导致queue/topic模式;
 * 通用{@code createConnection}调用将导致JMS 1.1连接, 该连接能够为两种模式提供服务.
 *
 * <p>对于测试和独立环境很有用, 以便在多个{@link org.springframework.jms.core.JmsTemplate}调用中保持使用相同的Connection,
 * 而不需要使用池中的ConnectionFactory.
 * 这可能跨越任意数量的事务, 甚至同时执行事务.
 *
 * <p>请注意, Spring的消息监听器容器支持在每个监听器容器实例中使用共享Connection.
 * 组合使用SingleConnectionFactory对于<i>跨多个监听器容器</i>共享单个JMS Connection非常有意义.
 */
public class SingleConnectionFactory implements ConnectionFactory, QueueConnectionFactory,
		TopicConnectionFactory, ExceptionListener, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory targetConnectionFactory;

	private String clientId;

	private ExceptionListener exceptionListener;

	private boolean reconnectOnException = false;

	/** 目标Connection */
	private Connection connection;

	/** 是否创建队列或主题连接 */
	private Boolean pubSubMode;

	/** 允许每个连接ExceptionListener的内部聚合器 */
	private AggregatedExceptionListener aggregatedExceptionListener;

	/** 是否已启动共享 Connection */
	private int startedCount = 0;

	/** 共享Connection的同步监视器 */
	private final Object connectionMonitor = new Object();


	public SingleConnectionFactory() {
	}

	/**
	 * @param targetConnection 单个Connection
	 */
	public SingleConnectionFactory(Connection targetConnection) {
		Assert.notNull(targetConnection, "Target Connection must not be null");
		this.connection = targetConnection;
	}

	/**
	 * 创建一个新的SingleConnectionFactory, 它总是返回一个将通过给定目标ConnectionFactory延迟创建的Connection.
	 * 
	 * @param targetConnectionFactory 目标ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * 设置目标ConnectionFactory, 用于延迟创建单个Connection.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回用于延迟地创建单个Connection的目标ConnectionFactory.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * 为此ConnectionFactory创建和公开的单个Connection指定JMS客户端ID.
	 * <p>请注意, 客户端ID在底层JMS提供器的所有活动连接中必须是唯一的.
	 * 此外, 只有在尚未分配原始ConnectionFactory的情况下才能分配客户端ID.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * 返回由此ConnectionFactory创建和公开的单个Connection的JMS客户端ID.
	 */
	protected String getClientId() {
		return this.clientId;
	}

	/**
	 * 指定应使用此工厂创建的单个Connection注册的JMS ExceptionListener实现.
	 */
	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * 返回应使用此工厂创建的单个Connection注册的JMS ExceptionListener实现.
	 */
	protected ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * 指定在基础Connection报告JMSException时是否应重置单个Connection (以便随后更新).
	 * <p>默认"false". 将其切换为"true"以根据JMS提供者的异常通知自动触发恢复.
	 * <p>在内部, 这将导致向底层连接注册一个特殊的JMS ExceptionListener (此SingleConnectionFactory本身).
	 * 如果需要, 这也可以与用户指定的ExceptionListener结合使用.
	 */
	public void setReconnectOnException(boolean reconnectOnException) {
		this.reconnectOnException = reconnectOnException;
	}

	/**
	 * 返回当底层Connection报告JMSException时是否应更新单个Connection.
	 */
	protected boolean isReconnectOnException() {
		return this.reconnectOnException;
	}

	/**
	 * 确保已设置Connection或ConnectionFactory.
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Target Connection or ConnectionFactory is required");
		}
	}


	@Override
	public Connection createConnection() throws JMSException {
		return getSharedConnectionProxy(getConnection());
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.FALSE;
			con = createConnection();
		}
		if (!(con instanceof QueueConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a QueueConnection but rather: " + con);
		}
		return ((QueueConnection) con);
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		Connection con;
		synchronized (this.connectionMonitor) {
			this.pubSubMode = Boolean.TRUE;
			con = createConnection();
		}
		if (!(con instanceof TopicConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a TopicConnection but rather: " + con);
		}
		return ((TopicConnection) con);
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}


	/**
	 * 获取初始化的共享连接.
	 * 
	 * @return the Connection (never {@code null})
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected Connection getConnection() throws JMSException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	/**
	 * 初始化底层共享Connection.
	 * <p>如果已存在底层连接, 则关闭并重新初始化Connection.
	 * 
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	public void initConnection() throws JMSException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			this.connection = doCreateConnection();
			prepareConnection(this.connection);
			if (this.startedCount > 0) {
				this.connection.start();
			}
			if (logger.isInfoEnabled()) {
				logger.info("Established shared JMS Connection: " + this.connection);
			}
		}
	}

	/**
	 * 更新底层单个Connection的异常监听器回调.
	 */
	@Override
	public void onException(JMSException ex) {
		logger.warn("Encountered a JMSException - resetting the underlying JMS Connection", ex);
		resetConnection();
	}

	/**
	 * 关闭底层共享连接.
	 * ConnectionFactory的提供者需要关心正确的关闭.
	 * <p>当这个bean实现DisposableBean时, bean工厂会在销毁其缓存的单例时自动调用它.
	 */
	@Override
	public void destroy() {
		resetConnection();
	}

	/**
	 * 重置底层共享Connection, 以便在下次访问时重新初始化.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.connection != null) {
				closeConnection(this.connection);
			}
			this.connection = null;
		}
	}

	/**
	 * 通过此模板的ConnectionFactory创建JMS连接.
	 * 
	 * @return 新的JMS Connection
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected Connection doCreateConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (Boolean.FALSE.equals(this.pubSubMode) && cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection();
		}
		else {
			return getTargetConnectionFactory().createConnection();
		}
	}

	/**
	 * 在给定连接暴露之前准备它.
	 * <p>默认实现应用ExceptionListener和客户端ID.
	 * 可以在子类中重写.
	 * 
	 * @param con 要准备的Connection
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void prepareConnection(Connection con) throws JMSException {
		if (getClientId() != null) {
			con.setClientID(getClientId());
		}
		if (this.aggregatedExceptionListener != null) {
			con.setExceptionListener(this.aggregatedExceptionListener);
		}
		else if (getExceptionListener() != null || isReconnectOnException()) {
			ExceptionListener listenerToUse = getExceptionListener();
			if (isReconnectOnException()) {
				this.aggregatedExceptionListener = new AggregatedExceptionListener();
				this.aggregatedExceptionListener.delegates.add(this);
				if (listenerToUse != null) {
					this.aggregatedExceptionListener.delegates.add(listenerToUse);
				}
				listenerToUse = this.aggregatedExceptionListener;
			}
			con.setExceptionListener(listenerToUse);
		}
	}

	/**
	 * 用于获取(可能缓存的) Session的模板方法.
	 * <p>默认实现总是返回{@code null}.
	 * 子类可以覆盖它以暴露特定的Session句柄, 可能委托给{@link #createSession}来创建原始Session对象, 然后将其从此处包装并返回.
	 * 
	 * @param con 要运行的JMS连接
	 * @param mode Session确认模式 ({@code Session.TRANSACTED}或其中一种常见模式)
	 * 
	 * @return 要使用的Session, 或{@code null}表示创建原始标准会话
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Session getSession(Connection con, Integer mode) throws JMSException {
		return null;
	}

	/**
	 * 为此ConnectionFactory创建默认Session, 必要时适配JMS 1.0.2样式队列/主题模式.
	 * 
	 * @param con 要运行的JMS连接
	 * @param mode Session确认模式 ({@code Session.TRANSACTED}或其中一种常见模式)
	 * 
	 * @return 新创建的Session
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Session createSession(Connection con, Integer mode) throws JMSException {
		// 确定JMS API参数...
		boolean transacted = (mode == Session.SESSION_TRANSACTED);
		int ackMode = (transacted ? Session.AUTO_ACKNOWLEDGE : mode);
		// 现在实际上调用适当的JMS工厂方法...
		if (Boolean.FALSE.equals(this.pubSubMode) && con instanceof QueueConnection) {
			return ((QueueConnection) con).createQueueSession(transacted, ackMode);
		}
		else if (Boolean.TRUE.equals(this.pubSubMode) && con instanceof TopicConnection) {
			return ((TopicConnection) con).createTopicSession(transacted, ackMode);
		}
		else {
			return con.createSession(transacted, ackMode);
		}
	}

	/**
	 * 关闭给定的Connection.
	 * 
	 * @param con 要关闭的Connection
	 */
	protected void closeConnection(Connection con) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing shared JMS Connection: " + con);
		}
		try {
			try {
				if (this.startedCount > 0) {
					con.stop();
				}
			}
			finally {
				con.close();
			}
		}
		catch (javax.jms.IllegalStateException ex) {
			logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
		}
		catch (Throwable ex) {
			logger.debug("Could not close shared JMS Connection", ex);
		}
	}

	/**
	 * 使用代理来包装给定的Connection, 该代理将每个方法调用委托给它, 但禁止关闭调用.
	 * 这对于允许应用程序代码处理特殊框架Connection， 就像来自JMS ConnectionFactory的普通Connection一样非常有用.
	 * 
	 * @param target 要包装的原始Connection
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getSharedConnectionProxy(Connection target) {
		List<Class<?>> classes = new ArrayList<Class<?>>(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new SharedConnectionInvocationHandler());
	}


	/**
	 * 缓存的JMS连接代理的调用处理器.
	 */
	private class SharedConnectionInvocationHandler implements InvocationHandler {

		private ExceptionListener localExceptionListener;

		private boolean locallyStarted = false;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				Object other = args[0];
				if (proxy == other) {
					return true;
				}
				if (other == null || !Proxy.isProxyClass(other.getClass())) {
					return false;
				}
				InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
				return (otherHandler instanceof SharedConnectionInvocationHandler &&
						factory() == ((SharedConnectionInvocationHandler) otherHandler).factory());
			}
			else if (method.getName().equals("hashCode")) {
				// 使用包含它的SingleConnectionFactory的hashCode.
				return System.identityHashCode(factory());
			}
			else if (method.getName().equals("toString")) {
				return "Shared JMS Connection: " + getConnection();
			}
			else if (method.getName().equals("setClientID")) {
				// 处理setClientID 方法: 如果不兼容则抛出异常.
				String currentClientId = getConnection().getClientID();
				if (currentClientId != null && currentClientId.equals(args[0])) {
					return null;
				}
				else {
					throw new javax.jms.IllegalStateException(
							"setClientID call not supported on proxy for shared Connection. " +
							"Set the 'clientId' property on the SingleConnectionFactory instead.");
				}
			}
			else if (method.getName().equals("setExceptionListener")) {
				// 处理setExceptionListener 方法: 添加到链中.
				synchronized (connectionMonitor) {
					if (aggregatedExceptionListener != null) {
						ExceptionListener listener = (ExceptionListener) args[0];
						if (listener != this.localExceptionListener) {
							if (this.localExceptionListener != null) {
								aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
							}
							if (listener != null) {
								aggregatedExceptionListener.delegates.add(listener);
							}
							this.localExceptionListener = listener;
						}
						return null;
					}
					else {
						throw new javax.jms.IllegalStateException(
								"setExceptionListener call not supported on proxy for shared Connection. " +
								"Set the 'exceptionListener' property on the SingleConnectionFactory instead. " +
								"Alternatively, activate SingleConnectionFactory's 'reconnectOnException' feature, " +
								"which will allow for registering further ExceptionListeners to the recovery chain.");
					}
				}
			}
			else if (method.getName().equals("getExceptionListener")) {
				synchronized (connectionMonitor) {
					if (this.localExceptionListener != null) {
						return this.localExceptionListener;
					}
					else {
						return getExceptionListener();
					}
				}
			}
			else if (method.getName().equals("start")) {
				localStart();
				return null;
			}
			else if (method.getName().equals("stop")) {
				localStop();
				return null;
			}
			else if (method.getName().equals("close")) {
				localStop();
				synchronized (connectionMonitor) {
					if (this.localExceptionListener != null) {
						if (aggregatedExceptionListener != null) {
							aggregatedExceptionListener.delegates.remove(this.localExceptionListener);
						}
						this.localExceptionListener = null;
					}
				}
				return null;
			}
			else if (method.getName().equals("createSession") || method.getName().equals("createQueueSession") ||
					method.getName().equals("createTopicSession")) {
				// Default: JMS 2.0 createSession() method
				Integer mode = Session.AUTO_ACKNOWLEDGE;
				if (args != null) {
					if (args.length == 1) {
						// JMS 2.0 createSession(int) method
						mode = (Integer) args[0];
					}
					else if (args.length == 2) {
						// JMS 1.1 createSession(boolean, int) method
						boolean transacted = (Boolean) args[0];
						Integer ackMode = (Integer) args[1];
						mode = (transacted ? Session.SESSION_TRANSACTED : ackMode);
					}
				}
				Session session = getSession(getConnection(), mode);
				if (session != null) {
					if (!method.getReturnType().isInstance(session)) {
						String msg = "JMS Session does not implement specific domain: " + session;
						try {
							session.close();
						}
						catch (Throwable ex) {
							logger.trace("Failed to close newly obtained JMS Session", ex);
						}
						throw new javax.jms.IllegalStateException(msg);
					}
					return session;
				}
			}
			try {
				return method.invoke(getConnection(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private void localStart() throws JMSException {
			synchronized (connectionMonitor) {
				if (!this.locallyStarted) {
					this.locallyStarted = true;
					if (startedCount == 0 && connection != null) {
						connection.start();
					}
					startedCount++;
				}
			}
		}

		private void localStop() throws JMSException {
			synchronized (connectionMonitor) {
				if (this.locallyStarted) {
					this.locallyStarted = false;
					if (startedCount == 1 && connection != null) {
						connection.stop();
					}
					if (startedCount > 0) {
						startedCount--;
					}
				}
			}
		}

		private SingleConnectionFactory factory() {
			return SingleConnectionFactory.this;
		}
	}


	/**
	 * 内部聚合ExceptionListener, 用于结合用户指定的监听器处理内部恢复监听器.
	 */
	private class AggregatedExceptionListener implements ExceptionListener {

		final Set<ExceptionListener> delegates = new LinkedHashSet<ExceptionListener>(2);

		@Override
		public void onException(JMSException ex) {
			// 迭代临时副本以避免ConcurrentModificationException, 因为监听器调用可能反过来触发监听器的注册...
			Set<ExceptionListener> copy;
			synchronized (connectionMonitor) {
				copy = new LinkedHashSet<ExceptionListener>(this.delegates);
			}
			for (ExceptionListener listener : copy) {
				listener.onException(ex);
			}
		}
	}
}
