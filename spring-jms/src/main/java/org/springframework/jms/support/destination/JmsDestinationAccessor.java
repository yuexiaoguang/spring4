package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.jms.support.JmsAccessor;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.jms.core.JmsTemplate}和其他JMS访问网关的基类,
 * 将与目标相关的属性添加到{@link JmsAccessor JmsAccessor's}常用属性.
 *
 * <p>不打算直接使用.
 * See {@link org.springframework.jms.core.JmsTemplate}.
 */
public abstract class JmsDestinationAccessor extends JmsAccessor {

	/**
	 * 超时值, 指示接收操作应检查消息是否立即可用而没有阻塞.
	 */
	public static final long RECEIVE_TIMEOUT_NO_WAIT = -1;

	/**
	 * 超时值, 表示没有超时的阻塞接收.
	 */
	public static final long RECEIVE_TIMEOUT_INDEFINITE_WAIT = 0;


	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private boolean pubSubDomain = false;


	/**
	 * 设置用于解析此访问器的{@link javax.jms.Destination}引用的{@link DestinationResolver}.
	 * <p>默认解析器是DynamicDestinationResolver. 指定JndiDestinationResolver以将目标名称解析为JNDI位置.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "'destinationResolver' must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回此访问者的DestinationResolver (never {@code null}).
	 */
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 使用所使用的JMS域配置目标访问器.
	 * 默认 Point-to-Point (Queues).
	 * <p>此设置主要指示启用动态目标时要解析的目标类型.
	 * 
	 * @param pubSubDomain "true" 用于Publish/Subscribe 域 ({@link javax.jms.Topic Topics}),
	 * 						"false"用于 Point-to-Point域 ({@link javax.jms.Queue Queues})
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * 返回是否使用Publish/Subscribe 域 ({@link javax.jms.Topic Topics}).
	 * 否则, 使用Point-to-Point域 ({@link javax.jms.Queue Queues}).
	 */
	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}


	/**
	 * 通过此访问器的{@link DestinationResolver}将给定的目标名称解析为JMS {@link Destination}.
	 * 
	 * @param session 当前JMS {@link Session}
	 * @param destinationName 目标名称
	 * 
	 * @return 找到的{@link Destination}
	 * @throws javax.jms.JMSException 如果解析失败
	 */
	protected Destination resolveDestinationName(Session session, String destinationName) throws JMSException {
		return getDestinationResolver().resolveDestinationName(session, destinationName, isPubSubDomain());
	}

	/**
	 * 实际接收来自给定消费者的消息.
	 * 
	 * @param consumer 要接收的JMS MessageConsumer
	 * @param timeout 接收超时 (负值表示无等待接收; 0 表示无限期等待)
	 * 
	 * @return 接受到的JMS Message, 或{@code null}
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Message receiveFromConsumer(MessageConsumer consumer, long timeout) throws JMSException {
		if (timeout > 0) {
			return consumer.receive(timeout);
		}
		else if (timeout < 0) {
			return consumer.receiveNoWait();
		}
		else {
			return consumer.receive();
		}
	}

}
