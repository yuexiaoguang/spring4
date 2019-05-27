package org.springframework.jms.core;

import java.lang.reflect.Method;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 简化了同步JMS访问代码.
 *
 * <p>如果要使用动态目标创建, 则必须使用"pubSubDomain" 属性指定要创建的JMS目标的类型.
 * 对于其他操作, 这不是必需的. Point-to-Point (Queues)是默认域.
 *
 * <p>JMS会话的默认设置为"没有事务"和"自动确认".
 * 根据Java EE规范的定义, 无论JTA事务还是Spring管理的事务, 在活动事务内创建JMS会话时都会忽略事务和确认参数.
 * 要为本机JMS用法配置它们, 请为"sessionTransacted"和"sessionAcknowledgeMode" bean属性指定适当的值.
 *
 * <p>此模板分别使用
 * {@link org.springframework.jms.support.destination.DynamicDestinationResolver}
 * 和{@link org.springframework.jms.support.converter.SimpleMessageConverter}
 * 作为解析目标名称或转换消息的默认策略.
 * 可以通过"destinationResolver" 和 "messageConverter" bean属性覆盖这些默认值.
 *
 * <p><b>NOTE: 与此模板一起使用的{@code ConnectionFactory}应返回池中的连接 (或单个共享Connection),
 * 以及池中的会话和 MessageProducer.
 * 否则, ad-hoc JMS操作的性能将受到影响.</b>
 * 最简单的选择是使用Spring提供的{@link org.springframework.jms.connection.SingleConnectionFactory}
 * 作为目标{@code ConnectionFactory}的装饰器, 以线程安全的方式重用单个JMS连接;
 * 这通常足以通过此模板发送消息.
 * 在Java EE环境中, 确保通过JNDI从应用程序的环境命名上下文获取{@code ConnectionFactory};
 * 应用程序服务器通常在那里公开池化的, 事务感知的工厂.
 */
public class JmsTemplate extends JmsDestinationAccessor implements JmsOperations {

	/** JMS 2.0 MessageProducer.setDeliveryDelay方法 */
	private static final Method setDeliveryDelayMethod =
			ClassUtils.getMethodIfAvailable(MessageProducer.class, "setDeliveryDelay", long.class);

	/** 内部ResourceFactory适配器, 用于与ConnectionFactoryUtils交互 */
	private final JmsTemplateResourceFactory transactionalResourceFactory = new JmsTemplateResourceFactory();


	private Object defaultDestination;

	private MessageConverter messageConverter;


	private boolean messageIdEnabled = true;

	private boolean messageTimestampEnabled = true;

	private boolean pubSubNoLocal = false;

	private long receiveTimeout = RECEIVE_TIMEOUT_INDEFINITE_WAIT;

	private long deliveryDelay = -1;


	private boolean explicitQosEnabled = false;

	private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

	private int priority = Message.DEFAULT_PRIORITY;

	private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;


	/**
	 * <p>Note: 必须在使用实例之前设置ConnectionFactory.
	 * 此构造函数可用于通过BeanFactory准备JmsTemplate, 通常通过setConnectionFactory设置ConnectionFactory.
	 */
	public JmsTemplate() {
		initDefaultStrategies();
	}

	/**
	 * @param connectionFactory 从中获取Connection的ConnectionFactory
	 */
	public JmsTemplate(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * 初始化模板策略的默认实现:
	 * DynamicDestinationResolver 和 SimpleMessageConverter.
	 */
	protected void initDefaultStrategies() {
		setMessageConverter(new SimpleMessageConverter());
	}


	/**
	 * 设置要在没有目标参数的发送/接收操作上使用的目标.
	 * <p>或者, 指定"defaultDestinationName", 以通过DestinationResolver动态解析.
	 */
	public void setDefaultDestination(Destination destination) {
		this.defaultDestination = destination;
	}

	/**
	 * 返回要在没有目标参数的发送/接收操作上使用的目标.
	 */
	public Destination getDefaultDestination() {
		return (this.defaultDestination instanceof Destination ? (Destination) this.defaultDestination : null);
	}

	private Queue getDefaultQueue() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null && !(defaultDestination instanceof Queue)) {
			throw new IllegalStateException(
					"'defaultDestination' does not correspond to a Queue. Check configuration of JmsTemplate.");
		}
		return (Queue) defaultDestination;
	}

	/**
	 * 设置要在没有目标参数的发送/接收操作上使用的目标名称.
	 * 指定的名称将通过DestinationResolver动态解析.
	 * <p>或者, 将JMS Destination对象指定为"defaultDestination".
	 */
	public void setDefaultDestinationName(String destinationName) {
		this.defaultDestination = destinationName;
	}

	/**
	 * 返回要在没有目标参数的发送/接收操作上使用的目标名称.
	 */
	public String getDefaultDestinationName() {
		return (this.defaultDestination instanceof String ? (String) this.defaultDestination : null);
	}

	private String getRequiredDefaultDestinationName() throws IllegalStateException {
		String name = getDefaultDestinationName();
		if (name == null) {
			throw new IllegalStateException(
					"No 'defaultDestination' or 'defaultDestinationName' specified. Check configuration of JmsTemplate.");
		}
		return name;
	}

	/**
	 * 设置此模板的消息转换器.
	 * 用于解析convertAndSend方法的Object参数和来自receiveAndConvert方法的Object结果.
	 * <p>默认转换器是SimpleMessageConverter, 它能够处理BytesMessage, TextMessage和ObjectMessage.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回此模板的消息转换器.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	private MessageConverter getRequiredMessageConverter() throws IllegalStateException {
		MessageConverter converter = getMessageConverter();
		if (converter == null) {
			throw new IllegalStateException("No 'messageConverter' specified. Check configuration of JmsTemplate.");
		}
		return converter;
	}


	/**
	 * 设置是否启用消息ID. 默认"true".
	 * <p>这只是JMS生产者的一个提示.
	 * See the JMS javadocs for details.
	 */
	public void setMessageIdEnabled(boolean messageIdEnabled) {
		this.messageIdEnabled = messageIdEnabled;
	}

	/**
	 * 返回是否启用消息ID.
	 */
	public boolean isMessageIdEnabled() {
		return this.messageIdEnabled;
	}

	/**
	 * 设置是否启用消息时间戳. 默认"true".
	 * <p>这只是JMS生产者的一个提示.
	 * See the JMS javadocs for details.
	 */
	public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
		this.messageTimestampEnabled = messageTimestampEnabled;
	}

	/**
	 * 返回是否启用消息时间戳.
	 */
	public boolean isMessageTimestampEnabled() {
		return this.messageTimestampEnabled;
	}

	/**
	 * 设置是否禁止传递由其自身连接发布的消息.
	 * 默认"false".
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * 返回是否禁止传递由其自身连接发布的消息.
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * 设置用于接收调用的超时 (以毫秒为单位).
	 * <p>默认{@link #RECEIVE_TIMEOUT_INDEFINITE_WAIT}, 表示阻塞接收没有超时.
	 * <p>指定{@link #RECEIVE_TIMEOUT_NO_WAIT} (或任何其他负值) 以指示接收操作应检查消息是否立即可用而不阻塞.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回用于接收调用的超时 (以毫秒为单位).
	 */
	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	/**
	 * 设置用于发送调用的传送延迟 (以毫秒为单位).
	 * <p>默认 -1 (传递给代理没有传递延迟).
	 * 请注意, 此功能需要JMS 2.0.
	 */
	public void setDeliveryDelay(long deliveryDelay) {
		this.deliveryDelay = deliveryDelay;
	}

	/**
	 * 返回用于发送调用的传送延迟 (以毫秒为单位).
	 */
	public long getDeliveryDelay() {
		return this.deliveryDelay;
	}


	/**
	 * 设置是否应使用QOS值 (deliveryMode, priority, timeToLive) 来发送消息.
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * 如果为"true", 则在发送消息时将使用deliveryMode, priority, 和 timeToLive 的值.
	 * 否则, 将使用可以在管理上设置的默认值.
	 * 
	 * @return true 如果覆盖QOS参数的默认值 (deliveryMode, priority, 和 timeToLive)
	 */
	public boolean isExplicitQosEnabled() {
		return this.explicitQosEnabled;
	}

	/**
	 * 设置消息传递应该是持久的还是非持久的 ("true" 或 "false").
	 * 这将相应地设置传送模式为 "PERSISTENT" (2) 或 "NON_PERSISTENT" (1).
	 * <p>默认"true" a.k.a. 传送模式 "PERSISTENT".
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * 设置发送消息时使用的传送模式.
	 * 默认值是JMS Message默认值: "PERSISTENT".
	 * <p>由于可以通过管理方式定义默认值, 因此仅在"isExplicitQosEnabled"等于"true"时使用.
	 * 
	 * @param deliveryMode 要使用的传送方式
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * 返回发送消息时使用的传送模式.
	 */
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	/**
	 * 设置发送时消息的优先级.
	 * <p>由于可以通过管理方式定义默认值, 因此仅在"isExplicitQosEnabled" 等于 "true"时使用.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 返回发送时消息的优先级.
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * 设置发送时消息的生存时间.
	 * <p>由于可以通过管理方式定义默认值, 因此仅在"isExplicitQosEnabled" 等于 "true"时使用.
	 * 
	 * @param timeToLive 消息的生存时间 (以毫秒为单位)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * 返回发送时消息的生存时间.
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}


	//---------------------------------------------------------------------------------------
	// JmsOperations execute methods
	//---------------------------------------------------------------------------------------

	@Override
	public <T> T execute(SessionCallback<T> action) throws JmsException {
		return execute(action, false);
	}

	/**
	 * 在JMS会话中执行给定操作对象指定的操作.
	 * {@code execute(SessionCallback)}的通用版本, 允许动态启动JMS连接.
	 * <p>一般使用{@code execute(SessionCallback)}.
	 * 启动JMS连接只是接收消息所必需的, 最好通过{@code receive}方法实现.
	 * 
	 * @param action 公开Session的回调对象
	 * @param startConnection 是否启动Connection
	 * 
	 * @return 使用Session的结果对象
	 * @throws JmsException 出错
	 */
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
					getConnectionFactory(), this.transactionalResourceFactory, startConnection);
			if (sessionToUse == null) {
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				if (startConnection) {
					conToClose.start();
				}
				sessionToUse = sessionToClose;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + sessionToUse);
			}
			return action.doInJms(sessionToUse);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), startConnection);
		}
	}

	@Override
	public <T> T execute(ProducerCallback<T> action) throws JmsException {
		String defaultDestinationName = getDefaultDestinationName();
		if (defaultDestinationName != null) {
			return execute(defaultDestinationName, action);
		}
		else {
			return execute(getDefaultDestination(), action);
		}
	}

	@Override
	public <T> T execute(final Destination destination, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				MessageProducer producer = createProducer(session, destination);
				try {
					return action.doInJms(session, producer);
				}
				finally {
					JmsUtils.closeMessageProducer(producer);
				}
			}
		}, false);
	}

	@Override
	public <T> T execute(final String destinationName, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				MessageProducer producer = createProducer(session, destination);
				try {
					return action.doInJms(session, producer);
				}
				finally {
					JmsUtils.closeMessageProducer(producer);
				}
			}
		}, false);
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

	@Override
	public void send(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			send(defaultDestination, messageCreator);
		}
		else {
			send(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	public void send(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		execute(new SessionCallback<Object>() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				doSend(session, destination, messageCreator);
				return null;
			}
		}, false);
	}

	@Override
	public void send(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		execute(new SessionCallback<Object>() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				doSend(session, destination, messageCreator);
				return null;
			}
		}, false);
	}

	/**
	 * 发送给定的JMS消息.
	 * 
	 * @param session 要运行的JMS会话
	 * @param destination 要发送到的JMS目标
	 * @param messageCreator 创建JMS消息的回调
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doSend(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		MessageProducer producer = createProducer(session, destination);
		try {
			Message message = messageCreator.createMessage(session);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + message);
			}
			doSend(producer, message);
			// 检查提交 - 避免在JTA事务中提交调用.
			if (session.getTransacted() && isSessionLocallyTransacted(session)) {
				// 由此模板创建的事务会话 -> 提交.
				JmsUtils.commitIfNecessary(session);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 实际发送给定的JMS消息.
	 * 
	 * @param producer 使用的JMS MessageProducer
	 * @param message 要发送的JMS消息
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doSend(MessageProducer producer, Message message) throws JMSException {
		if (this.deliveryDelay >= 0) {
			if (setDeliveryDelayMethod == null) {
				throw new IllegalStateException("setDeliveryDelay requires JMS 2.0");
			}
			ReflectionUtils.invokeMethod(setDeliveryDelayMethod, producer, this.deliveryDelay);
		}
		if (isExplicitQosEnabled()) {
			producer.send(message, getDeliveryMode(), getPriority(), getTimeToLive());
		}
		else {
			producer.send(message);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

	@Override
	public void convertAndSend(Object message) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message);
		}
	}

	@Override
	public void convertAndSend(Destination destination, final Object message) throws JmsException {
		send(destination, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return getRequiredMessageConverter().toMessage(message, session);
			}
		});
	}

	@Override
	public void convertAndSend(String destinationName, final Object message) throws JmsException {
		send(destinationName, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return getRequiredMessageConverter().toMessage(message, session);
			}
		});
	}

	@Override
	public void convertAndSend(Object message, MessagePostProcessor postProcessor) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message, postProcessor);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message, postProcessor);
		}
	}

	@Override
	public void convertAndSend(
			Destination destination, final Object message, final MessagePostProcessor postProcessor)
			throws JmsException {

		send(destination, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				Message msg = getRequiredMessageConverter().toMessage(message, session);
				return postProcessor.postProcessMessage(msg);
			}
		});
	}

	@Override
	public void convertAndSend(
			String destinationName, final Object message, final MessagePostProcessor postProcessor)
		throws JmsException {

		send(destinationName, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				Message msg = getRequiredMessageConverter().toMessage(message, session);
				return postProcessor.postProcessMessage(msg);
			}
		});
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//---------------------------------------------------------------------------------------

	@Override
	public Message receive() throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receive(defaultDestination);
		}
		else {
			return receive(getRequiredDefaultDestinationName());
		}
	}

	@Override
	public Message receive(Destination destination) throws JmsException {
		return receiveSelected(destination, null);
	}

	@Override
	public Message receive(String destinationName) throws JmsException {
		return receiveSelected(destinationName, null);
	}

	@Override
	public Message receiveSelected(String messageSelector) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receiveSelected(defaultDestination, messageSelector);
		}
		else {
			return receiveSelected(getRequiredDefaultDestinationName(), messageSelector);
		}
	}

	@Override
	public Message receiveSelected(final Destination destination, final String messageSelector) throws JmsException {
		return execute(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				return doReceive(session, destination, messageSelector);
			}
		}, true);
	}

	@Override
	public Message receiveSelected(final String destinationName, final String messageSelector) throws JmsException {
		return execute(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				return doReceive(session, destination, messageSelector);
			}
		}, true);
	}

	/**
	 * 接收JMS消息.
	 * 
	 * @param session 要运行的JMS会话
	 * @param destination 要从中接收的JMS目标
	 * @param messageSelector 此消费者的消息选择器 (can be {@code null})
	 * 
	 * @return 收到的JMS消息, 或{@code null}
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Message doReceive(Session session, Destination destination, String messageSelector)
			throws JMSException {

		return doReceive(session, createConsumer(session, destination, messageSelector));
	}

	/**
	 * 实际接收一条JMS消息.
	 * 
	 * @param session 要运行的JMS会话
	 * @param consumer 使用的JMS MessageConsumer
	 * 
	 * @return 收到的JMS消息, 或{@code null}
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Message doReceive(Session session, MessageConsumer consumer) throws JMSException {
		try {
			// Use transaction timeout (if available).
			long timeout = getReceiveTimeout();
			JmsResourceHolder resourceHolder =
					(JmsResourceHolder) TransactionSynchronizationManager.getResource(getConnectionFactory());
			if (resourceHolder != null && resourceHolder.hasTimeout()) {
				timeout = Math.min(timeout, resourceHolder.getTimeToLiveInMillis());
			}
			Message message = receiveFromConsumer(consumer, timeout);
			if (session.getTransacted()) {
				// 提交 - 但避免在JTA事务中提交调用.
				if (isSessionLocallyTransacted(session)) {
					// 由此模板创建的事务会话 -> 提交.
					JmsUtils.commitIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				// 手动确认消息.
				if (message != null) {
					message.acknowledge();
				}
			}
			return message;
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//---------------------------------------------------------------------------------------

	@Override
	public Object receiveAndConvert() throws JmsException {
		return doConvertFromMessage(receive());
	}

	@Override
	public Object receiveAndConvert(Destination destination) throws JmsException {
		return doConvertFromMessage(receive(destination));
	}

	@Override
	public Object receiveAndConvert(String destinationName) throws JmsException {
		return doConvertFromMessage(receive(destinationName));
	}

	@Override
	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(messageSelector));
	}

	@Override
	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destination, messageSelector));
	}

	@Override
	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destinationName, messageSelector));
	}

	/**
	 * 从给定的JMS消息中提取内容.
	 * 
	 * @param message 要转换的JMS消息 (can be {@code null})
	 * 
	 * @return 消息的内容, 或{@code null}
	 */
	protected Object doConvertFromMessage(Message message) {
		if (message != null) {
			try {
				return getRequiredMessageConverter().fromMessage(message);
			}
			catch (JMSException ex) {
				throw convertJmsAccessException(ex);
			}
		}
		return null;
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages to and receiving the reply from a destination
	//---------------------------------------------------------------------------------------

	@Override
	public Message sendAndReceive(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return sendAndReceive(defaultDestination, messageCreator);
		}
		else {
			return sendAndReceive(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	public Message sendAndReceive(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				return doSendAndReceive(session, destination, messageCreator);
			}
		}, true);
	}

	@Override
	public Message sendAndReceive(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				return doSendAndReceive(session, destination, messageCreator);
			}
		}, true);
	}

	/**
	 * 向给定的{@link Destination}发送请求消息并阻塞, 直到在动态创建的临时队列上收到回复为止.
	 * <p>返回响应消息或{@code null}
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Message doSendAndReceive(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			Message requestMessage = messageCreator.createMessage(session);
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(destination);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + requestMessage);
			}
			doSend(producer, requestMessage);
			return receiveFromConsumer(consumer, getReceiveTimeout());
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
			if (responseQueue != null) {
				responseQueue.delete();
			}
		}
	}

	/**
	 * {@link #execute(SessionCallback, boolean)}的变体, 显式创建非事务性{@link Session}.
	 * 给定的{@link SessionCallback}不参与现有事务.
	 */
	private <T> T executeLocal(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection con = null;
		Session session = null;
		try {
			con = getConnectionFactory().createConnection();
			session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
			if (startConnection) {
				con.start();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + session);
			}
			return action.doInJms(session);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), startConnection);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//---------------------------------------------------------------------------------------

	@Override
	public <T> T browse(BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browse(defaultQueue, action);
		}
		else {
			return browse(getRequiredDefaultDestinationName(), action);
		}
	}

	@Override
	public <T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queue, null, action);
	}

	@Override
	public <T> T browse(String queueName, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queueName, null, action);
	}

	@Override
	public <T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browseSelected(defaultQueue, messageSelector, action);
		}
		else {
			return browseSelected(getRequiredDefaultDestinationName(), messageSelector, action);
		}
	}

	@Override
	public <T> T browseSelected(final Queue queue, final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				QueueBrowser browser = createBrowser(session, queue, messageSelector);
				try {
					return action.doInJms(session, browser);
				}
				finally {
					JmsUtils.closeQueueBrowser(browser);
				}
			}
		}, true);
	}

	@Override
	public <T> T browseSelected(final String queueName, final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				Queue queue = (Queue) getDestinationResolver().resolveDestinationName(session, queueName, false);
				QueueBrowser browser = createBrowser(session, queue, messageSelector);
				try {
					return action.doInJms(session, browser);
				}
				finally {
					JmsUtils.closeQueueBrowser(browser);
				}
			}
		}, true);
	}


	/**
	 * 从给定的JmsResourceHolder获取适当的连接.
	 * <p>此实现接受JMS 1.1 Connection.
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
	 * <p>此实现接受JMS 1.1 Session.
	 * 
	 * @param holder the JmsResourceHolder
	 * 
	 * @return 从持有者获取的适当Session, 或{@code null}
	 */
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession();
	}

	/**
	 * 检查给定的Session是否是本地事务的, 即它的事务是由该监听器容器的会话处理管理, 而不是由外部事务协调器管理.
	 * <p>Note: 会话自己的事务标志之前已经过检查.
	 * 此方法用于查明会话的事务是本地, 还是外部协调.
	 * 
	 * @param session 要检查的Session
	 * 
	 * @return 给定的Session是否是本地事务的
	 */
	protected boolean isSessionLocallyTransacted(Session session) {
		return isSessionTransacted() &&
				!ConnectionFactoryUtils.isSessionTransactional(session, getConnectionFactory());
	}

	/**
	 * 为给定的Session和Destination创建JMS MessageProducer, 将其配置为禁用消息ID和/或时间戳.
	 * <p>委托给{@link #doCreateProducer}创建原始JMS MessageProducer.
	 * 
	 * @param session 用于创建MessageProducer的JMS Session
	 * @param destination 用于为其创建MessageProducer的JMS Destination
	 * 
	 * @return 新的JMS MessageProducer
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected MessageProducer createProducer(Session session, Destination destination) throws JMSException {
		MessageProducer producer = doCreateProducer(session, destination);
		if (!isMessageIdEnabled()) {
			producer.setDisableMessageID(true);
		}
		if (!isMessageTimestampEnabled()) {
			producer.setDisableMessageTimestamp(true);
		}
		return producer;
	}

	/**
	 * 为给定的Session和Destination创建原始JMS MessageProducer.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param session 用于创建MessageProducer的JMS Session
	 * @param destination 用于创建MessageProducer的JMS Destination
	 * 
	 * @return 新的JMS MessageProducer
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected MessageProducer doCreateProducer(Session session, Destination destination) throws JMSException {
		return session.createProducer(destination);
	}

	/**
	 * 为给定的Session和Destination创建JMS MessageConsumer.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param session 用于创建MessageConsumer的JMS Session
	 * @param destination 用于创建MessageConsumer的JMS Destination
	 * @param messageSelector 此消费者的消息选择器 (can be {@code null})
	 * 
	 * @return 新的JMS MessageConsumer
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination, String messageSelector)
			throws JMSException {

		// 仅在Topic的情况下传递NoLocal标志:
		// 某些JMS提供者, 如 WebSphere MQ 6.0, 在为Queue指定NoLocal标志的情况下抛出IllegalStateException.
		if (isPubSubDomain()) {
			return session.createConsumer(destination, messageSelector, isPubSubNoLocal());
		}
		else {
			return session.createConsumer(destination, messageSelector);
		}
	}

	/**
	 * 为给定的Session和Destination创建JMS MessageProducer, 将其配置为禁用消息ID和/或时间戳.
	 * <p>委托给{@link #doCreateProducer}创建原始JMS MessageProducer.
	 * 
	 * @param session 用于创建QueueBrowser的JMS Session
	 * @param queue 用于创建QueueBrowser的JMS Queue
	 * @param messageSelector 此消费者的消息选择器 (can be {@code null})
	 * 
	 * @return 新的JMS QueueBrowser
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected QueueBrowser createBrowser(Session session, Queue queue, String messageSelector)
			throws JMSException {

		return session.createBrowser(queue, messageSelector);
	}


	/**
	 * ResourceFactory实现, 它委托给此模板的受保护的回调方法.
	 */
	private class JmsTemplateResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		public Connection getConnection(JmsResourceHolder holder) {
			return JmsTemplate.this.getConnection(holder);
		}

		@Override
		public Session getSession(JmsResourceHolder holder) {
			return JmsTemplate.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			return JmsTemplate.this.createConnection();
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return JmsTemplate.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return JmsTemplate.this.isSessionTransacted();
		}
	}
}
