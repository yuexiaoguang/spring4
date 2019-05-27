package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.Topic;
import javax.jms.TopicPublisher;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JMS MessageProducer装饰器, 它调用下面的共享MessageProducer实例, 在装饰器内本地管理QoS设置.
 */
class CachedMessageProducer implements MessageProducer, QueueSender, TopicPublisher {

	// Various JMS 2.0 MessageProducer methods, if available

	private static final Method setDeliveryDelayMethod =
			ClassUtils.getMethodIfAvailable(MessageProducer.class, "setDeliveryDelay", long.class);

	private static final Method getDeliveryDelayMethod =
			ClassUtils.getMethodIfAvailable(MessageProducer.class, "getDeliveryDelay");

	private static Class<?> completionListenerClass;

	private static Method sendWithCompletionListenerMethod;

	private static Method sendWithDestinationAndCompletionListenerMethod;

	static {
		try {
			completionListenerClass = ClassUtils.forName(
					"javax.jms.CompletionListener", CachedMessageProducer.class.getClassLoader());
			sendWithCompletionListenerMethod = MessageProducer.class.getMethod(
					"send", Message.class, int.class, int.class, long.class, completionListenerClass);
			sendWithDestinationAndCompletionListenerMethod = MessageProducer.class.getMethod(
					"send", Destination.class, Message.class, int.class, int.class, long.class, completionListenerClass);
		}
		catch (Exception ex) {
			// No JMS 2.0 API available
			completionListenerClass = null;
		}
	}


	private final MessageProducer target;

	private Boolean originalDisableMessageID;

	private Boolean originalDisableMessageTimestamp;

	private Long originalDeliveryDelay;

	private int deliveryMode;

	private int priority;

	private long timeToLive;


	public CachedMessageProducer(MessageProducer target) throws JMSException {
		this.target = target;
		this.deliveryMode = target.getDeliveryMode();
		this.priority = target.getPriority();
		this.timeToLive = target.getTimeToLive();
	}


	@Override
	public void setDisableMessageID(boolean disableMessageID) throws JMSException {
		if (this.originalDisableMessageID == null) {
			this.originalDisableMessageID = this.target.getDisableMessageID();
		}
		this.target.setDisableMessageID(disableMessageID);
	}

	@Override
	public boolean getDisableMessageID() throws JMSException {
		return this.target.getDisableMessageID();
	}

	@Override
	public void setDisableMessageTimestamp(boolean disableMessageTimestamp) throws JMSException {
		if (this.originalDisableMessageTimestamp == null) {
			this.originalDisableMessageTimestamp = this.target.getDisableMessageTimestamp();
		}
		this.target.setDisableMessageTimestamp(disableMessageTimestamp);
	}

	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		return this.target.getDisableMessageTimestamp();
	}

	public void setDeliveryDelay(long deliveryDelay) {
		if (this.originalDeliveryDelay == null) {
			this.originalDeliveryDelay = (Long) ReflectionUtils.invokeMethod(getDeliveryDelayMethod, this.target);
		}
		ReflectionUtils.invokeMethod(setDeliveryDelayMethod, this.target, deliveryDelay);
	}

	public long getDeliveryDelay() {
		return (Long) ReflectionUtils.invokeMethod(getDeliveryDelayMethod, this.target);
	}

	@Override
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	@Override
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	@Override
	public long getTimeToLive() {
		return this.timeToLive;
	}

	@Override
	public Destination getDestination() throws JMSException {
		return this.target.getDestination();
	}

	@Override
	public Queue getQueue() throws JMSException {
		return (Queue) this.target.getDestination();
	}

	@Override
	public Topic getTopic() throws JMSException {
		return (Topic) this.target.getDestination();
	}

	@Override
	public void send(Message message) throws JMSException {
		this.target.send(message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.target.send(message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void send(Destination destination, Message message) throws JMSException {
		this.target.send(destination, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.target.send(destination, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void send(Queue queue, Message message) throws JMSException {
		this.target.send(queue, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void send(Queue queue, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.target.send(queue, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void publish(Message message) throws JMSException {
		this.target.send(message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void publish(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.target.send(message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void publish(Topic topic, Message message) throws JMSException {
		this.target.send(topic, message, this.deliveryMode, this.priority, this.timeToLive);
	}

	@Override
	public void publish(Topic topic, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
		this.target.send(topic, message, deliveryMode, priority, timeToLive);
	}

	@Override
	public void close() throws JMSException {
		// It's a cached MessageProducer... reset properties only.
		if (this.originalDisableMessageID != null) {
			this.target.setDisableMessageID(this.originalDisableMessageID);
			this.originalDisableMessageID = null;
		}
		if (this.originalDisableMessageTimestamp != null) {
			this.target.setDisableMessageTimestamp(this.originalDisableMessageTimestamp);
			this.originalDisableMessageTimestamp = null;
		}
		if (this.originalDeliveryDelay != null) {
			ReflectionUtils.invokeMethod(setDeliveryDelayMethod, this.target, this.originalDeliveryDelay);
			this.originalDeliveryDelay = null;
		}
	}

	@Override
	public String toString() {
		return "Cached JMS MessageProducer: " + this.target;
	}


	/**
	 * 如有必要, 构建一个动态代理, 以反射性地适配JMS 2.0 API方法.
	 * 否则只需返回此 CachedMessageProducer实例本身.
	 */
	public MessageProducer getProxyIfNecessary() {
		if (completionListenerClass != null) {
			return (MessageProducer) Proxy.newProxyInstance(CachedMessageProducer.class.getClassLoader(),
					new Class<?>[] {MessageProducer.class, QueueSender.class, TopicPublisher.class},
					new Jms2MessageProducerInvocationHandler());
		}
		else {
			return this;
		}
	}


	/**
	 * 反射式的InvocationHandler, 它适用于无法静态编译的 JMS 2.0 API方法, 同时保留了JMS 1.1的兼容性
	 * (由于签名中新的{@code javax.jms.CompletionListener}类型).
	 */
	private class Jms2MessageProducerInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("send") && args != null &&
						completionListenerClass == method.getParameterTypes()[args.length - 1]) {
					switch (args.length) {
						case 2: // send(message, completionListener)
							return sendWithCompletionListenerMethod.invoke(
									target, args[0], deliveryMode, priority, timeToLive, args[1]);
						case 3: // send(destination, message, completionListener)
							return sendWithDestinationAndCompletionListenerMethod.invoke(
									target, args[0], args[1], deliveryMode, priority, timeToLive, args[2]);
						case 5: // send(message, deliveryMode, priority, timeToLive, completionListener)
							return sendWithCompletionListenerMethod.invoke(target, args);
						case 6: // send(destination, message, deliveryMode, priority, timeToLive, completionListener)
							return sendWithDestinationAndCompletionListenerMethod.invoke(target, args);
					}
				}
				return method.invoke(CachedMessageProducer.this, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
