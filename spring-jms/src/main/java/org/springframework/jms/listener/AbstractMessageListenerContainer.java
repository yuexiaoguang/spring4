package org.springframework.jms.listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * Spring消息监听器容器实现的抽象基类.
 * 可以托管标准JMS {@link javax.jms.MessageListener}或Spring的{@link SessionAwareMessageListener}进行实际的消息处理.
 *
 * <p>通常持有一个JMS {@link Connection}, 所有监听器都应该注册, 这是管理监听器会话的标准JMS方式.
 * 也可以与每个监听器的新连接一起使用, 用于Java EE样式的XA感知JMS消息.
 * 实际的注册过程取决于具体的子类.
 *
 * <p><b>NOTE:</b> 此消息监听器容器的默认行为是<b>从不</b>将消息监听器抛出的异常传播到JMS提供者.
 * 相反, 它会在错误级别记录任何此类异常.
 * 这意味着从伴随的JMS提供者的角度来看, 任何此类监听器都不会失败.
 * 但是, 如果需要进行错误处理, 则可以向{@link #setErrorHandler(ErrorHandler)}方法提供{@link ErrorHandler}策略的任何实现.
 * 请注意, JMSExceptions <b>也将</b>传递给 ErrorHandler, 除了(但之后)传递给{@link ExceptionListener}之外, 如果已经提供了一个.
 *
 * <p>监听器容器提供以下消息确认选项:
 * <ul>
 * <li>"sessionAcknowledgeMode" 设置为 "AUTO_ACKNOWLEDGE" (默认):
 * 此模式取决于容器: 对于{@link DefaultMessageListenerContainer}, 它表示在侦听器执行<i>之前</i>自动消息确认,
 * 如果发生异常也不会重新传递, 并且在其他侦听器执行中断时也不会重新传递.
 * 对于{@link SimpleMessageListenerContainer}, 它表示在侦听器执行<i>之后</i>自动消息确认,
 * 如果抛出用户异常也不会重新传递, 但在JVM在监听器执行期间死亡时可能重新传递.
 * 为了始终如一地安排任何容器变体的重新传递, 考虑"CLIENT_ACKNOWLEDGE"模式或 - 最好 - 将 "sessionTransacted" 设置为 "true".
 * <li>"sessionAcknowledgeMode" 设置为 "DUPS_OK_ACKNOWLEDGE":
 * <i>延迟</i>消息确认, 在({@link DefaultMessageListenerContainer})期间或({@link SimpleMessageListenerContainer})监听器执行后不久;
 * 如果抛出用户异常也不会重新传递, 但在监听器执行期间JVM死亡时可能会重新传递.
 * 为了始终如一地安排任何容器变体的重新传递, 考虑"CLIENT_ACKNOWLEDGE"模式或 - 最好 - 将"sessionTransacted"设置为"true".
 * <li>"sessionAcknowledgeMode" 设置为 "CLIENT_ACKNOWLEDGE":
 * 监听器执行成功<i>后</i>自动确认消息;
 * 在抛出用户异常的情况下以及在其他监听器执行中断 (例如JVM死亡)的情况下, 尽量重新传递.
 * <li>"sessionTransacted" 设置为 "true":
 * 监听器执行成功后的事务确认;
 * <i>保证重新传递</i>, 在抛出用户异常的情况下以及在其他监听器执行中断的情况下 (例如JVM死亡).
 * </ul>
 *
 * <p>重复的消息处理问题有两种解决方案:
 * <ul>
 * <li>以业务实体存在检查的形式向监听器添加<i>重复的消息检测</i>, 或协议表检查.
 * 这通常只需要在传入消息上设置JMSRedelivered标志的情况下完成 (否则只是直接处理).
 * 请注意, 如果将"sessionTransacted" 设置为 "true", 则只有在JVM死亡的情况下才会出现重复消息
 * (i.e. 在业务逻辑执行之后但在JMS部分提交之前), 所以重复的消息检测只是覆盖角落的情况.
 * <li>或者使用XA事务包装整个处理, 包括接收JMS消息以及在消息监听器中执行业务逻辑 (包括数据库操作等).
 * 这仅由{@link DefaultMessageListenerContainer}支持, 通过指定外部"transactionManager"
 * (通常是{@link org.springframework.transaction.jta.JtaTransactionManager},
 * 通常是JMS {@link javax.jms.ConnectionFactory}作为"connectionFactory"传入).
 * </ul>
 * 请注意, XA事务协调会增加大量的运行时开销, 因此除非绝对必要, 否则可能会避免它.
 *
 * <p><b>建议:</b>
 * <ul>
 * <li>一般建议是将"sessionTransacted"设置为"true", 通常与监听器实现触发的本地数据库事务相结合, 通过Spring的标准事务工具.
 * 这将在Tomcat或独立环境中很好地工作, 通常与自定义重复消息检测相结合 (如果两次处理相同的消息是不可接受的).
 * <li>或者, 为完全支持XA的JMS提供者指定{@link org.springframework.transaction.jta.JtaTransactionManager}
 * 作为"transactionManager" - 通常在Java EE服务器上运行时, 也适用于存在JTA事务管理器的其他环境.
 * 这将提供完整的"完全一次"保证, 无需自定义重复消息检查, 代价是额外的运行时处理开销.
 * </ul>
 *
 * <p>请注意，强烈建议在{@link org.springframework.jms.connection.JmsTransactionManager}上
 * 使用"sessionTransacted"标志, 前提是不需要在外部管理事务.
 * 因此, 仅在使用JTA或需要与自定义外部事务安排同步时才设置事务管理器.
 */
public abstract class AbstractMessageListenerContainer extends AbstractJmsListeningContainer
		implements MessageListenerContainer {

	/** The JMS 2.0 Session.createSharedConsumer method, if available */
	private static final Method createSharedConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedConsumer", Topic.class, String.class, String.class);

	/** The JMS 2.0 Session.createSharedDurableConsumer method, if available */
	private static final Method createSharedDurableConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedDurableConsumer", Topic.class, String.class, String.class);


	private volatile Object destination;

	private volatile String messageSelector;

	private volatile Object messageListener;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	private String subscriptionName;

	private Boolean replyPubSubDomain;

	private boolean pubSubNoLocal = false;

	private MessageConverter messageConverter;

	private ExceptionListener exceptionListener;

	private ErrorHandler errorHandler;

	private boolean exposeListenerSession = true;

	private boolean acceptMessagesWhileStopping = false;


	/**
	 * 指定并发限制.
	 */
	public abstract void setConcurrency(String concurrency);

	/**
	 * 设置从中接收消息的目标.
	 * <p>或者, 指定一个"destinationName", 通过{@link org.springframework.jms.support.destination.DestinationResolver}动态解析.
	 * <p>Note: 目标可以在运行时替换, 监听器容器立即拾取新目标
	 * (e.g. 使用DefaultMessageListenerContainer, 只要缓存级别小于CACHE_CONSUMER).
	 * 但是, 这被认为是高级用法; 小心使用它!
	 */
	public void setDestination(Destination destination) {
		this.destination = destination;
		if (destination instanceof Topic && !(destination instanceof Queue)) {
			// 显然是一个 Topic: 相应地设置"pubSubDomain"标志.
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回从中接收消息的目标.
	 * 如果配置的目标不是实际的{@link Destination}类型, 则为{@code null};
	 * c.f. {@link #setDestinationName(String) 当目标是一个字符串}.
	 */
	public Destination getDestination() {
		return (this.destination instanceof Destination ? (Destination) this.destination : null);
	}

	/**
	 * 设置要从中接收消息的目标的名称.
	 * <p>指定的名称将通过配置的{@link #setDestinationResolver 目标解析器}动态解析.
	 * <p>或者, 将JMS {@link Destination}对象指定为 "destination".
	 * <p>Note: 目标可以在运行时替换, 监听器容器立即拾取新目标
	 * (e.g. 使用DefaultMessageListenerContainer, 只要缓存级别小于CACHE_CONSUMER).
	 * 但是, 这被认为是高级用法; 小心使用它!
	 */
	public void setDestinationName(String destinationName) {
		this.destination = destinationName;
	}

	/**
	 * 返回要从中接收消息的目标的名称.
	 * 如果配置的目标不是{@link String}类型, 则为{@code null};
	 * c.f. {@link #setDestination(Destination) 如果是实际的Destination}.
	 */
	public String getDestinationName() {
		return (this.destination instanceof String ? (String) this.destination : null);
	}

	/**
	 * 返回此容器的JMS目标的描述性字符串 (never {@code null}).
	 */
	protected String getDestinationDescription() {
		Object destination = this.destination;
		return (destination != null ? destination.toString() : "");
	}

	/**
	 * 设置JMS消息选择器表达式 (或{@code null}).
	 * 默认无.
	 * <p>有关选择器表达式的详细定义, 请参阅JMS规范.
	 * <p>Note: 目标可以在运行时替换, 监听器容器立即拾取新目标
	 * (e.g. 使用DefaultMessageListenerContainer, 只要缓存级别小于CACHE_CONSUMER).
	 * 但是, 这被认为是高级用法; 小心使用它!
	 */
	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	/**
	 * 返回JMS消息选择器表达式 (或{@code null}).
	 */
	public String getMessageSelector() {
		return this.messageSelector;
	}


	/**
	 * 设置要注册的消息监听器实现.
	 * 这可以是标准的JMS {@link MessageListener}对象, 也可以是Spring {@link SessionAwareMessageListener}对象.
	 * <p>Note: 目标可以在运行时替换, 监听器容器立即拾取新目标
	 * (e.g. 使用DefaultMessageListenerContainer, 只要缓存级别小于CACHE_CONSUMER).
	 * 但是, 这被认为是高级用法; 小心使用它!
	 * 
	 * @throws IllegalArgumentException 如果提供的监听器不是{@link MessageListener}或{@link SessionAwareMessageListener}
	 */
	public void setMessageListener(Object messageListener) {
		checkMessageListener(messageListener);
		this.messageListener = messageListener;
		if (this.subscriptionName == null) {
			this.subscriptionName = getDefaultSubscriptionName(messageListener);
		}
	}

	/**
	 * 返回要注册的消息监听器实现.
	 */
	public Object getMessageListener() {
		return this.messageListener;
	}

	/**
	 * 检查给定的消息监听器, 如果它与支持的监听器类型不对应, 则抛出异常.
	 * <p>默认情况下, 只接受标准JMS {@link MessageListener}对象或 Spring {@link SessionAwareMessageListener}对象.
	 * 
	 * @param messageListener 要检查的消息监听器对象
	 * 
	 * @throws IllegalArgumentException 如果提供的监听器不是{@link MessageListener}或{@link SessionAwareMessageListener}
	 */
	protected void checkMessageListener(Object messageListener) {
		if (!(messageListener instanceof MessageListener ||
				messageListener instanceof SessionAwareMessageListener)) {
			throw new IllegalArgumentException(
					"Message listener needs to be of type [" + MessageListener.class.getName() +
					"] or [" + SessionAwareMessageListener.class.getName() + "]");
		}
	}

	/**
	 * 确定给定消息监听器的默认订阅名称.
	 * 
	 * @param messageListener 要检查的消息监听器对象
	 * 
	 * @return 默认订阅名称
	 */
	protected String getDefaultSubscriptionName(Object messageListener) {
		if (messageListener instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) messageListener).getSubscriptionName();
		}
		else {
			return messageListener.getClass().getName();
		}
	}

	/**
	 * 设置是否使订阅持久.
	 * 可以通过"subscriptionName"属性指定要使用的持久订阅名称.
	 * <p>默认"false". 设置为"true"以注册持久订阅, 通常与"subscriptionName"值结合使用
	 * (除非消息监听器类名称能够作为订阅名称).
	 * <p>只有在监听topic (pub-sub domain)时才有意义, 因此此方法也会切换"pubSubDomain"标志.
	 */
	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回是否使订阅持久.
	 */
	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	/**
	 * 设置是否共享订阅.
	 * 可以通过"subscriptionName"属性指定要使用的共享订阅名称.
	 * <p>默认"false".
	 * 设置为 "true"以注册共享订阅, 通常与"subscriptionName"值结合使用 (除非消息监听器类名称能够作为订阅名称).
	 * 请注意, 共享订阅也可能是持久的, 因此该标志可以 (通常)与"subscriptionDurable"结合使用.
	 * <p>只有在监听topic (pub-sub domain)时才有意义, 因此此方法也会切换"pubSubDomain"标志.
	 * <p><b>需要JMS 2.0兼容的消息代理.</b>
	 */
	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			setPubSubDomain(true);
		}
	}

	/**
	 * 返回是否共享订阅.
	 */
	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	/**
	 * 设置要创建的订阅的名称.
	 * 在具有共享或持久订阅的topic (pub-sub domain)的情况下应用.
	 * <p>订阅名称在此客户端的JMS客户端ID中必须是唯一的. 默认是指定消息监听器的类名.
	 * <p>Note: 除共享订阅(需要JMS 2.0)外, 每个订阅仅允许1个并发消费者(这是此消息监听器容器的默认值).
	 */
	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	/**
	 * 返回要创建的订阅的名称.
	 */
	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	/**
	 * 设置要创建的持久订阅的名称.
	 * 此方法切换到pub-sub域模式并激活订阅持久性.
	 * <p>持久订阅名称在此客户端的JMS客户端ID中必须是唯一的. 默认是指定消息监听器的类名.
	 * <p>Note: 除共享订阅(需要JMS 2.0)外, 每个订阅仅允许1个并发消费者(这是此消息监听器容器的默认值).
	 */
	public void setDurableSubscriptionName(String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = (durableSubscriptionName != null);
	}

	/**
	 * 返回要创建的持久订阅的名称.
	 */
	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	/**
	 * 设置是否禁止传递由其自身连接发布的消息.
	 * 默认 "false".
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * 返回是否禁止传递由其自身连接发布的消息
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * 配置回复目标类型.
	 * 默认情况下, 使用配置的{@code pubSubDomain}值 (see {@link #isPubSubDomain()}.
	 * <p>此设置主要指示启用动态目标时要解析的目标类型.
	 * 
	 * @param replyPubSubDomain "true"表示Publish/Subscribe域 ({@link Topic Topics}),
	 * 							"false"表示Point-to-Point域 ({@link Queue Queues})
	 */
	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	/**
	 * 返回是否将Publish/Subscribe域 ({@link javax.jms.Topic Topics})用于回复.
	 * 否则使用Point-to-Point域 ({@link javax.jms.Queue Queues}).
	 */
	@Override
	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	/**
	 * 设置用于转换JMS消息的{@link MessageConverter}策略.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 设置JMS ExceptionListener, 以便在已注册的消息侦听器或调用基础结构抛出JMSException的情况下进行通知.
	 */
	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * 返回JMS ExceptionListener, 以便在已注册的消息侦听器或调用基础结构抛出JMSException的情况下进行通知.
	 */
	public ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * 设置要调用的ErrorHandler, 如果在处理Message时抛出任何未捕获的异常.
	 * <p>默认情况下, <b>没有</b> ErrorHandler, 以便错误级别日志记录是唯一的结果.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回要调用的ErrorHandler, 如果在处理Message时抛出任何未捕获的异常.
	 */
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * 设置是否将监听器JMS会话公开给已注册的{@link SessionAwareMessageListener}
	 * 以及{@link org.springframework.jms.core.JmsTemplate}调用.
	 * <p>默认"true", 重用监听器的{@link Session}.
	 * 将其关闭, 以公开从相同的底层JMS {@link Connection}获取的新的JMS Session, 这可能是某些JMS提供者所必需的.
	 * <p>请注意, 由外部事务管理器管理的会话将始终暴露给{@link org.springframework.jms.core.JmsTemplate}调用.
	 * 因此, 就JmsTemplate公开而言, 此设置仅影响本地事务的会话.
	 */
	public void setExposeListenerSession(boolean exposeListenerSession) {
		this.exposeListenerSession = exposeListenerSession;
	}

	/**
	 * 返回是否将监听器JMS {@link Session}公开给已注册的{@link SessionAwareMessageListener}.
	 */
	public boolean isExposeListenerSession() {
		return this.exposeListenerSession;
	}

	/**
	 * 设置是否在监听器容器停止过程中接受收到的消息.
	 * <p>默认"false", 通过中止接收尝试拒绝此类消息.
	 * 即使在停止阶段, 也要打开此标志以完全处理此类消息, 其缺点是即使新发送的消息仍可能被处理 (如果在所有接收超时到期之前进入).
	 * <p><b>NOTE:</b> 中止对此类传入消息的接收尝试, 可能会导致提供者的重试次数减少.
	 * 如果有大量并发消费者, 请确保重试次数高于消费者数量, 以确保所有潜在停止场景的安全性.
	 */
	public void setAcceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
		this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
	}

	/**
	 * 返回是否在监听器容器停止的过程中接受收到的消息.
	 */
	public boolean isAcceptMessagesWhileStopping() {
		return this.acceptMessagesWhileStopping;
	}

	@Override
	protected void validateConfiguration() {
		if (this.destination == null) {
			throw new IllegalArgumentException("Property 'destination' or 'destinationName' is required");
		}
	}

	@Override
	public void setupMessageListener(Object messageListener) {
		setMessageListener(messageListener);
	}


	//-------------------------------------------------------------------------
	// Template methods for listener execution
	//-------------------------------------------------------------------------

	/**
	 * 执行指定的监听器, 之后提交或回滚事务.
	 * 
	 * @param session 要运行的JMS会话
	 * @param message 收到的JMS Message
	 */
	protected void executeListener(Session session, Message message) {
		try {
			doExecuteListener(session, message);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * 执行指定的监听器, 之后提交或回滚事务.
	 * 
	 * @param session 要运行的JMS会话
	 * @param message 收到的JMS Message
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doExecuteListener(Session session, Message message) throws JMSException {
		if (!isAcceptMessagesWhileStopping() && !isRunning()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Rejecting received message because of the listener container " +
						"having been stopped in the meantime: " + message);
			}
			rollbackIfNecessary(session);
			throw new MessageRejectedWhileStoppingException();
		}

		try {
			invokeListener(session, message);
		}
		catch (JMSException ex) {
			rollbackOnExceptionIfNecessary(session, ex);
			throw ex;
		}
		catch (RuntimeException ex) {
			rollbackOnExceptionIfNecessary(session, ex);
			throw ex;
		}
		catch (Error err) {
			rollbackOnExceptionIfNecessary(session, err);
			throw err;
		}
		commitIfNecessary(session, message);
	}

	/**
	 * 调用指定的监听器: 作为标准JMS MessageListener 或(最好)作为Spring SessionAwareMessageListener.
	 * 
	 * @param session 要运行的JMS会话
	 * @param message 收到的JMS Message
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	@SuppressWarnings("rawtypes")
	protected void invokeListener(Session session, Message message) throws JMSException {
		Object listener = getMessageListener();

		if (listener instanceof SessionAwareMessageListener) {
			doInvokeListener((SessionAwareMessageListener) listener, session, message);
		}
		else if (listener instanceof MessageListener) {
			doInvokeListener((MessageListener) listener, message);
		}
		else if (listener != null) {
			throw new IllegalArgumentException(
					"Only MessageListener and SessionAwareMessageListener supported: " + listener);
		}
		else {
			throw new IllegalStateException("No message listener specified - see property 'messageListener'");
		}
	}

	/**
	 * 调用指定的监听器, 如果需要, 将新的JMS Session (可能具有自己的事务)暴露给监听器.
	 * 
	 * @param listener 要调用的Spring SessionAwareMessageListener
	 * @param session 要运行的JMS会话
	 * @param message 收到的JMS Message
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void doInvokeListener(SessionAwareMessageListener listener, Session session, Message message)
			throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = session;
			if (!isExposeListenerSession()) {
				// 需要公开一个单独的Session.
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				sessionToUse = sessionToClose;
			}
			// 实际调用消息监听器...
			listener.onMessage(message, sessionToUse);
			// 清理特殊暴露的Session.
			if (sessionToUse != session) {
				if (sessionToUse.getTransacted() && isSessionLocallyTransacted(sessionToUse)) {
					// 由此容器创建的事务会话 -> 提交.
					JmsUtils.commitIfNecessary(sessionToUse);
				}
			}
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			JmsUtils.closeConnection(conToClose);
		}
	}

	/**
	 * 调用指定的监听器.
	 * <p>默认实现执行{@code onMessage}方法的简单调用.
	 * 
	 * @param listener 要调用的JMS MessageListener
	 * @param message 接收的JMS Message
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doInvokeListener(MessageListener listener, Message message) throws JMSException {
		listener.onMessage(message);
	}

	/**
	 * 根据需要执行提交或消息确认.
	 * 
	 * @param session 要提交的JMS Session
	 * @param message 要确认的消息
	 * 
	 * @throws javax.jms.JMSException 提交失败
	 */
	protected void commitIfNecessary(Session session, Message message) throws JMSException {
		// 提交会话或确认消息.
		if (session.getTransacted()) {
			// 提交 - 但避免在JTA事务中提交调用.
			if (isSessionLocallyTransacted(session)) {
				// 由此容器创建的事务会话 -> 提交.
				JmsUtils.commitIfNecessary(session);
			}
		}
		else if (message != null && isClientAcknowledge(session)) {
			message.acknowledge();
		}
	}

	/**
	 * 如果适用, 执行回滚.
	 * 
	 * @param session 要回滚的JMS会话
	 * 
	 * @throws javax.jms.JMSException 回滚错误
	 */
	protected void rollbackIfNecessary(Session session) throws JMSException {
		if (session.getTransacted()) {
			if (isSessionLocallyTransacted(session)) {
				// 由此容器创建的事务会话 -> 回滚.
				JmsUtils.rollbackIfNecessary(session);
			}
		}
		else if (isClientAcknowledge(session)) {
			session.recover();
		}
	}

	/**
	 * 执行回滚, 正确处理回滚异常.
	 * 
	 * @param session 要回滚的JMS会话
	 * @param ex 抛出的应用程序异常或错误
	 * 
	 * @throws javax.jms.JMSException 回滚错误
	 */
	protected void rollbackOnExceptionIfNecessary(Session session, Throwable ex) throws JMSException {
		try {
			if (session.getTransacted()) {
				if (isSessionLocallyTransacted(session)) {
					// 由此容器创建的事务会话 -> 回滚.
					if (logger.isDebugEnabled()) {
						logger.debug("Initiating transaction rollback on application exception", ex);
					}
					JmsUtils.rollbackIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				session.recover();
			}
		}
		catch (IllegalStateException ex2) {
			logger.debug("Could not roll back because Session already closed", ex2);
		}
		catch (JMSException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (RuntimeException ex2) {
			logger.error("Application exception overridden by rollback exception", ex);
			throw ex2;
		}
		catch (Error err) {
			logger.error("Application exception overridden by rollback error", ex);
			throw err;
		}
	}

	/**
	 * 检查给定的Session是否是本地事务的, 即它的事务是由该监听器容器的会话处理管理, 而不是由外部事务协调器管理.
	 * <p>Note: 会话自己的事务标志之前已经检查.
	 * 此方法用于查明会话的事务是本地还是外部协调.
	 * 
	 * @param session 要检查的会话
	 * 
	 * @return 给定的会话是否是本地事务的
	 */
	protected boolean isSessionLocallyTransacted(Session session) {
		return isSessionTransacted();
	}

	/**
	 * 为给定的Session和Destination创建JMS MessageConsumer.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param session 用于创建MessageConsumer的JMS会话
	 * @param destination 用于创建MessageConsumer的JMS目标
	 * 
	 * @return 新的JMS MessageConsumer
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
		if (isPubSubDomain() && destination instanceof Topic) {
			if (isSubscriptionShared()) {
				// createSharedConsumer((Topic) dest, subscription, selector);
				// createSharedDurableConsumer((Topic) dest, subscription, selector);
				Method method = (isSubscriptionDurable() ?
						createSharedDurableConsumerMethod : createSharedConsumerMethod);
				try {
					return (MessageConsumer) method.invoke(session, destination, getSubscriptionName(), getMessageSelector());
				}
				catch (InvocationTargetException ex) {
					if (ex.getTargetException() instanceof JMSException) {
						throw (JMSException) ex.getTargetException();
					}
					ReflectionUtils.handleInvocationTargetException(ex);
					return null;
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException("Could not access JMS 2.0 API method: " + ex.getMessage());
				}
			}
			else if (isSubscriptionDurable()) {
				return session.createDurableSubscriber(
						(Topic) destination, getSubscriptionName(), getMessageSelector(), isPubSubNoLocal());
			}
			else {
				// 仅在Topic (pub-sub 模式)的情况下传入NoLocal标志:
				// 某些JMS提供者, 如WebSphere MQ 6.0, 在为Queue指定NoLocal标志的情况下抛出IllegalStateException.
				return session.createConsumer(destination, getMessageSelector(), isPubSubNoLocal());
			}
		}
		else {
			return session.createConsumer(destination, getMessageSelector());
		}
	}

	/**
	 * 处理在监听器执行期间出现的给定异常.
	 * <p>默认实现在警告级别记录异常, 而不是将其传播到JMS提供者 &mdash;
	 * 假设所有确认和事务的处理都由此监听器容器完成.
	 * 可以在子类中重写.
	 * 
	 * @param ex 要处理的异常
	 */
	protected void handleListenerException(Throwable ex) {
		if (ex instanceof MessageRejectedWhileStoppingException) {
			// 内部异常 - 之前已经处理过.
			return;
		}
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (isActive()) {
			// 常规情况: 活动时失败.
			// 调用ErrorHandler.
			invokeErrorHandler(ex);
		}
		else {
			// 罕见的情况: 容器关闭后监听器线程失败.
			// 在调试级别记录, 以避免垃圾关闭日志.
			logger.debug("Listener exception after container shutdown", ex);
		}
	}

	/**
	 * 调用已注册的JMS ExceptionListener.
	 * 
	 * @param ex 在JMS处理期间出现的异常
	 */
	protected void invokeExceptionListener(JMSException ex) {
		ExceptionListener exceptionListener = getExceptionListener();
		if (exceptionListener != null) {
			exceptionListener.onException(ex);
		}
	}

	/**
	 * 调用已注册的ErrorHandler. 否则在警告级别记录.
	 * 
	 * @param ex 在JMS处理期间出现的未捕获错误.
	 */
	protected void invokeErrorHandler(Throwable ex) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			errorHandler.handleError(ex);
		}
		else {
			logger.warn("Execution of JMS message listener failed, and no ErrorHandler has been set.", ex);
		}
	}


	/**
	 * 内部异常类, 指示关闭时被拒绝的消息.
	 * 在这种情况下, 用于触发外部事务管理器的回滚.
	 */
	@SuppressWarnings("serial")
	private static class MessageRejectedWhileStoppingException extends RuntimeException {
	}

}
