package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link javax.jms.ConnectionFactory}实现, 它将所有调用委托给给定目标{@link javax.jms.ConnectionFactory},
 * 在必要时适配特定的{@code create(Queue/Topic)Connection}调用到目标ConnectionFactory
 * (e.g. 针对通用JMS 1.1 ConnectionFactory运行基于JMS 1.0.2 API的代码时, 例如ActiveMQ的PooledConnectionFactory).
 *
 * <p>此类允许进行子类化, 子类仅覆盖那些不应简单地委托给目标ConnectionFactory的方法 (例如{@link #createConnection()}).
 *
 * <p>也可以定义为, 包装特定的目标ConnectionFactory,
 * 使用"shouldStopConnections"标志以指示从目标工厂获取的Connection是否应该在关闭之前停止.
 * 对于某些连接池而言, 后者可能是必需的, 这些连接池只是将释放的连接返回到池中, 而不是在它们位于池中时停止它们.
 */
public class DelegatingConnectionFactory
		implements SmartConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, InitializingBean {

	private ConnectionFactory targetConnectionFactory;

	private boolean shouldStopConnections = false;


	/**
	 * 设置此ConnectionFactory应委托给的目标ConnectionFactory.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "'targetConnectionFactory' must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此ConnectionFactory委托给的目标ConnectionFactory.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * 指示从目标工厂获得的连接是否应在关闭之前停止 ("true") 或简单地关闭 ("false").
	 * 某些连接池可能需要额外的停止调用, 这些连接池只是将已释放的连接返回到池中, 而不是在它们位于池中时停止它们.
	 * <p>默认"false", 只需关闭Connection.
	 */
	public void setShouldStopConnections(boolean shouldStopConnections) {
		this.shouldStopConnections = shouldStopConnections;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection createConnection() throws JMSException {
		return getTargetConnectionFactory().createConnection();
	}

	@Override
	public Connection createConnection(String username, String password) throws JMSException {
		return getTargetConnectionFactory().createConnection(username, password);
	}

	@Override
	public QueueConnection createQueueConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		else {
			Connection con = cf.createConnection();
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	@Override
	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection(username, password);
		}
		else {
			Connection con = cf.createConnection(username, password);
			if (!(con instanceof QueueConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a QueueConnectionFactory");
			}
			return (QueueConnection) con;
		}
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection();
		}
		else {
			Connection con = cf.createConnection();
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
	}

	@Override
	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		ConnectionFactory cf = getTargetConnectionFactory();
		if (cf instanceof TopicConnectionFactory) {
			return ((TopicConnectionFactory) cf).createTopicConnection(username, password);
		}
		else {
			Connection con = cf.createConnection(username, password);
			if (!(con instanceof TopicConnection)) {
				throw new javax.jms.IllegalStateException("'targetConnectionFactory' is not a TopicConnectionFactory");
			}
			return (TopicConnection) con;
		}
	}

	@Override
	public boolean shouldStop(Connection con) {
		return this.shouldStopConnections;
	}

}
