package org.springframework.jms.remoting;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * 用于访问基于JMS的远程服务的{@link org.aopalliance.intercept.MethodInterceptor}.
 *
 * <p>序列化远程调用对象, 并反序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但使用JMS提供者作为通信基础结构.
 *
 * <p>要配置{@link javax.jms.QueueConnectionFactory}和目标队列 (作为{@link javax.jms.Queue}引用或作为队列名称).
 *
 * <p>Thanks to James Strachan for the original prototype that this JMS invoker mechanism was inspired by!
 */
public class JmsInvokerClientInterceptor implements MethodInterceptor, InitializingBean {

	private ConnectionFactory connectionFactory;

	private Object queue;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private long receiveTimeout = 0;


	/**
	 * 设置用于获取JMS QueueConnection的QueueConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回用于获取JMS QueueConnection的QueueConnectionFactory.
	 */
	protected ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 设置调用者请求发送到的目标队列.
	 */
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/**
	 * 设置调用者请求发送到的目标队列的名称.
	 * <p>指定的名称将通过{@link #setDestinationResolver DestinationResolver}动态解析.
	 */
	public void setQueueName(String queueName) {
		this.queue = queueName;
	}

	/**
	 * 设置要用于解析此访问者的Queue引用的DestinationResolver.
	 * <p>默认解析器是{@code DynamicDestinationResolver}.
	 * 指定将目标名称解析为JNDI位置的{@code JndiDestinationResolver}.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver =
				(destinationResolver != null ? destinationResolver : new DynamicDestinationResolver());
	}

	/**
	 * 设置用于此访问者的{@link RemoteInvocationFactory}.
	 * <p>默认是{@link DefaultRemoteInvocationFactory}.
	 * <p>自定义调用工厂可以向调用添加更多上下文信息, 例如用户凭据.
	 */
	public void setRemoteInvocationFactory(RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}

	/**
	 * 指定{@link MessageConverter}, 用于将{@link org.springframework.remoting.support.RemoteInvocation}对象转换为请求消息,
	 * 以及将响应消息转换为{@link org.springframework.remoting.support.RemoteInvocationResult}对象.
	 * <p>默认为{@link SimpleMessageConverter}, 为每个调用/调用结果对象使用标准JMS {@link javax.jms.ObjectMessage}.
	 * <p>自定义实现通常可以将{@link java.io.Serializable}对象适配为特殊类型的消息,
	 * 或者可能专门用于将{@code RemoteInvocation(Result)}转换为特定类型的消息.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * 设置用于接收请求的响应消息的超时 (以毫秒为单位).
	 * <p>默认值为 0, 表示没有超时的阻塞接收.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * 返回用于接收请求的响应消息的超时 (以毫秒为单位).
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.queue == null) {
			throw new IllegalArgumentException("'queue' or 'queueName' is required");
		}
	}


	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JMS invoker proxy for queue [" + this.queue + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;
		try {
			result = executeRequest(invocation);
		}
		catch (JMSException ex) {
			throw convertJmsInvokerAccessException(ex);
		}
		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in JMS invoker remote service at queue [" + this.queue + "]", ex);
			}
		}
	}

	/**
	 * 为给定的AOP方法调用创建一个新的{@code RemoteInvocation}对象.
	 * <p>默认实现委托给{@link RemoteInvocationFactory}.
	 * <p>可以在子类中重写以提供自定义{@code RemoteInvocation}子类, 其中包含其他调用参数, 如用户凭据.
	 * 请注意, 最好使用自定义{@code RemoteInvocationFactory}, 这是一种可重用的策略.
	 * 
	 * @param methodInvocation 当前的AOP方法调用
	 * 
	 * @return the RemoteInvocation object
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return this.remoteInvocationFactory.createRemoteInvocation(methodInvocation);
	}

	/**
	 * 执行给定的远程调用, 向此访问者的目标队列发送调用者请求消息, 并等待相应的响应.
	 * 
	 * @param invocation 要执行的RemoteInvocation
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws JMSException 如果JMS失败
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws JMSException {
		Connection con = createConnection();
		Session session = null;
		try {
			session = createSession(con);
			Queue queueToUse = resolveQueue(session);
			Message requestMessage = createRequestMessage(session, invocation);
			con.start();
			Message responseMessage = doExecuteRequest(session, queueToUse, requestMessage);
			if (responseMessage != null) {
				return extractInvocationResult(responseMessage);
			}
			else {
				return onReceiveTimeout(invocation);
			}
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), true);
		}
	}

	/**
	 * 为此JMS调用者创建新的JMS连接.
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	/**
	 * 为此JMS调用者创建一个新的JMS会话.
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	/**
	 * 解析此访问者的目标队列.
	 * 
	 * @param session 当前的JMS会话
	 * 
	 * @return 已解析的目标队列
	 * @throws JMSException 如果解析失败
	 */
	protected Queue resolveQueue(Session session) throws JMSException {
		if (this.queue instanceof Queue) {
			return (Queue) this.queue;
		}
		else if (this.queue instanceof String) {
			return resolveQueueName(session, (String) this.queue);
		}
		else {
			throw new javax.jms.IllegalStateException(
					"Queue object [" + this.queue + "] is neither a [javax.jms.Queue] nor a queue name String");
		}
	}

	/**
	 * 通过此访问者的{@link DestinationResolver}将给定的队列名称解析为JMS {@link javax.jms.Queue}.
	 * 
	 * @param session 当前JMS Session
	 * @param queueName 队列名称
	 * 
	 * @return 找到的Queue
	 * @throws JMSException 如果解析失败
	 */
	protected Queue resolveQueueName(Session session, String queueName) throws JMSException {
		return (Queue) this.destinationResolver.resolveDestinationName(session, queueName, false);
	}

	/**
	 * 创建调用者请求消息.
	 * <p>默认实现为给定的RemoteInvocation对象创建JMS {@link javax.jms.ObjectMessage}.
	 * 
	 * @param session 当前JMS Session
	 * @param invocation 要发送的远程调用
	 * 
	 * @return 要发送的JMS消息
	 * @throws JMSException 如果无法创建消息
	 */
	protected Message createRequestMessage(Session session, RemoteInvocation invocation) throws JMSException {
		return this.messageConverter.toMessage(invocation, session);
	}

	/**
	 * 实际执行给定的请求, 将调用者请求消息发送到指定的目标队列, 并等待相应的响应.
	 * <p>默认实现基于标准JMS发送/接收, 使用{@link javax.jms.TemporaryQueue}接收响应.
	 * 
	 * @param session 要使用的JMS Session
	 * @param queue 要发送到的已解析的目标队列
	 * @param requestMessage 要发送的JMS消息
	 * 
	 * @return the RemoteInvocationResult object
	 * @throws JMSException 如果JMS失败
	 */
	protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(queue);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			producer.send(requestMessage);
			long timeout = getReceiveTimeout();
			return (timeout > 0 ? consumer.receive(timeout) : consumer.receive());
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
	 * 从响应消息中提取调用结果.
	 * <p>默认实现需要携带{@link RemoteInvocationResult}对象的JMS {@link javax.jms.ObjectMessage}.
	 * 如果遇到无效的响应消息, 则会调用{@code onInvalidResponse}回调.
	 * 
	 * @param responseMessage 响应消息
	 * 
	 * @return 调用结果
	 * @throws JMSException 如果发生JMS异常
	 */
	protected RemoteInvocationResult extractInvocationResult(Message responseMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(responseMessage);
		if (content instanceof RemoteInvocationResult) {
			return (RemoteInvocationResult) content;
		}
		return onInvalidResponse(responseMessage);
	}

	/**
	 * 当指定的{@link RemoteInvocation}的接收超时已到期时, {@link #executeRequest}调用的回调.
	 * <p>默认抛出{@link RemoteTimeoutException}.
	 * 子类可以选择抛出更专用的异常, 甚至可以返回默认的{@link RemoteInvocationResult}作为后备.
	 * 
	 * @param invocation 调用
	 * 
	 * @return 接收超时到期时的默认结果
	 */
	protected RemoteInvocationResult onReceiveTimeout(RemoteInvocation invocation) {
		throw new RemoteTimeoutException("Receive timeout after " + this.receiveTimeout + " ms for " + invocation);
	}

	/**
	 * {@link #extractInvocationResult}在遇到无效的响应消息时调用的回调.
	 * <p>默认抛出{@link MessageFormatException}.
	 * 
	 * @param responseMessage 无效的响应消息
	 * 
	 * @return 应该返回给调用者的替代调用结果
	 * @throws JMSException 如果无效的响应应导致基础结构异常传播到调用者
	 */
	protected RemoteInvocationResult onInvalidResponse(Message responseMessage) throws JMSException {
		throw new MessageFormatException("Invalid response message: " + responseMessage);
	}

	/**
	 * 重新创建给定{@link RemoteInvocationResult}对象中包含的调用结果.
	 * <p>默认实现调用默认的{@code recreate()}方法.
	 * <p>可以在子类中重写以提供自定义重新创建, 从而可能处理返回的结果对象.
	 * 
	 * @param result 要重新创建的RemoteInvocationResult
	 * 
	 * @return 如果成功返回调用结果的返回值
	 * @throws Throwable 如果调用结果是异常
	 */
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}

	/**
	 * 将给定的JMS调用者访问异常转换为适当的Spring {@link RemoteAccessException}.
	 * 
	 * @param ex 要转换的异常
	 * 
	 * @return 要抛出的 RemoteAccessException
	 */
	protected RemoteAccessException convertJmsInvokerAccessException(JMSException ex) {
		return new RemoteAccessException("Could not access JMS invoker queue [" + this.queue + "]", ex);
	}

}
