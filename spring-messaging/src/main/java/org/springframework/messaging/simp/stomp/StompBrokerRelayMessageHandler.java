package org.springframework.messaging.simp.stomp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.tcp.FixedIntervalReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * 一个{@link org.springframework.messaging.MessageHandler}, 它通过将消息转发给STOMP代理来处理消息.
 *
 * <p>对于每个新的{@link SimpMessageType#CONNECT CONNECT}消息, 将打开与代理的独立TCP连接,
 * 并专门用于发起C​​ONNECT消息的客户端的所有消息.
 * 来自同一客户端的消息通过会话ID消息header标识.
 * 相反, 当STOMP代理在TCP连接上发回消息时, 这些消息会使用客户端的会话ID进行丰富,
 * 并通过提供给构造函数的{@link MessageChannel}向下游发送回来.
 *
 * <p>此类还自动打开与消息代理的默认"系统" TCP连接,
 * 该消息代理用于发送源自服务器应用程序的消息 (而不是来自客户端).
 * 此类消息不与任何客户端关联, 因此没有会​​话ID header.
 * "系统"连接实际上是共享的, 不能用于接收消息.
 * 提供了几个属性来配置"系统"连接, 包括:
 * <ul>
 * <li>{@link #setSystemLogin}</li>
 * <li>{@link #setSystemPasscode}</li>
 * <li>{@link #setSystemHeartbeatSendInterval}</li>
 * <li>{@link #setSystemHeartbeatReceiveInterval}</li>
 * </ul>
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	public static final String SYSTEM_SESSION_ID = "_system_";

	// STOMP建议的接收心跳的误差
	private static final long HEARTBEAT_MULTIPLIER = 3;

	/**
	 * 一旦收到包含我们需要的心跳设置的CONNECTED帧, 就会设置心跳.
	 * 如果在一分钟内未收到CONNECTED, 则主动关闭连接.
	 */
	private static final int MAX_TIME_TO_CONNECTED_FRAME = 60 * 1000;

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static final ListenableFutureTask<Void> EMPTY_TASK = new ListenableFutureTask<Void>(new VoidCallable());

	private static final Message<byte[]> HEARTBEAT_MESSAGE;


	static {
		EMPTY_TASK.run();
		StompHeaderAccessor accessor = StompHeaderAccessor.createForHeartbeat();
		HEARTBEAT_MESSAGE = MessageBuilder.createMessage(StompDecoder.HEARTBEAT_PAYLOAD, accessor.getMessageHeaders());
	}


	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private final Map<String, MessageHandler> systemSubscriptions = new HashMap<String, MessageHandler>(4);

	private String virtualHost;

	private TcpOperations<byte[]> tcpClient;

	private MessageHeaderInitializer headerInitializer;

	private final Stats stats = new Stats();

	private final Map<String, StompConnectionHandler> connectionHandlers =
			new ConcurrentHashMap<String, StompConnectionHandler>();


	/**
	 * @param inboundChannel 用于从客户端 (e.g. WebSocket客户端)接收消息的通道
	 * @param outboundChannel 用于向客户端 (e.g. WebSocket客户端)发送消息的通道
	 * @param brokerChannel 应用程序向代理发送消息的通道
	 * @param destinationPrefixes 代理支持的目标前缀; 将忽略与给定前缀不匹配的目标.
	 */
	public StompBrokerRelayMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(inboundChannel, outboundChannel, brokerChannel, destinationPrefixes);
	}


	/**
	 * 设置STOMP消息代理主机.
	 */
	public void setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
	}

	/**
	 * 返回STOMP消息代理主机.
	 */
	public String getRelayHost() {
		return this.relayHost;
	}

	/**
	 * 设置STOMP消息代理端口.
	 */
	public void setRelayPort(int relayPort) {
		this.relayPort = relayPort;
	}

	/**
	 * 返回STOMP消息代理端口.
	 */
	public int getRelayPort() {
		return this.relayPort;
	}
	/**
	 * 设置客户端创建与STOMP代理的连接时使用的登录名.
	 * <p>默认"guest".
	 */
	public void setClientLogin(String clientLogin) {
		Assert.hasText(clientLogin, "clientLogin must not be empty");
		this.clientLogin = clientLogin;
	}

	/**
	 * 返回客户端创建与STOMP代理的连接时使用的登录名.
	 */
	public String getClientLogin() {
		return this.clientLogin;
	}

	/**
	 * 设置客户端创建与STOMP代理的连接时使用的密码.
	 * <p>默认"guest".
	 */
	public void setClientPasscode(String clientPasscode) {
		Assert.hasText(clientPasscode, "clientPasscode must not be empty");
		this.clientPasscode = clientPasscode;
	}

	/**
	 * 返回客户端创建与STOMP代理的连接时使用的密码.
	 */
	public String getClientPasscode() {
		return this.clientPasscode;
	}

	/**
	 * 设置用于从应用程序内向STOMP代理发送消息的共享"系统"连接的登录名,
	 * i.e. 与特定客户端会话无关的消息 (e.g. REST/HTTP请求处理方法).
	 * <p>默认 "guest".
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * 返回用于从应用程序内向STOMP代理发送消息的共享"系统"连接的登录名.
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * 设置用于从应用程序内向STOMP代理发送消息的共享"系统"连接的密码,
	 * i.e. 与特定客户端会话无关的消息 (e.g. REST/HTTP请求处理方法).
	 * <p>默认 "guest".
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * 返回用于从应用程序内向STOMP代理发送消息的共享"系统"连接的密码.
	 */
	public String getSystemPasscode() {
		return this.systemPasscode;
	}


	/**
	 * 设置"系统"连接在没有发送任何其他数据的情况下, 向STOMP代理发送心跳的间隔时间, 以毫秒为单位.
	 * 值为零将阻止将心跳发送到代理.
	 * <p>默认 10000.
	 * <p>有关"系统"连接的更多信息, 请参阅类级文档.
	 */
	public void setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
	}

	/**
	 * 返回"系统"连接在没有发送任何其他数据的情况下, 向STOMP代理发送心跳的间隔时间, 以毫秒为单位.
	 */
	public long getSystemHeartbeatSendInterval() {
		return this.systemHeartbeatSendInterval;
	}

	/**
	 * 设置"系统"连接在没有任何其他数据的情况下, 期望从STOMP代理接收心跳的最大间隔, 以毫秒为单位.
	 * 值为零将配置连接以期望不从代理接收心跳.
	 * <p>默认 10000.
	 * <p>有关"系统"连接的更多信息, 请参阅类级文档.
	 */
	public void setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
	}

	/**
	 * 返回"系统"连接在没有任何其他数据的情况下, 期望从STOMP代理接收心跳的最大间隔, 以毫秒为单位.
	 */
	public long getSystemHeartbeatReceiveInterval() {
		return this.systemHeartbeatReceiveInterval;
	}

	/**
	 * 在共享的"系统"连接上配置另外一个要订阅的目标, 以及MessageHandler来处理收到的消息.
	 * <p>这适用于多应用程序服务器场景中的内部使用, 其中服务器将消息转发给彼此 (e.g. 未解析的用户目标).
	 * 
	 * @param subscriptions 订阅的目标.
	 */
	public void setSystemSubscriptions(Map<String, MessageHandler> subscriptions) {
		this.systemSubscriptions.clear();
		if (subscriptions != null) {
			this.systemSubscriptions.putAll(subscriptions);
		}
	}

	/**
	 * 返回"system"连接上订阅已配置的映射.
	 */
	public Map<String, MessageHandler> getSystemSubscriptions() {
		return this.systemSubscriptions;
	}

	/**
	 * 设置要在STOMP CONNECT帧中使用的"host" header的值.
	 * 配置此属性后, 将向发送到STOMP代理的每个STOMP帧添加"host" header.
	 * 这可能是有用的, 例如在云环境中, 建立TCP连接的实际主机与提供基于云的STOMP服务的主机不同.
	 * <p>默认不设置.
	 */
	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * 返回配置的虚拟主机值.
	 */
	public String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * 配置TCP客户端以管理与STOMP代理的TCP连接.
	 * 默认{@link Reactor2TcpClient}.
	 * <p><strong>Note:</strong> 使用此属性时, 指定的任何{@link #setRelayHost(String) host}或{@link #setRelayPort(int) port}都会被忽略.
	 */
	public void setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * 获取配置的TCP客户端 (不能是{@code null}, 除非未调用配置, 并且在启动处理器之前调用此方法, 因此初始化默认实现).
	 */
	public TcpOperations<byte[]> getTcpClient() {
		return this.tcpClient;
	}

	/**
	 * 配置{@link MessageHeaderInitializer},
	 * 以应用于通过{@code StompBrokerRelayMessageHandler}创建的发送到客户端出站消息通道的所有消息的header.
	 * <p>默认不设置.
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

	/**
	 * 返回描述内部状态和计数器的String.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}

	/**
	 * 返回与代理的TCP连接的当前计数.
	 */
	public int getConnectionCount() {
		return this.connectionHandlers.size();
	}


	@Override
	protected void startInternal() {
		if (this.tcpClient == null) {
			StompDecoder decoder = new StompDecoder();
			decoder.setHeaderInitializer(getHeaderInitializer());
			Reactor2StompCodec codec = new Reactor2StompCodec(new StompEncoder(), decoder);
			this.tcpClient = new StompTcpClientFactory().create(this.relayHost, this.relayPort, codec);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Connecting \"system\" session to " + this.relayHost + ":" + this.relayPort);
		}

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setAcceptVersion("1.1,1.2");
		accessor.setLogin(this.systemLogin);
		accessor.setPasscode(this.systemPasscode);
		accessor.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		accessor.setHost(getVirtualHost());
		accessor.setSessionId(SYSTEM_SESSION_ID);
		if (logger.isDebugEnabled()) {
			logger.debug("Forwarding " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
		}

		SystemStompConnectionHandler handler = new SystemStompConnectionHandler(accessor);
		this.connectionHandlers.put(handler.getSessionId(), handler);

		this.stats.incrementConnectCount();
		this.tcpClient.connect(handler, new FixedIntervalReconnectStrategy(5000));
	}

	@Override
	protected void stopInternal() {
		publishBrokerUnavailableEvent();
		try {
			this.tcpClient.shutdown().get(5000, TimeUnit.MILLISECONDS);
		}
		catch (Throwable ex) {
			logger.error("Error in shutdown of TCP client", ex);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());

		if (!isBrokerAvailable()) {
			if (sessionId == null || SYSTEM_SESSION_ID.equals(sessionId)) {
				throw new MessageDeliveryException("Message broker not active. Consider subscribing to " +
						"receive BrokerAvailabilityEvent's from an ApplicationListener Spring bean.");
			}
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler != null) {
				handler.sendStompErrorFrameToClient("Broker not available.");
				handler.clearConnection();
			}
			else {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(accessor);
				}
				accessor.setSessionId(sessionId);
				accessor.setUser(SimpMessageHeaderAccessor.getUser(message.getHeaders()));
				accessor.setMessage("Broker not available.");
				MessageHeaders headers = accessor.getMessageHeaders();
				getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers));
			}
			return;
		}

		StompHeaderAccessor stompAccessor;
		StompCommand command;

		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor == null) {
			throw new IllegalStateException(
					"No header accessor (not using the SimpMessagingTemplate?): " + message);
		}
		else if (accessor instanceof StompHeaderAccessor) {
			stompAccessor = (StompHeaderAccessor) accessor;
			command = stompAccessor.getCommand();
		}
		else if (accessor instanceof SimpMessageHeaderAccessor) {
			stompAccessor = StompHeaderAccessor.wrap(message);
			command = stompAccessor.getCommand();
			if (command == null) {
				command = stompAccessor.updateStompCommandAsClientMessage();
			}
		}
		else {
			throw new IllegalStateException(
					"Unexpected header accessor type " + accessor.getClass() + " in " + message);
		}

		if (sessionId == null) {
			if (!SimpMessageType.MESSAGE.equals(stompAccessor.getMessageType())) {
				if (logger.isErrorEnabled()) {
					logger.error("Only STOMP SEND supported from within the server side. Ignoring " + message);
				}
				return;
			}
			sessionId = SYSTEM_SESSION_ID;
			stompAccessor.setSessionId(sessionId);
		}

		String destination = stompAccessor.getDestination();
		if (command != null && command.requiresDestination() && !checkDestinationPrefix(destination)) {
			return;
		}

		if (StompCommand.CONNECT.equals(command)) {
			if (logger.isDebugEnabled()) {
				logger.debug(stompAccessor.getShortLogMessage(EMPTY_PAYLOAD));
			}
			stompAccessor = (stompAccessor.isMutable() ? stompAccessor : StompHeaderAccessor.wrap(message));
			stompAccessor.setLogin(this.clientLogin);
			stompAccessor.setPasscode(this.clientPasscode);
			if (getVirtualHost() != null) {
				stompAccessor.setHost(getVirtualHost());
			}
			StompConnectionHandler handler = new StompConnectionHandler(sessionId, stompAccessor);
			this.connectionHandlers.put(sessionId, handler);
			this.stats.incrementConnectCount();
			this.tcpClient.connect(handler);
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring DISCONNECT in session " + sessionId + ". Connection already cleaned up.");
				}
				return;
			}
			stats.incrementDisconnectCount();
			handler.forward(message, stompAccessor);
		}
		else {
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No TCP connection for session " + sessionId + " in " + message);
				}
				return;
			}
			handler.forward(message, stompAccessor);
		}
	}

	@Override
	public String toString() {
		return "StompBrokerRelay[" + this.relayHost + ":" + this.relayPort + "]";
	}


	private class StompConnectionHandler implements TcpConnectionHandler<byte[]> {

		private final String sessionId;

		private final boolean isRemoteClientSession;

		private final StompHeaderAccessor connectHeaders;

		private volatile TcpConnection<byte[]> tcpConnection;

		private volatile boolean isStompConnected;


		private StompConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders) {
			this(sessionId, connectHeaders, true);
		}

		private StompConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders, boolean isClientSession) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			Assert.notNull(connectHeaders, "'connectHeaders' must not be null");
			this.sessionId = sessionId;
			this.connectHeaders = connectHeaders;
			this.isRemoteClientSession = isClientSession;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		protected TcpConnection<byte[]> getTcpConnection() {
			return this.tcpConnection;
		}

		@Override
		public void afterConnected(TcpConnection<byte[]> connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("TCP connection opened in session=" + getSessionId());
			}
			this.tcpConnection = connection;
			this.tcpConnection.onReadInactivity(new Runnable() {
				@Override
				public void run() {
					if (tcpConnection != null && !isStompConnected) {
						handleTcpConnectionFailure("No CONNECTED frame received in " +
								MAX_TIME_TO_CONNECTED_FRAME + " ms.", null);
					}
				}
			}, MAX_TIME_TO_CONNECTED_FRAME);
			connection.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, this.connectHeaders.getMessageHeaders()));
		}

		@Override
		public void afterConnectFailure(Throwable ex) {
			handleTcpConnectionFailure("Failed to connect: " + ex.getMessage(), ex);
		}

		/**
		 * 检测到任何TCP连接问题时调用, i.e. 无法建立TCP连接, 无法发送消息, 丢失心跳等.
		 */
		protected void handleTcpConnectionFailure(String error, Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("TCP connection failure in session " + this.sessionId + ": " + error, ex);
			}
			try {
				sendStompErrorFrameToClient(error);
			}
			finally {
				try {
					clearConnection();
				}
				catch (Throwable ex2) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex2);
					}
				}
			}
		}

		private void sendStompErrorFrameToClient(String errorText) {
			if (this.isRemoteClientSession) {
				StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(headerAccessor);
				}
				headerAccessor.setSessionId(this.sessionId);
				headerAccessor.setUser(this.connectHeaders.getUser());
				headerAccessor.setMessage(errorText);
				Message<?> errorMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, headerAccessor.getMessageHeaders());
				handleInboundMessage(errorMessage);
			}
		}

		protected void handleInboundMessage(Message<?> message) {
			if (this.isRemoteClientSession) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				accessor.setImmutable();
				StompBrokerRelayMessageHandler.this.getClientOutboundChannel().send(message);
			}
		}

		@Override
		public void handleMessage(Message<byte[]> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			accessor.setSessionId(this.sessionId);
			accessor.setUser(this.connectHeaders.getUser());

			StompCommand command = accessor.getCommand();
			if (StompCommand.CONNECTED.equals(command)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
				}
				afterStompConnected(accessor);
			}
			else if (logger.isErrorEnabled() && StompCommand.ERROR.equals(command)) {
				logger.error("Received " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Received " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			handleInboundMessage(message);
		}

		/**
		 * 在收到STOMP CONNECTED帧后调用. 此时, 连接已准备好将STOMP消息发送到代理.
		 */
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			this.isStompConnected = true;
			stats.incrementConnectedCount();
			initHeartbeats(connectedHeaders);
		}

		private void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			if (this.isRemoteClientSession) {
				return;
			}

			long clientSendInterval = this.connectHeaders.getHeartbeat()[0];
			long clientReceiveInterval = this.connectHeaders.getHeartbeat()[1];
			long serverSendInterval = connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = connectedHeaders.getHeartbeat()[1];

			if (clientSendInterval > 0 && serverReceiveInterval > 0) {
				long interval = Math.max(clientSendInterval,  serverReceiveInterval);
				this.tcpConnection.onWriteInactivity(new Runnable() {
					@Override
					public void run() {
						TcpConnection<byte[]> conn = tcpConnection;
						if (conn != null) {
							conn.send(HEARTBEAT_MESSAGE).addCallback(
									new ListenableFutureCallback<Void>() {
										public void onSuccess(Void result) {
										}
										public void onFailure(Throwable ex) {
											handleTcpConnectionFailure(
													"Failed to forward heartbeat: " + ex.getMessage(), ex);
										}
									});
						}
					}
				}, interval);
			}
			if (clientReceiveInterval > 0 && serverSendInterval > 0) {
				final long interval = Math.max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
				this.tcpConnection.onReadInactivity(new Runnable() {
					@Override
					public void run() {
						handleTcpConnectionFailure("No messages received in " + interval + " ms.", null);
					}
				}, interval);
			}
		}

		@Override
		public void handleFailure(Throwable ex) {
			if (this.tcpConnection != null) {
				handleTcpConnectionFailure("Transport failure: " + ex.getMessage(), ex);
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Transport failure: " + ex);
			}
		}

		@Override
		public void afterConnectionClosed() {
			if (this.tcpConnection == null) {
				return;
			}
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("TCP connection to broker closed in session " + this.sessionId);
				}
				sendStompErrorFrameToClient("Connection to broker closed.");
			}
			finally {
				try {
					// 防止clearConnection() 尝试关闭
					this.tcpConnection = null;
					clearConnection();
				}
				catch (Throwable ex) {
					// Shouldn't happen with connection reset beforehand
				}
			}
		}

		/**
		 * 将给定消息转发给STOMP代理.
		 * <p>该方法检查是否具有活动的TCP连接, 并且已收到STOMP CONNECTED帧.
		 * 对于客户端消息, 只有在转发客户端消息的同时丢失TCP连接, 才应该是false, 所以只需在调试级别记录被忽略的消息.
		 * 对于在"系统"连接上从应用程序内发送的消息, 会引发异常, 以便发送消息的组件有机会处理它 -- 默认情况下, 代理消息通道是同步的.
		 * <p>请注意, 如果消息在TCP连接丢失的同时到达, 则在重置连接之前, 会有一段短暂的时间, 一条或多条消息可能会潜入并尝试转发它们.
		 * 这种方法不是同步来防范, 而是让它们尝试失败.
		 * 对于可能导致向下游发送额外STOMP ERROR帧的客户端会话, 但在这种情况下代码处理下游应该是幂等的.
		 * 
		 * @param message 要发送的消息 (never {@code null})
		 * 
		 * @return 等待结果的Future
		 */
		@SuppressWarnings("unchecked")
		public ListenableFuture<Void> forward(final Message<?> message, final StompHeaderAccessor accessor) {
			TcpConnection<byte[]> conn = this.tcpConnection;

			if (!this.isStompConnected || conn == null) {
				if (this.isRemoteClientSession) {
					if (logger.isDebugEnabled()) {
						logger.debug("TCP connection closed already, ignoring " +
								accessor.getShortLogMessage(message.getPayload()));
					}
					return EMPTY_TASK;
				}
				else {
					throw new IllegalStateException("Cannot forward messages " +
							(conn != null ? "before STOMP CONNECTED. " : "while inactive. ") +
							"Consider subscribing to receive BrokerAvailabilityEvent's from " +
							"an ApplicationListener Spring bean. Dropped " +
							accessor.getShortLogMessage(message.getPayload()));
				}
			}

			final Message<?> messageToSend = (accessor.isMutable() && accessor.isModified()) ?
					MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders()) : message;

			StompCommand command = accessor.getCommand();
			if (logger.isDebugEnabled() && (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command) ||
					StompCommand.UNSUBSCRIBE.equals(command) || StompCommand.DISCONNECT.equals(command))) {
				logger.debug("Forwarding " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Forwarding " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			ListenableFuture<Void> future = conn.send((Message<byte[]>) messageToSend);
			future.addCallback(new ListenableFutureCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					if (accessor.getCommand() == StompCommand.DISCONNECT) {
						afterDisconnectSent(accessor);
					}
				}
				@Override
				public void onFailure(Throwable ex) {
					if (tcpConnection != null) {
						handleTcpConnectionFailure("failed to forward " +
								accessor.getShortLogMessage(message.getPayload()), ex);
					}
					else if (logger.isErrorEnabled()) {
						logger.error("Failed to forward " + accessor.getShortLogMessage(message.getPayload()));
					}
				}
			});
			return future;
		}

		/**
		 * 在DISCONNECT之后, 应该没有更多的客户端帧, 因此可以主动关闭连接.
		 * 但是, 如果DISCONNECT有一个接收header, 保持连接打开, 并期望服务器响应RECEIPT, 然后关闭连接.
		 * 
		 * @see <a href="http://stomp.github.io/stomp-specification-1.2.html#DISCONNECT">
		 *     STOMP Specification 1.2 DISCONNECT</a>
		 */
		private void afterDisconnectSent(StompHeaderAccessor accessor) {
			if (accessor.getReceipt() == null) {
				try {
					clearConnection();
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex);
					}
				}
			}
		}

		/**
		 * 清除与连接关联的状态并将其关闭.
		 * 传播关闭连接引起的任何异常.
		 */
		public void clearConnection() {
			if (logger.isDebugEnabled()) {
				logger.debug("Cleaning up connection state for session " + this.sessionId);
			}

			if (this.isRemoteClientSession) {
				StompBrokerRelayMessageHandler.this.connectionHandlers.remove(this.sessionId);
			}

			this.isStompConnected = false;

			TcpConnection<byte[]> conn = this.tcpConnection;
			this.tcpConnection = null;
			if (conn != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing TCP connection in session " + this.sessionId);
				}
				conn.close();
			}
		}

		@Override
		public String toString() {
			return "StompConnectionHandler[sessionId=" + this.sessionId + "]";
		}
	}


	private class SystemStompConnectionHandler extends StompConnectionHandler {

		public SystemStompConnectionHandler(StompHeaderAccessor connectHeaders) {
			super(SYSTEM_SESSION_ID, connectHeaders, false);
		}

		@Override
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			if (logger.isInfoEnabled()) {
				logger.info("\"System\" session connected.");
			}
			super.afterStompConnected(connectedHeaders);
			publishBrokerAvailableEvent();
			sendSystemSubscriptions();
		}

		private void sendSystemSubscriptions() {
			int i = 0;
			for (String destination : getSystemSubscriptions().keySet()) {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
				accessor.setSubscriptionId(String.valueOf(i++));
				accessor.setDestination(destination);
				if (logger.isDebugEnabled()) {
					logger.debug("Subscribing to " + destination + " on \"system\" connection.");
				}
				TcpConnection<byte[]> conn = getTcpConnection();
				if (conn != null) {
					MessageHeaders headers = accessor.getMessageHeaders();
					conn.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers)).addCallback(
							new ListenableFutureCallback<Void>() {
								public void onSuccess(Void result) {
								}
								public void onFailure(Throwable ex) {
									String error = "Failed to subscribe in \"system\" session.";
									handleTcpConnectionFailure(error, ex);
								}
							});
				}
			}
		}

		@Override
		protected void handleInboundMessage(Message<?> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
				String destination = accessor.getDestination();
				if (destination == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection, with no destination: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				if (!getSystemSubscriptions().containsKey(destination)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection with no handler: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				try {
					MessageHandler handler = getSystemSubscriptions().get(destination);
					handler.handleMessage(message);
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Error while handling message on \"system\" connection.", ex);
					}
				}
			}
		}

		@Override
		protected void handleTcpConnectionFailure(String errorMessage, Throwable ex) {
			super.handleTcpConnectionFailure(errorMessage, ex);
			publishBrokerUnavailableEvent();
		}

		@Override
		public void afterConnectionClosed() {
			super.afterConnectionClosed();
			publishBrokerUnavailableEvent();
		}

		@Override
		public ListenableFuture<Void> forward(Message<?> message, StompHeaderAccessor accessor) {
			try {
				ListenableFuture<Void> future = super.forward(message, accessor);
				if (message.getHeaders().get(SimpMessageHeaderAccessor.IGNORE_ERROR) == null) {
					future.get();
				}
				return future;
			}
			catch (Throwable ex) {
				throw new MessageDeliveryException(message, ex);
			}
		}
	}


	private static class StompTcpClientFactory {

		public TcpOperations<byte[]> create(String relayHost, int relayPort, Reactor2StompCodec codec) {
			return new Reactor2TcpClient<byte[]>(relayHost, relayPort, codec);
		}
	}


	private static class VoidCallable implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			return null;
		}
	}


	private class Stats {

		private final AtomicInteger connect = new AtomicInteger();

		private final AtomicInteger connected = new AtomicInteger();

		private final AtomicInteger disconnect = new AtomicInteger();

		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

		public String toString() {
			return (connectionHandlers.size() + " sessions, " + relayHost + ":" + relayPort +
					(isBrokerAvailable() ? " (available)" : " (not available)") +
					", processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")");
		}
	}

}
