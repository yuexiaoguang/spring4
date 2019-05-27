package org.springframework.jms.remoting;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * JMS消息监听器, 它将指定的服务bean导出为JMS服务端点, 可通过JMS调用器代理访问.
 *
 * <p>请注意, 此类实现了Spring的{@link org.springframework.jms.listener.SessionAwareMessageListener}接口,
 * 因为它需要访问活动的JMS会话.
 * 因此, 此类只能与支持SessionAwareMessageListener接口的消息监听器容器一起使用
 * (e.g. Spring的 {@link org.springframework.jms.listener.DefaultMessageListenerContainer}).
 *
 * <p>Thanks to James Strachan for the original prototype that this JMS invoker mechanism was inspired by!
 */
public class JmsInvokerServiceExporter extends RemoteInvocationBasedExporter
		implements SessionAwareMessageListener<Message>, InitializingBean {

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private boolean ignoreInvalidRequests = true;

	private Object proxy;


	/**
	 * 指定MessageConverter, 用于将请求消息转换为{@link org.springframework.remoting.support.RemoteInvocation}对象,
	 * 以及将{@link org.springframework.remoting.support.RemoteInvocationResult}对象转换为响应消息.
	 * <p>默认为{@link org.springframework.jms.support.converter.SimpleMessageConverter},
	 * 为每个调用/调用结果对象使用标准JMS {@link javax.jms.ObjectMessage}.
	 * <p>自定义实现通常可以将Serializable调整为特殊类型的消息,
	 * 或者可能专门用于将 RemoteInvocation(Result)转换为特定类型的消息.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * 设置是否应丢弃格式无效的消息.
	 * 默认"true".
	 * <p>将此标志切换为"false" 以将异常返回给监听器容器.
	 * 这通常会导致重新传递消息, 这通常是不可取的 - 因为消息内容将是相同的 (即, 仍然无效).
	 */
	public void setIgnoreInvalidRequests(boolean ignoreInvalidRequests) {
		this.ignoreInvalidRequests = ignoreInvalidRequests;
	}

	@Override
	public void afterPropertiesSet() {
		this.proxy = getProxyForService();
	}


	@Override
	public void onMessage(Message requestMessage, Session session) throws JMSException {
		RemoteInvocation invocation = readRemoteInvocation(requestMessage);
		if (invocation != null) {
			RemoteInvocationResult result = invokeAndCreateResult(invocation, this.proxy);
			writeRemoteInvocationResult(requestMessage, session, result);
		}
	}

	/**
	 * 从给定的JMS消息中读取RemoteInvocation.
	 * 
	 * @param requestMessage 当前请求消息
	 * 
	 * @return RemoteInvocation对象 (如果无效消息将被忽略, 则为{@code null})
	 * @throws javax.jms.JMSException 在消息访问失败的情况下
	 */
	protected RemoteInvocation readRemoteInvocation(Message requestMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(requestMessage);
		if (content instanceof RemoteInvocation) {
			return (RemoteInvocation) content;
		}
		return onInvalidRequest(requestMessage);
	}


	/**
	 * 将给定的RemoteInvocationResult作为JMS消息发送给发起者.
	 * 
	 * @param requestMessage 当前请求消息
	 * @param session 要使用的JMS会话
	 * @param result the RemoteInvocationResult object
	 * 
	 * @throws javax.jms.JMSException 如果尝试发送消息时抛出
	 */
	protected void writeRemoteInvocationResult(
			Message requestMessage, Session session, RemoteInvocationResult result) throws JMSException {

		Message response = createResponseMessage(requestMessage, session, result);
		MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
		try {
			producer.send(response);
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * 创建调用结果响应消息.
	 * <p>默认实现为给定的RemoteInvocationResult对象创建JMS ObjectMessage.
	 * 它将响应的相关id设置为请求消息的相关ID; 否则为请求消息ID.
	 * 
	 * @param request 原始请求消息
	 * @param session 要使用的JMS会话
	 * @param result 调用结果
	 * 
	 * @return 要发送的消息响应
	 * @throws javax.jms.JMSException 如果创建消息失败
	 */
	protected Message createResponseMessage(Message request, Session session, RemoteInvocationResult result)
			throws JMSException {

		Message response = this.messageConverter.toMessage(result, session);
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
		return response;
	}

	/**
	 * {@link #readRemoteInvocation}在遇到无效请求消息时调用的回调.
	 * <p>默认实现丢弃无效消息或抛出MessageFormatException - 根据"ignoreInvalidRequests"标志,
	 * 默认设置为"true" (即丢弃无效消息).
	 * 
	 * @param requestMessage 无效的请求消息
	 * 
	 * @return 用于公开无效请求的RemoteInvocation (如果无效消息通常会被忽略, 则通常为{@code null})
	 * @throws javax.jms.JMSException 如果无效请求应该导致异常 (而不是忽略它)
	 */
	protected RemoteInvocation onInvalidRequest(Message requestMessage) throws JMSException {
		if (this.ignoreInvalidRequests) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid request message will be discarded: " + requestMessage);
			}
			return null;
		}
		else {
			throw new MessageFormatException("Invalid request message: " + requestMessage);
		}
	}
}
