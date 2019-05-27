package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.support.JmsUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * 基于轮询的监听器容器实现的基类.
 * 基于{@link javax.jms.MessageConsumer}为监听器处理提供支持, 可选地参与外部管理的事务.
 *
 * <p>此监听器容器变体是为重复轮询尝试而构建的, 每个都调用{@link #receiveAndExecute}方法.
 * 所使用的MessageConsumer可以在尝试之间重新获取或缓存; 这取决于具体实现.
 * 可以通过{@link #setReceiveTimeout "receiveTimeout"}属性配置每次尝试的接收超时.
 *
 * <p>底层机制基于标准JMS MessageConsumer处理, 它与Java EE环境中的JMS和本机JMS完全兼容.
 * JMS {@code MessageConsumer.setMessageListener}工具和JMS ServerSessionPool工具都不是必需的.
 * 这种方法的另一个优点是可以完全控制监听过程, 允许自定义扩展和限制以及并发消息处理 (这取决于具体的子类).
 *
 * <p>通过将Spring {@link org.springframework.transaction.PlatformTransactionManager}
 * 传递到{@link #setTransactionManager "transactionManager"}属性, 消息接收和监听器执行可以自动封装在事务中.
 * 这通常是Java EE环境中的{@link org.springframework.transaction.jta.JtaTransactionManager},
 * 以及从JNDI获取的JTA感知JMS ConnectionFactory (请查看应用程序服务器的文档).
 *
 * <p>此基类不假定用于轮询调用器的异步执行的任何特定机制.
 * 查看{@link DefaultMessageListenerContainer}, 了解基于Spring的{@link org.springframework.core.task.TaskExecutor}抽象的具体实现,
 * 包括并发消费者的动态扩展和自动自我恢复.
 */
public abstract class AbstractPollingMessageListenerContainer extends AbstractMessageListenerContainer {

	/**
	 * 默认接收超时: 1000 ms = 1 second.
	 */
	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;


	private final MessageListenerContainerResourceFactory transactionalResourceFactory =
			new MessageListenerContainerResourceFactory();

	private boolean sessionTransactedCalled = false;

	private PlatformTransactionManager transactionManager;

	private DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;


	@Override
	public void setSessionTransacted(boolean sessionTransacted) {
		super.setSessionTransacted(sessionTransacted);
		this.sessionTransactedCalled = true;
	}

	/**
	 * 指定Spring {@link org.springframework.transaction.PlatformTransactionManager},
	 * 用于事务包装消息接收和监听器执行.
	 * <p>默认无, 不执行任何事务包装.
	 * 如果指定, 这通常是Spring {@link org.springframework.transaction.jta.JtaTransactionManager}或其子类之一,
	 * 结合JTA感知的ConnectionFactory, 此消息监听器容器从中获取其连接.
	 * <p><b>Note: 考虑使用本地JMS事务.</b>
	 * 将{@link #setSessionTransacted "sessionTransacted"}标志切换为"true", 以便为整个接收处理使用本地事务的JMS会话,
	 * 包括{@link SessionAwareMessageListener}执行的任何会话操作 (e.g. 发送响应消息).
	 * 这允许基于本地JMS事务的完全同步的Spring事务, 类似于{@link org.springframework.jms.connection.JmsTransactionManager}提供的事务.
	 * 检查{@link AbstractMessageListenerContainer}的javadoc, 讨论事务选择和消息重新传递场景.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * 返回Spring PlatformTransactionManager, 用于事务包装消息接收和侦听器执行.
	 */
	protected final PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * 指定用于事务包装的事务名称.
	 * 默认是此监听器容器的bean名称.
	 */
	public void setTransactionName(String transactionName) {
		this.transactionDefinition.setName(transactionName);
	}

	/**
	 * 指定用于事务包装的事务超时, 以<b>秒</b>为单位.
	 * 默认无, 使用事务管理器的默认超时.
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionDefinition.setTimeout(transactionTimeout);
	}

	/**
	 * 设置用于接收调用的超时时间, 单位为<b>毫秒</b>.
	 * 默认是 1000 ms, 即1 秒.
	 * <p><b>NOTE:</b> 此值需要小于事务管理器使用的事务超时 (当然, 在适当的单位中).
	 * 0 表示不会超时; 但是, 这只有不在事务管理器中运行时才可行, 并且通常不鼓励, 因为这样的监听器容器无法干净地关闭.
	 * 诸如-1的负值表示不等待接收操作.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回为此监听器容器配置的接收超时 (ms).
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void initialize() {
		// 如果是非JTA事务管理器, 则设置sessionTransacted=true.
		if (!this.sessionTransactedCalled &&
				this.transactionManager instanceof ResourceTransactionManager &&
				!TransactionSynchronizationUtils.sameResourceFactory(
						(ResourceTransactionManager) this.transactionManager, getConnectionFactory())) {
			super.setSessionTransacted(true);
		}

		// 使用bean名称作为默认事务名称.
		if (this.transactionDefinition.getName() == null) {
			this.transactionDefinition.setName(getBeanName());
		}

		// 继续进行超类初始化.
		super.initialize();
	}


	/**
	 * 为给定的JMS会话创建MessageConsumer, 为指定的侦听器注册MessageListener.
	 * 
	 * @param session 要使用的JMS会话
	 * 
	 * @return the MessageConsumer
	 * @throws javax.jms.JMSException 如果由JMS方法抛出
	 */
	protected MessageConsumer createListenerConsumer(Session session) throws JMSException {
		Destination destination = getDestination();
		if (destination == null) {
			destination = resolveDestinationName(session, getDestinationName());
		}
		return createConsumer(session, destination);
	}

	/**
	 * 执行监听器, 以获取从给定消费者接收的消息, 如果需要, 将整个操作包装在外部事务中.
	 * 
	 * @param session 要使用的JMS会话
	 * @param consumer 要使用的MessageConsumer
	 * 
	 * @return 是否已收到消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer)
			throws JMSException {

		if (this.transactionManager != null) {
			// 在事务中执行接收.
			TransactionStatus status = this.transactionManager.getTransaction(this.transactionDefinition);
			boolean messageReceived;
			try {
				messageReceived = doReceiveAndExecute(invoker, session, consumer, status);
			}
			catch (JMSException ex) {
				rollbackOnException(status, ex);
				throw ex;
			}
			catch (RuntimeException ex) {
				rollbackOnException(status, ex);
				throw ex;
			}
			catch (Error err) {
				rollbackOnException(status, err);
				throw err;
			}
			this.transactionManager.commit(status);
			return messageReceived;
		}

		else {
			// 在事务之外执行接收.
			return doReceiveAndExecute(invoker, session, consumer, null);
		}
	}

	/**
	 * 实际执行监听器, 以获取从给定消费者接收的消息, 获取所有需要的资源并调用监听器.
	 * 
	 * @param session 要使用的JMS Session
	 * @param consumer 要使用的MessageConsumer
	 * @param status the TransactionStatus (may be {@code null})
	 * 
	 * @return 是否已收到消息
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected boolean doReceiveAndExecute(
			Object invoker, Session session, MessageConsumer consumer, TransactionStatus status)
			throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		MessageConsumer consumerToClose = null;
		try {
			Session sessionToUse = session;
			boolean transactional = false;
			if (sessionToUse == null) {
				sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
						getConnectionFactory(), this.transactionalResourceFactory, true);
				transactional = (sessionToUse != null);
			}
			if (sessionToUse == null) {
				Connection conToUse;
				if (sharedConnectionEnabled()) {
					conToUse = getSharedConnection();
				}
				else {
					conToUse = createConnection();
					conToClose = conToUse;
					conToUse.start();
				}
				sessionToUse = createSession(conToUse);
				sessionToClose = sessionToUse;
			}
			MessageConsumer consumerToUse = consumer;
			if (consumerToUse == null) {
				consumerToUse = createListenerConsumer(sessionToUse);
				consumerToClose = consumerToUse;
			}
			Message message = receiveMessage(consumerToUse);
			if (message != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received message of type [" + message.getClass() + "] from consumer [" +
							consumerToUse + "] of " + (transactional ? "transactional " : "") + "session [" +
							sessionToUse + "]");
				}
				messageReceived(invoker, sessionToUse);
				boolean exposeResource = (!transactional && isExposeListenerSession() &&
						!TransactionSynchronizationManager.hasResource(getConnectionFactory()));
				if (exposeResource) {
					TransactionSynchronizationManager.bindResource(
							getConnectionFactory(), new LocallyExposedJmsResourceHolder(sessionToUse));
				}
				try {
					doExecuteListener(sessionToUse, message);
				}
				catch (Throwable ex) {
					if (status != null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Rolling back transaction because of listener exception thrown: " + ex);
						}
						status.setRollbackOnly();
					}
					handleListenerException(ex);
					// 重新抛出JMSException指示可能必须触发恢复的基础结构问题...
					if (ex instanceof JMSException) {
						throw (JMSException) ex;
					}
				}
				finally {
					if (exposeResource) {
						TransactionSynchronizationManager.unbindResource(getConnectionFactory());
					}
				}
				// 表示已收到消息.
				return true;
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Consumer [" + consumerToUse + "] of " + (transactional ? "transactional " : "") +
							"session [" + sessionToUse + "] did not receive a message");
				}
				noMessageReceived(invoker, sessionToUse);
				// 尽管如此, 调用提交, 以重置事务超时.
				if (shouldCommitAfterNoMessageReceived(sessionToUse)) {
					commitIfNecessary(sessionToUse, message);
				}
				// 表示未收到任何消息.
				return false;
			}
		}
		finally {
			JmsUtils.closeMessageConsumer(consumerToClose);
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), true);
		}
	}

	/**
	 * 此实现检查会话是否在外部同步.
	 * 在这种情况下, 尽管监听器容器的"sessionTransacted"标志设置为"true", 但Session不是本地事务处理的.
	 */
	@Override
	protected boolean isSessionLocallyTransacted(Session session) {
		if (!super.isSessionLocallyTransacted(session)) {
			return false;
		}
		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(getConnectionFactory());
		return (resourceHolder == null || resourceHolder instanceof LocallyExposedJmsResourceHolder ||
				!resourceHolder.containsSession(session));
	}

	/**
	 * 确定在没有收到消息后是否触发提交.
	 * 
	 * @param session 没有收到任何消息的当前JMS会话
	 * 
	 * @return 是否在给定的Session上调用{@link #commitIfNecessary}
	 */
	protected boolean shouldCommitAfterNoMessageReceived(Session session) {
		return true;
	}

	/**
	 * 执行回滚, 正确处理回滚异常.
	 * 
	 * @param status 事务
	 * @param ex 抛出的监听器异常或错误
	 */
	private void rollbackOnException(TransactionStatus status, Throwable ex) {
		logger.debug("Initiating transaction rollback on listener exception", ex);
		try {
			this.transactionManager.rollback(status);
		}
		catch (RuntimeException ex2) {
			logger.error("Listener exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (Error err) {
			logger.error("Listener exception overridden by rollback error", ex);
			throw err;
		}
	}

	/**
	 * 接收来自给定消费者的消息.
	 * 
	 * @param consumer 要使用的MessageConsumer
	 * 
	 * @return Message, 或{@code null}
	 * @throws JMSException 如果由JMS方法抛出
	 */
	protected Message receiveMessage(MessageConsumer consumer) throws JMSException {
		return receiveFromConsumer(consumer, getReceiveTimeout());
	}

	/**
	 * 在尝试处理新消息之前, 收到新消息时调用的模板方法.
	 * 允许子类对实际传入消息的事件做出反应, 例如调整其消费者计数.
	 * 
	 * @param invoker 调用者对象 (通过)
	 * @param session 接收JMS Session
	 */
	protected void messageReceived(Object invoker, Session session) {
	}

	/**
	 * 在再次返回到接收循环之前, 当没有收到消息时调用的模板方法.
	 * 允许子类对没有传入消息的事件做出反应, 例如将调用者标记为空闲.
	 * 
	 * @param invoker 调用者对象 (通过)
	 * @param session 接收JMS Session
	 */
	protected void noMessageReceived(Object invoker, Session session) {
	}

	/**
	 * 从给定的JmsResourceHolder获取适当的连接.
	 * <p>此实现接受任何JMS 1.1 Connection.
	 * 
	 * @param holder the JmsResourceHolder
	 * 
	 * @return 从持有者获取的适当Connection, 或{@code null}
	 */
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection();
	}

	/**
	 * 从给定的JmsResourceHolder中获取适当的Session.
	 * <p>此实现接受任何JMS 1.1 Session.
	 * 
	 * @param holder the JmsResourceHolder
	 * 
	 * @return 从持有者获取的适当Session, 或{@code null}
	 */
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession();
	}


	/**
	 * ResourceFactory实现, 它委托给此监听器容器的受保护的回调方法.
	 */
	private class MessageListenerContainerResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		public Connection getConnection(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getConnection(holder);
		}

		@Override
		public Session getSession(JmsResourceHolder holder) {
			return AbstractPollingMessageListenerContainer.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			if (AbstractPollingMessageListenerContainer.this.sharedConnectionEnabled()) {
				Connection sharedCon = AbstractPollingMessageListenerContainer.this.getSharedConnection();
				return new SingleConnectionFactory(sharedCon).createConnection();
			}
			else {
				return AbstractPollingMessageListenerContainer.this.createConnection();
			}
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return AbstractPollingMessageListenerContainer.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return AbstractPollingMessageListenerContainer.this.isSessionTransacted();
		}
	}
}
