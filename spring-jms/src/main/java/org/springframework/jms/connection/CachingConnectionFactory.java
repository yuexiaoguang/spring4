package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicSession;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link SingleConnectionFactory}子类, 它添加了{@link javax.jms.Session}缓存以及{@link javax.jms.MessageProducer}缓存.
 * 默认情况下，此ConnectionFactory还将{@link #setReconnectOnException "reconnectOnException" property}切换为"true",
 * 允许自动恢复底层Connection.
 *
 * <p>默认情况下, 只会缓存一个会话, 并根据需要创建和处理进一步请求的会话.
 * 在高并发环境的情况下, 考虑提升{@link #setSessionCacheSize "sessionCacheSize"值}.
 *
 * <p>使用JMS 1.0.2 API时, 此ConnectionFactory将根据运行时使用的JMS API方法切换到 queue/topic模式:
 * {@code createQueueConnection}和{@code createTopicConnection}将分别导致 queue/topic 模式;
 * 通用{@code createConnection}调用将导致JJMS 1.1连接, 该连接能够为两种模式提供服务.
 *
 * <p><b>NOTE: 此ConnectionFactory要求显式关闭从其共享Connection获取的所有Session.</b>
 * 无论如何, 这是本机JMS访问代码的通常建议.
 * 但是, 使用此 ConnectionFactory, 必须使用它才能实际允许会话重用.
 *
 * <p>另请注意, 在最终从池中删除Session之前, 从缓存的Session获取的MessageConsumers不会关闭.
 * 在某些情况下, 这可能会导致语义副作用. 对于持久订阅者, 逻辑{@code Session.close()}调用也将关闭订阅.
 * 不支持在同一个Session句柄上为同一订阅重新注册持久消费者; 首先关闭并重新获得缓存的会话.
 */
public class CachingConnectionFactory extends SingleConnectionFactory {

	/** JMS 2.0 Session.createSharedConsumer方法 */
	private static final Method createSharedConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedConsumer", Topic.class, String.class, String.class);

	/** JMS 2.0 Session.createSharedDurableConsumer方法 */
	private static final Method createSharedDurableConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedDurableConsumer", Topic.class, String.class, String.class);


	private int sessionCacheSize = 1;

	private boolean cacheProducers = true;

	private boolean cacheConsumers = true;

	private volatile boolean active = true;

	private final Map<Integer, LinkedList<Session>> cachedSessions =
			new HashMap<Integer, LinkedList<Session>>();


	public CachingConnectionFactory() {
		super();
		setReconnectOnException(true);
	}

	/**
	 * @param targetConnectionFactory 目标ConnectionFactory
	 */
	public CachingConnectionFactory(ConnectionFactory targetConnectionFactory) {
		super(targetConnectionFactory);
		setReconnectOnException(true);
	}


	/**
	 * 指定JMS会话缓存所需的大小 (根据JMS会话类型).
	 * <p>此缓存大小是缓存 Session数的最大限制, 根据会话的类型 (auto, client, dups_ok, transacted).
	 * 因此, 缓存的Session的实际数量可能高达指定值的四倍 - 不太可能混合和匹配不同的确认类型.
	 * <p>默认 1: 缓存单个Session, (重新)按需创建更多会话.
	 * 如果提高缓存的会话数, 指定一个10之类的数字; 也就是说, 1对于低并发场景可能就足够了.
	 */
	public void setSessionCacheSize(int sessionCacheSize) {
		Assert.isTrue(sessionCacheSize >= 1, "Session cache size must be 1 or higher");
		this.sessionCacheSize = sessionCacheSize;
	}

	/**
	 * 返回JMS会话缓存所需的大小 (根据JMS会话类型).
	 */
	public int getSessionCacheSize() {
		return this.sessionCacheSize;
	}

	/**
	 * 指定是否为每个JMS会话实例缓存JMS MessageProducer
	 * (更具体地说: 每个Destination 和 Session一个MessageProducer).
	 * <p>默认"true". 将其切换为"false", 以便始终按需重新创建MessageProducer.
	 */
	public void setCacheProducers(boolean cacheProducers) {
		this.cacheProducers = cacheProducers;
	}

	/**
	 * 返回是否为每个JMS会话实例缓存JMS MessageProducer.
	 */
	public boolean isCacheProducers() {
		return this.cacheProducers;
	}

	/**
	 * 指定是否为每个JMS会话实例缓存JMS MessageConsumer
	 * (更具体地说: 每个Destinationselector String 和 Session一个MessageConsumer).
	 * 请注意, 在Session句柄的逻辑关闭之前会缓存持久订阅者.
	 * <p>默认"true". 将其切换为"false", 以便始终按需重新创建MessageConsumer.
	 */
	public void setCacheConsumers(boolean cacheConsumers) {
		this.cacheConsumers = cacheConsumers;
	}

	/**
	 * 返回是否为每个JMS会话实例缓存JMS MessageConsumer.
	 */
	public boolean isCacheConsumers() {
		return this.cacheConsumers;
	}


	/**
	 * 同样重置Session缓存.
	 */
	@Override
	public void resetConnection() {
		this.active = false;

		synchronized (this.cachedSessions) {
			for (LinkedList<Session> sessionList : this.cachedSessions.values()) {
				synchronized (sessionList) {
					for (Session session : sessionList) {
						try {
							session.close();
						}
						catch (Throwable ex) {
							logger.trace("Could not close cached JMS Session", ex);
						}
					}
				}
			}
			this.cachedSessions.clear();
		}

		// 现在继续实际关闭共享Connection...
		super.resetConnection();

		this.active = true;
	}

	/**
	 * 检查给定模式的缓存会话.
	 */
	@Override
	protected Session getSession(Connection con, Integer mode) throws JMSException {
		if (!this.active) {
			return null;
		}

		LinkedList<Session> sessionList;
		synchronized (this.cachedSessions) {
			sessionList = this.cachedSessions.get(mode);
			if (sessionList == null) {
				sessionList = new LinkedList<Session>();
				this.cachedSessions.put(mode, sessionList);
			}
		}
		Session session = null;
		synchronized (sessionList) {
			if (!sessionList.isEmpty()) {
				session = sessionList.removeFirst();
			}
		}
		if (session != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found cached JMS Session for mode " + mode + ": " +
						(session instanceof SessionProxy ? ((SessionProxy) session).getTargetSession() : session));
			}
		}
		else {
			Session targetSession = createSession(con, mode);
			if (logger.isDebugEnabled()) {
				logger.debug("Registering cached JMS Session for mode " + mode + ": " + targetSession);
			}
			session = getCachedSessionProxy(targetSession, sessionList);
		}
		return session;
	}

	/**
	 * 使用代理来包装给定的Session, 该代理将每个方法调用委托给它, 但是会调整close调用.
	 * 这对于允许应用程序代码像普通Session一样处理特殊框架Session非常有用.
	 * 
	 * @param target 要包装的原始Session
	 * @param sessionList 给定会话所属的缓存会话列表
	 * 
	 * @return 包装的Session
	 */
	protected Session getCachedSessionProxy(Session target, LinkedList<Session> sessionList) {
		List<Class<?>> classes = new ArrayList<Class<?>>(3);
		classes.add(SessionProxy.class);
		if (target instanceof QueueSession) {
			classes.add(QueueSession.class);
		}
		if (target instanceof TopicSession) {
			classes.add(TopicSession.class);
		}
		return (Session) Proxy.newProxyInstance(SessionProxy.class.getClassLoader(),
				ClassUtils.toClassArray(classes), new CachedSessionInvocationHandler(target, sessionList));
	}


	/**
	 * 缓存的JMS会话代理的调用处理器.
	 */
	private class CachedSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		private final LinkedList<Session> sessionList;

		private final Map<DestinationCacheKey, MessageProducer> cachedProducers =
				new HashMap<DestinationCacheKey, MessageProducer>();

		private final Map<ConsumerCacheKey, MessageConsumer> cachedConsumers =
				new HashMap<ConsumerCacheKey, MessageConsumer>();

		private boolean transactionOpen = false;

		public CachedSessionInvocationHandler(Session target, LinkedList<Session> sessionList) {
			this.target = target;
			this.sessionList = sessionList;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if (methodName.equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (methodName.equals("hashCode")) {
				// Use hashCode of Session proxy.
				return System.identityHashCode(proxy);
			}
			else if (methodName.equals("toString")) {
				return "Cached JMS Session: " + this.target;
			}
			else if (methodName.equals("close")) {
				// 处理close方法: 不要通过调用.
				if (active) {
					synchronized (this.sessionList) {
						if (this.sessionList.size() < getSessionCacheSize()) {
							try {
								logicalClose((Session) proxy);
								// 在会话列表中保持打开状态.
								return null;
							}
							catch (JMSException ex) {
								logger.trace("Logical close of cached JMS Session failed - discarding it", ex);
								// 从这里开始接近物理...
							}
						}
					}
				}
				// 如果到这里, 应该关闭.
				physicalClose();
				return null;
			}
			else if (methodName.equals("getTargetSession")) {
				// 处理getTargetSession方法: 返回底层Session.
				return this.target;
			}
			else if (methodName.equals("commit") || methodName.equals("rollback")) {
				this.transactionOpen = false;
			}
			else if (methodName.startsWith("create")) {
				this.transactionOpen = true;
				if (isCacheProducers() && (methodName.equals("createProducer") ||
						methodName.equals("createSender") || methodName.equals("createPublisher"))) {
					// 对于生产者, Destination参数为null是可以的
					Destination dest = (Destination) args[0];
					if (!(dest instanceof TemporaryQueue || dest instanceof TemporaryTopic)) {
						return getCachedProducer(dest);
					}
				}
				else if (isCacheConsumers()) {
					// 如果Destination (i.e. args[0]) 为 null, 则让原始JMS调用抛出异常
					if ((methodName.equals("createConsumer") || methodName.equals("createReceiver") ||
							methodName.equals("createSubscriber"))) {
						Destination dest = (Destination) args[0];
						if (dest != null && !(dest instanceof TemporaryQueue || dest instanceof TemporaryTopic)) {
							return getCachedConsumer(dest,
									(args.length > 1 ? (String) args[1] : null),
									(args.length > 2 && (Boolean) args[2]),
									null,
									false);
						}
					}
					else if (methodName.equals("createDurableConsumer") || methodName.equals("createDurableSubscriber")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									(args.length > 3 && (Boolean) args[3]),
									(String) args[1],
									true);
						}
					}
					else if (methodName.equals("createSharedConsumer")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									null,
									(String) args[1],
									false);
						}
					}
					else if (methodName.equals("createSharedDurableConsumer")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									null,
									(String) args[1],
									true);
						}
					}
				}
			}
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private MessageProducer getCachedProducer(Destination dest) throws JMSException {
			DestinationCacheKey cacheKey = (dest != null ? new DestinationCacheKey(dest) : null);
			MessageProducer producer = this.cachedProducers.get(cacheKey);
			if (producer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
			}
			else {
				producer = this.target.createProducer(dest);
				if (logger.isDebugEnabled()) {
					logger.debug("Registering cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
				this.cachedProducers.put(cacheKey, producer);
			}
			return new CachedMessageProducer(producer).getProxyIfNecessary();
		}

		private MessageConsumer getCachedConsumer(
				Destination dest, String selector, Boolean noLocal, String subscription, boolean durable) throws JMSException {

			ConsumerCacheKey cacheKey = new ConsumerCacheKey(dest, selector, noLocal, subscription, durable);
			MessageConsumer consumer = this.cachedConsumers.get(cacheKey);
			if (consumer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
			}
			else {
				if (dest instanceof Topic) {
					if (noLocal == null) {
						// createSharedConsumer((Topic) dest, subscription, selector);
						// createSharedDurableConsumer((Topic) dest, subscription, selector);
						Method method = (durable ? createSharedDurableConsumerMethod : createSharedConsumerMethod);
						try {
							consumer = (MessageConsumer) method.invoke(this.target, dest, subscription, selector);
						}
						catch (InvocationTargetException ex) {
							if (ex.getTargetException() instanceof JMSException) {
								throw (JMSException) ex.getTargetException();
							}
							ReflectionUtils.handleInvocationTargetException(ex);
						}
						catch (IllegalAccessException ex) {
							throw new IllegalStateException("Could not access JMS 2.0 API method: " + ex.getMessage());
						}
					}
					else {
						consumer = (durable ?
								this.target.createDurableSubscriber((Topic) dest, subscription, selector, noLocal) :
								this.target.createConsumer(dest, selector, noLocal));
					}
				}
				else {
					consumer = this.target.createConsumer(dest, selector);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Registering cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
				this.cachedConsumers.put(cacheKey, consumer);
			}
			return new CachedMessageConsumer(consumer);
		}

		private void logicalClose(Session proxy) throws JMSException {
			// 保留关闭时回滚语义.
			if (this.transactionOpen && this.target.getTransacted()) {
				this.transactionOpen = false;
				this.target.rollback();
			}
			// 会话结束时, 物理上关闭持久订阅者.
			for (Iterator<Map.Entry<ConsumerCacheKey, MessageConsumer>> it = this.cachedConsumers.entrySet().iterator(); it.hasNext();) {
				Map.Entry<ConsumerCacheKey, MessageConsumer> entry = it.next();
				if (entry.getKey().subscription != null) {
					entry.getValue().close();
					it.remove();
				}
			}
			// 允许多次close调用...
			boolean returned = false;
			synchronized (this.sessionList) {
				if (!this.sessionList.contains(proxy)) {
					this.sessionList.addLast(proxy);
					returned = true;
				}
			}
			if (returned && logger.isTraceEnabled()) {
				logger.trace("Returned cached Session: " + this.target);
			}
		}

		private void physicalClose() throws JMSException {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing cached Session: " + this.target);
			}
			// 显式关闭此Session恰好缓存的所有MessageProducer和MessageConsumer...
			try {
				for (MessageProducer producer : this.cachedProducers.values()) {
					producer.close();
				}
				for (MessageConsumer consumer : this.cachedConsumers.values()) {
					consumer.close();
				}
			}
			finally {
				this.cachedProducers.clear();
				this.cachedConsumers.clear();
				// 现在实际关闭Session.
				this.target.close();
			}
		}
	}


	/**
	 * 围绕Destination引用的简单包装类.
	 * 在缓存MessageProducer对象时用作缓存键.
	 */
	private static class DestinationCacheKey implements Comparable<DestinationCacheKey> {

		private final Destination destination;

		private String destinationString;

		public DestinationCacheKey(Destination destination) {
			Assert.notNull(destination, "Destination must not be null");
			this.destination = destination;
		}

		private String getDestinationString() {
			if (this.destinationString == null) {
				this.destinationString = this.destination.toString();
			}
			return this.destinationString;
		}

		protected boolean destinationEquals(DestinationCacheKey otherKey) {
			return (this.destination.getClass() == otherKey.destination.getClass() &&
					(this.destination.equals(otherKey.destination) ||
							getDestinationString().equals(otherKey.getDestinationString())));
		}

		@Override
		public boolean equals(Object other) {
			// 有效地检查对象相等性以及toString相等性.
			// 在WebSphere MQ上, Destination对象没有实现equals...
			return (this == other || destinationEquals((DestinationCacheKey) other));
		}

		@Override
		public int hashCode() {
			// 不能使用更具体的hashCode, 因为不能依赖this.destination.hashCode(), 实际上是相同目标的相同值...
			return this.destination.getClass().hashCode();
		}

		@Override
		public String toString() {
			return getDestinationString();
		}

		@Override
		public int compareTo(DestinationCacheKey other) {
			return getDestinationString().compareTo(other.getDestinationString());
		}
	}


	/**
	 * 围绕Destination和其他消费者属性的简单包装类.
	 * 在缓存MessageConsumer对象时用作缓存键.
	 */
	private static class ConsumerCacheKey extends DestinationCacheKey {

		private final String selector;

		private final Boolean noLocal;

		private final String subscription;

		private final boolean durable;

		public ConsumerCacheKey(Destination destination, String selector, Boolean noLocal, String subscription, boolean durable) {
			super(destination);
			this.selector = selector;
			this.noLocal = noLocal;
			this.subscription = subscription;
			this.durable = durable;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			ConsumerCacheKey otherKey = (ConsumerCacheKey) other;
			return (destinationEquals(otherKey) &&
					ObjectUtils.nullSafeEquals(this.selector, otherKey.selector) &&
					ObjectUtils.nullSafeEquals(this.noLocal, otherKey.noLocal) &&
					ObjectUtils.nullSafeEquals(this.subscription, otherKey.subscription) &&
					this.durable == otherKey.durable);
		}

		@Override
		public String toString() {
			return super.toString() + " [selector=" + this.selector + ", noLocal=" + this.noLocal +
					", subscription=" + this.subscription + ", durable=" + this.durable + "]";
		}
	}
}
