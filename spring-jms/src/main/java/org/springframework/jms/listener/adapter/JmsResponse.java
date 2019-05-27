package org.springframework.jms.listener.adapter;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.Assert;

/**
 * 返回任何JMS监听器方法的类型, 用于指示响应本身的实际响应目标.
 * 通常在需要在运行中计算目标时使用.
 *
 * <p>下面的示例向{@code queueOut Queue}发送带有{@code result}参数内容的响应:
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *     &#064;JmsListener
 *     public JmsResponse process(String msg) {
 *         // process incoming message
 *         return JmsResponse.forQueue(result, "queueOut");
 *     }
 * }</pre>
 *
 * 如果目标不需要在运行时计算, {@link org.springframework.messaging.handler.annotation.SendTo @SendTo}是推荐的声明方法.
 *
 * @param <T> 响应的类型
 */
public class JmsResponse<T> {

	private final T response;

	private final Object destination;


	/**
	 * @param response 结果的内容
	 * @param destination 目标
	 */
	protected JmsResponse(T response, Object destination) {
		Assert.notNull(response, "Result must not be null");
		this.response = response;
		this.destination = destination;
	}


	/**
	 * 返回响应的内容.
	 */
	public T getResponse() {
		return this.response;
	}

	/**
	 * 解析用于此实例的{@link Destination}.
	 * {@link DestinationResolver}和{@link Session}可用于在运行时解析目标.
	 * 
	 * @param destinationResolver 必要时使用的目标解析器
	 * @param session 必要时使用的会话
	 * 
	 * @return 要使用的{@link Destination}
	 * @throws JMSException 如果DestinationResolver无法解析目标
	 */
	public Destination resolveDestination(DestinationResolver destinationResolver, Session session)
			throws JMSException {

		if (this.destination instanceof Destination) {
			return (Destination) this.destination;
		}
		if (this.destination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.destination;
			return destinationResolver.resolveDestinationName(session,
					nameHolder.destinationName, nameHolder.pubSubDomain);
		}
		return null;
	}

	@Override
	public String toString() {
		return "JmsResponse [" + "response=" + this.response + ", destination=" + this.destination + ']';
	}


	/**
	 * 创建一个{@link JmsResponse}, 使用指定的名称定位队列.
	 */
	public static <T> JmsResponse<T> forQueue(T result, String queueName) {
		Assert.notNull(queueName, "Queue name must not be null");
		return new JmsResponse<T>(result, new DestinationNameHolder(queueName, false));
	}

	/**
	 * 创建一个{@link JmsResponse}, 使用指定的名称定位topic.
	 */
	public static <T> JmsResponse<T> forTopic(T result, String topicName) {
		Assert.notNull(topicName, "Topic name must not be null");
		return new JmsResponse<T>(result, new DestinationNameHolder(topicName, true));
	}

	/**
	 * 创建一个定位到指定{@link Destination}的{@link JmsResponse}.
	 */
	public static <T> JmsResponse<T> forDestination(T result, Destination destination) {
		Assert.notNull(destination, "Destination must not be null");
		return new JmsResponse<T>(result, destination);
	}


	/**
	 * 组合目标名称及其目标目标类型 (queue or topic)的内部类.
	 */
	private static class DestinationNameHolder {

		private final String destinationName;

		private final boolean pubSubDomain;

		public DestinationNameHolder(String destinationName, boolean pubSubDomain) {
			this.destinationName = destinationName;
			this.pubSubDomain = pubSubDomain;
		}

		@Override
		public String toString() {
			return this.destinationName + "{" + "pubSubDomain=" + this.pubSubDomain + '}';
		}
	}

}
