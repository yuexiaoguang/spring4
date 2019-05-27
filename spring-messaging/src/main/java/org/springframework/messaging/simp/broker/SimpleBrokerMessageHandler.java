package org.springframework.messaging.simp.broker;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

/**
 * 一个"简单"消息代理, 它识别{@link SimpMessageType}中定义的消息类型,
 * 在{@link SubscriptionRegistry}的帮助下跟踪订阅并向订阅者发送消息.
 */
public class SimpleBrokerMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];


	private PathMatcher pathMatcher;

	private Integer cacheLimit;

	private String selectorHeaderName = "selector";

	private TaskScheduler taskScheduler;

	private long[] heartbeatValue;

	private MessageHeaderInitializer headerInitializer;


	private SubscriptionRegistry subscriptionRegistry;

	private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<String, SessionInfo>();

	private ScheduledFuture<?> heartbeatFuture;


	/**
	 * @param clientInboundChannel 用于从客户端 (e.g. WebSocket客户端)接收消息的通道
	 * @param clientOutboundChannel 用于向客户端 (e.g. WebSocket客户端)发送消息的通道
	 * @param brokerChannel 应用程序向代理发送消息的通道
	 * @param destinationPrefixes 用于过滤消息的前缀
	 */
	public SimpleBrokerMessageHandler(SubscribableChannel clientInboundChannel, MessageChannel clientOutboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(clientInboundChannel, clientOutboundChannel, brokerChannel, destinationPrefixes);
		this.subscriptionRegistry = new DefaultSubscriptionRegistry();
	}


	/**
	 * 配置自定义SubscriptionRegistry以用于存储订阅.
	 * <p><strong>Note</strong> 当通过{@link #setPathMatcher}配置自定义PathMatcher时,
	 * 如果自定义注册表不是{@link DefaultSubscriptionRegistry}的实例, 则不使用提供的PathMatcher, 必须直接在自定义注册表上配置.
	 */
	public void setSubscriptionRegistry(SubscriptionRegistry subscriptionRegistry) {
		Assert.notNull(subscriptionRegistry, "SubscriptionRegistry must not be null");
		this.subscriptionRegistry = subscriptionRegistry;
		initPathMatcherToUse();
		initCacheLimitToUse();
		initSelectorHeaderNameToUse();
	}

	public SubscriptionRegistry getSubscriptionRegistry() {
		return this.subscriptionRegistry;
	}

	/**
	 * 配置订阅消息可以具有的header名称, 以便过滤与订阅匹配的消息.
	 * header值应该是一个Spring EL布尔表达式, 应用于与订阅匹配的消息的header.
	 * <p>例如:
	 * <pre>
	 * headers.foo == 'bar'
	 * </pre>
	 * <p>默认为"selector". 可以将其设置为其他名称, 或{@code null}以关闭对选择器header的支持.
	 * 
	 * @param selectorHeaderName 用于选择器header的名称
	 */
	public void setSelectorHeaderName(String selectorHeaderName) {
		this.selectorHeaderName = selectorHeaderName;
		initSelectorHeaderNameToUse();
	}

	private void initSelectorHeaderNameToUse() {
		if (this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setSelectorHeaderName(this.selectorHeaderName);
		}
	}

	/**
	 * 配置后, 给定的PathMatcher将传递给底层的SubscriptionRegistry, 以用于匹配目标和订阅.
	 * <p>默认是标准的{@link org.springframework.util.AntPathMatcher}.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
		initPathMatcherToUse();
	}

	private void initPathMatcherToUse() {
		if (this.pathMatcher != null && this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setPathMatcher(this.pathMatcher);
		}
	}

	/**
	 * 配置后, 指定的缓存限制将传递给底层SubscriptionRegistry, 从而覆盖其中的默认值.
	 * <p>使用标准{@link DefaultSubscriptionRegistry}, 默认缓存限制为 1024.
	 */
	public void setCacheLimit(Integer cacheLimit) {
		this.cacheLimit = cacheLimit;
		initCacheLimitToUse();
	}

	private void initCacheLimitToUse() {
		if (this.cacheLimit != null && this.subscriptionRegistry instanceof DefaultSubscriptionRegistry) {
			((DefaultSubscriptionRegistry) this.subscriptionRegistry).setCacheLimit(this.cacheLimit);
		}
	}

	/**
	 * 配置{@link org.springframework.scheduling.TaskScheduler}以用于提供心跳支持.
	 * 设置此属性还会将{@link #setHeartbeatValue heartbeatValue}设置为 "10000, 10000".
	 * <p>默认不设置.
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");
		this.taskScheduler = taskScheduler;
		if (this.heartbeatValue == null) {
			this.heartbeatValue = new long[] {10000, 10000};
		}
	}

	/**
	 * 返回配置的TaskScheduler.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 配置心跳设置的值.
	 * 第一个数字表示服务器写入或发送心跳的频率.
	 * 第二个是客户端应该多久写一次. 0 表示没有心跳.
	 * <p>默认设置为 "0, 0", 除非{@link #setTaskScheduler taskScheduler}在这种情况下默认值为"10000,10000" (以毫秒为单位).
	 */
	public void setHeartbeatValue(long[] heartbeat) {
		if (heartbeat == null || heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.heartbeatValue = heartbeat;
	}

	/**
	 * 心跳设置的配置值.
	 */
	public long[] getHeartbeatValue() {
		return this.heartbeatValue;
	}

	/**
	 * 配置{@link MessageHeaderInitializer}以应用于发送到客户端出站通道的所有消息的header.
	 * <p>默认不设置此属性.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回配置的header初始化器.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	@Override
	public void startInternal() {
		publishBrokerAvailableEvent();
		if (getTaskScheduler() != null) {
			long interval = initHeartbeatTaskDelay();
			if (interval > 0) {
				this.heartbeatFuture = this.taskScheduler.scheduleWithFixedDelay(new HeartbeatTask(), interval);
			}
		}
		else {
			Assert.isTrue(getHeartbeatValue() == null ||
					(getHeartbeatValue()[0] == 0 && getHeartbeatValue()[1] == 0),
					"Heartbeat values configured but no TaskScheduler provided");
		}
	}

	private long initHeartbeatTaskDelay() {
		if (getHeartbeatValue() == null) {
			return 0;
		}
		else if (getHeartbeatValue()[0] > 0 && getHeartbeatValue()[1] > 0) {
			return Math.min(getHeartbeatValue()[0], getHeartbeatValue()[1]);
		}
		else {
			return (getHeartbeatValue()[0] > 0 ? getHeartbeatValue()[0] : getHeartbeatValue()[1]);
		}
	}

	@Override
	public void stopInternal() {
		publishBrokerUnavailableEvent();
		if (this.heartbeatFuture != null) {
			this.heartbeatFuture.cancel(true);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);

		updateSessionReadTime(sessionId);

		if (!checkDestinationPrefix(destination)) {
			return;
		}

		if (SimpMessageType.MESSAGE.equals(messageType)) {
			logMessage(message);
			sendMessageToSubscribers(destination, message);
		}
		else if (SimpMessageType.CONNECT.equals(messageType)) {
			logMessage(message);
			long[] clientHeartbeat = SimpMessageHeaderAccessor.getHeartbeat(headers);
			long[] serverHeartbeat = getHeartbeatValue();
			Principal user = SimpMessageHeaderAccessor.getUser(headers);
			this.sessions.put(sessionId, new SessionInfo(sessionId, user, clientHeartbeat, serverHeartbeat));
			SimpMessageHeaderAccessor connectAck = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
			initHeaders(connectAck);
			connectAck.setSessionId(sessionId);
			connectAck.setUser(SimpMessageHeaderAccessor.getUser(headers));
			connectAck.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, message);
			connectAck.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, serverHeartbeat);
			Message<byte[]> messageOut = MessageBuilder.createMessage(EMPTY_PAYLOAD, connectAck.getMessageHeaders());
			getClientOutboundChannel().send(messageOut);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			logMessage(message);
			Principal user = SimpMessageHeaderAccessor.getUser(headers);
			handleDisconnect(sessionId, user, message);
		}
		else if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			logMessage(message);
			this.subscriptionRegistry.registerSubscription(message);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			logMessage(message);
			this.subscriptionRegistry.unregisterSubscription(message);
		}
	}

	private void updateSessionReadTime(String sessionId) {
		if (sessionId != null) {
			SessionInfo info = this.sessions.get(sessionId);
			if (info != null) {
				info.setLastReadTime(System.currentTimeMillis());
			}
		}
	}

	private void logMessage(Message<?> message) {
		if (logger.isDebugEnabled()) {
			SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			accessor = (accessor != null ? accessor : SimpMessageHeaderAccessor.wrap(message));
			logger.debug("Processing " + accessor.getShortLogMessage(message.getPayload()));
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor accessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
	}

	private void handleDisconnect(String sessionId, Principal user, Message<?> origMessage) {
		this.sessions.remove(sessionId);
		this.subscriptionRegistry.unregisterAllSubscriptions(sessionId);
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT_ACK);
		accessor.setSessionId(sessionId);
		accessor.setUser(user);
		if (origMessage != null) {
			accessor.setHeader(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER, origMessage);
		}
		initHeaders(accessor);
		Message<byte[]> message = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
		getClientOutboundChannel().send(message);
	}

	protected void sendMessageToSubscribers(String destination, Message<?> message) {
		MultiValueMap<String,String> subscriptions = this.subscriptionRegistry.findSubscriptions(message);
		if (!subscriptions.isEmpty() && logger.isDebugEnabled()) {
			logger.debug("Broadcasting to " + subscriptions.size() + " sessions.");
		}
		long now = System.currentTimeMillis();
		for (Map.Entry<String, List<String>> subscriptionEntry : subscriptions.entrySet()) {
			for (String subscriptionId : subscriptionEntry.getValue()) {
				SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				initHeaders(headerAccessor);
				headerAccessor.setSessionId(subscriptionEntry.getKey());
				headerAccessor.setSubscriptionId(subscriptionId);
				headerAccessor.copyHeadersIfAbsent(message.getHeaders());
				Object payload = message.getPayload();
				Message<?> reply = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
				try {
					getClientOutboundChannel().send(reply);
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to send " + message, ex);
					}
				}
				finally {
					SessionInfo info = this.sessions.get(subscriptionEntry.getKey());
					if (info != null) {
						info.setLastWriteTime(now);
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "SimpleBrokerMessageHandler [" + this.subscriptionRegistry + "]";
	}


	private static class SessionInfo {

		/* STOMP spec: 接收方应该考虑到误差范围 */
		private static final long HEARTBEAT_MULTIPLIER = 3;

		private final String sessiondId;

		private final Principal user;

		private final long readInterval;

		private final long writeInterval;

		private volatile long lastReadTime;

		private volatile long lastWriteTime;

		public SessionInfo(String sessiondId, Principal user, long[] clientHeartbeat, long[] serverHeartbeat) {
			this.sessiondId = sessiondId;
			this.user = user;
			if (clientHeartbeat != null && serverHeartbeat != null) {
				this.readInterval = (clientHeartbeat[0] > 0 && serverHeartbeat[1] > 0 ?
						Math.max(clientHeartbeat[0], serverHeartbeat[1]) * HEARTBEAT_MULTIPLIER : 0);
				this.writeInterval = (clientHeartbeat[1] > 0 && serverHeartbeat[0] > 0 ?
						Math.max(clientHeartbeat[1], serverHeartbeat[0]) : 0);
			}
			else {
				this.readInterval = 0;
				this.writeInterval = 0;
			}
			this.lastReadTime = this.lastWriteTime = System.currentTimeMillis();
		}

		public String getSessiondId() {
			return this.sessiondId;
		}

		public Principal getUser() {
			return this.user;
		}

		public long getReadInterval() {
			return this.readInterval;
		}

		public long getWriteInterval() {
			return this.writeInterval;
		}

		public long getLastReadTime() {
			return this.lastReadTime;
		}

		public void setLastReadTime(long lastReadTime) {
			this.lastReadTime = lastReadTime;
		}

		public long getLastWriteTime() {
			return this.lastWriteTime;
		}

		public void setLastWriteTime(long lastWriteTime) {
			this.lastWriteTime = lastWriteTime;
		}
	}


	private class HeartbeatTask implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			for (SessionInfo info : sessions.values()) {
				if (info.getReadInterval() > 0 && (now - info.getLastReadTime()) > info.getReadInterval()) {
					handleDisconnect(info.getSessiondId(), info.getUser(), null);
				}
				if (info.getWriteInterval() > 0 && (now - info.getLastWriteTime()) > info.getWriteInterval()) {
					SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
					accessor.setSessionId(info.getSessiondId());
					accessor.setUser(info.getUser());
					initHeaders(accessor);
					MessageHeaders headers = accessor.getMessageHeaders();
					getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers));
				}
			}
		}
	}
}
