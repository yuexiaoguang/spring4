package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

/**
 * 用于STOMP的{@link SubProtocolHandler}, 支持STOMP规范的1.0, 1.1和1.2版本.
 */
public class StompSubProtocolHandler implements SubProtocolHandler, ApplicationEventPublisherAware {

	/**
	 * 此处理程序支持将大型STOMP消息拆分为多个WebSocket消息, 并且STOMP客户端 (如 stomp.js)确实在16K边界拆分大型STOMP消息.
	 * 因此, WebSocket服务器输入消息缓冲区大小必须至少允许16K加上SockJS框架的额外一点.
	 */
	public static final int MINIMUM_WEBSOCKET_MESSAGE_SIZE = 16 * 1024 + 256;

	/**
	 * CONNECTED帧上header的名称, 指示在WebSocket会话上进行身份验证的用户的名称.
	 */
	public static final String CONNECTED_USER_HEADER = "user-name";

	private static final Log logger = LogFactory.getLog(StompSubProtocolHandler.class);

	private static final byte[] EMPTY_PAYLOAD = new byte[0];


	private StompSubProtocolErrorHandler errorHandler;

	private int messageSizeLimit = 64 * 1024;

	@SuppressWarnings("deprecation")
	private org.springframework.messaging.simp.user.UserSessionRegistry userSessionRegistry;

	private StompEncoder stompEncoder = new StompEncoder();

	private StompDecoder stompDecoder = new StompDecoder();

	private final Map<String, BufferingStompDecoder> decoders = new ConcurrentHashMap<String, BufferingStompDecoder>();

	private MessageHeaderInitializer headerInitializer;

	private final Map<String, Principal> stompAuthentications = new ConcurrentHashMap<String, Principal>();

	private Boolean immutableMessageInterceptorPresent;

	private ApplicationEventPublisher eventPublisher;

	private final Stats stats = new Stats();


	/**
	 * 为发送给客户端的错误消息配置处理器, 允许自定义错误消息或阻止发送错误消息.
	 * <p>默认未配置, 在此情况下发送ERROR帧, 并显示反映错误的消息header.
	 * 
	 * @param errorHandler 错误处理器
	 */
	public void setErrorHandler(StompSubProtocolErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回配置的错误处理器.
	 */
	public StompSubProtocolErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * 配置传入STOMP消息允许的最大大小.
	 * 由于可以在多个WebSocket消息中接收STOMP消息, 因此可能需要缓冲, 因此有必要知道允许的最大消息大小.
	 * <p>默认为 64K.
	 */
	public void setMessageSizeLimit(int messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
	}

	/**
	 * 获取配置的消息缓冲区大小限制, 以字节为单位.
	 */
	public int getMessageSizeLimit() {
		return this.messageSizeLimit;
	}

	/**
	 * 提供用于注册活动用户会话ID的注册表.
	 * 
	 * @deprecated as of 4.2 in favor of {@link DefaultSimpUserRegistry} which relies
	 * on the ApplicationContext events published by this class and is created via
	 * {@link org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport#createLocalUserRegistry
	 * WebSocketMessageBrokerConfigurationSupport.createLocalUserRegistry}
	 */
	@Deprecated
	public void setUserSessionRegistry(org.springframework.messaging.simp.user.UserSessionRegistry registry) {
		this.userSessionRegistry = registry;
	}

	/**
	 * @deprecated as of 4.2
	 */
	@Deprecated
	public org.springframework.messaging.simp.user.UserSessionRegistry getUserSessionRegistry() {
		return this.userSessionRegistry;
	}

	/**
	 * 配置编码STOMP帧的{@link StompEncoder}
	 */
	public void setEncoder(StompEncoder encoder) {
		this.stompEncoder = encoder;
	}

	/**
	 * 配置解码STOMP帧的{@link StompDecoder}
	 */
	public void setDecoder(StompDecoder decoder) {
		this.stompDecoder = decoder;
	}

	/**
	 * 配置{@link MessageHeaderInitializer}以应用于从解码的STOMP帧创建的所有消息和发送到客户端入站channel的其他消息的header.
	 * <p>默认未设置.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
		this.stompDecoder.setHeaderInitializer(headerInitializer);
	}

	/**
	 * 返回配置的header初始化器.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	@Override
	public List<String> getSupportedProtocols() {
		return Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp");
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	/**
	 * 返回描述内部状态和计数器的String.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}


	/**
	 * 处理来自客户端的传入WebSocket消息.
	 */
	public void handleMessageFromClient(WebSocketSession session,
			WebSocketMessage<?> webSocketMessage, MessageChannel outputChannel) {

		List<Message<byte[]>> messages;
		try {
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage) {
				byteBuffer = ByteBuffer.wrap(((TextMessage) webSocketMessage).asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage) {
				byteBuffer = ((BinaryMessage) webSocketMessage).getPayload();
			}
			else {
				return;
			}

			BufferingStompDecoder decoder = this.decoders.get(session.getId());
			if (decoder == null) {
				throw new IllegalStateException("No decoder for session id '" + session.getId() + "'");
			}

			messages = decoder.decode(byteBuffer);
			if (messages.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Incomplete STOMP frame content received in session " +
							session + ", bufferSize=" + decoder.getBufferSize() +
							", bufferSizeLimit=" + decoder.getBufferSizeLimit() + ".");
				}
				return;
			}
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to parse " + webSocketMessage +
						" in session " + session.getId() + ". Sending STOMP ERROR to client.", ex);
			}
			handleError(session, ex, null);
			return;
		}

		for (Message<byte[]> message : messages) {
			try {
				StompHeaderAccessor headerAccessor =
						MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

				headerAccessor.setSessionId(session.getId());
				headerAccessor.setSessionAttributes(session.getAttributes());
				headerAccessor.setUser(getUser(session));
				headerAccessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, headerAccessor.getHeartbeat());
				if (!detectImmutableMessageInterceptor(outputChannel)) {
					headerAccessor.setImmutable();
				}

				if (logger.isTraceEnabled()) {
					logger.trace("From client: " + headerAccessor.getShortLogMessage(message.getPayload()));
				}

				StompCommand command = headerAccessor.getCommand();
				boolean isConnect = StompCommand.CONNECT.equals(command);
				if (isConnect) {
					this.stats.incrementConnectCount();
				}
				else if (StompCommand.DISCONNECT.equals(command)) {
					this.stats.incrementDisconnectCount();
				}

				try {
					SimpAttributesContextHolder.setAttributesFromMessage(message);
					boolean sent = outputChannel.send(message);

					if (sent) {
						if (isConnect) {
							Principal user = headerAccessor.getUser();
							if (user != null && user != session.getPrincipal()) {
								this.stompAuthentications.put(session.getId(), user);
							}
						}
						if (this.eventPublisher != null) {
							if (isConnect) {
								publishEvent(new SessionConnectEvent(this, message, getUser(session)));
							}
							else if (StompCommand.SUBSCRIBE.equals(command)) {
								publishEvent(new SessionSubscribeEvent(this, message, getUser(session)));
							}
							else if (StompCommand.UNSUBSCRIBE.equals(command)) {
								publishEvent(new SessionUnsubscribeEvent(this, message, getUser(session)));
							}
						}
					}
				}
				finally {
					SimpAttributesContextHolder.resetAttributes();
				}
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to send client message to application via MessageChannel" +
							" in session " + session.getId() + ". Sending STOMP ERROR to client.", ex);
				}
				handleError(session, ex, message);
			}
		}
	}

	private Principal getUser(WebSocketSession session) {
		Principal user = this.stompAuthentications.get(session.getId());
		return user != null ? user : session.getPrincipal();
	}

	@SuppressWarnings("deprecation")
	private void handleError(WebSocketSession session, Throwable ex, Message<byte[]> clientMessage) {
		if (getErrorHandler() == null) {
			sendErrorMessage(session, ex);
			return;
		}

		Message<byte[]> message = getErrorHandler().handleClientMessageProcessingError(clientMessage, ex);
		if (message == null) {
			return;
		}

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		Assert.state(accessor != null, "Expected STOMP headers");
		sendToClient(session, accessor, message.getPayload());
	}

	/**
	 * 没有配置{@link #setErrorHandler(StompSubProtocolErrorHandler) errorHandler}向客户端发送ERROR帧时调用.
	 * 
	 * @deprecated as of Spring 4.2, in favor of
	 * {@link #setErrorHandler(StompSubProtocolErrorHandler) configuring}
	 * a {@code StompSubProtocolErrorHandler}
	 */
	@Deprecated
	protected void sendErrorMessage(WebSocketSession session, Throwable error) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
		headerAccessor.setMessage(error.getMessage());

		byte[] bytes = this.stompEncoder.encode(headerAccessor.getMessageHeaders(), EMPTY_PAYLOAD);
		try {
			session.sendMessage(new TextMessage(bytes));
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			logger.debug("Failed to send STOMP ERROR to client", ex);
		}
	}

	private boolean detectImmutableMessageInterceptor(MessageChannel channel) {
		if (this.immutableMessageInterceptorPresent != null) {
			return this.immutableMessageInterceptorPresent;
		}

		if (channel instanceof AbstractMessageChannel) {
			for (ChannelInterceptor interceptor : ((AbstractMessageChannel) channel).getInterceptors()) {
				if (interceptor instanceof ImmutableMessageChannelInterceptor) {
					this.immutableMessageInterceptorPresent = true;
					return true;
				}
			}
		}
		this.immutableMessageInterceptorPresent = false;
		return false;
	}

	private void publishEvent(ApplicationEvent event) {
		try {
			this.eventPublisher.publishEvent(event);
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Error publishing " + event, ex);
			}
		}
	}

	/**
	 * 处理返回WebSocket客户端的STOMP消息.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {
		if (!(message.getPayload() instanceof byte[])) {
			if (logger.isErrorEnabled()) {
				logger.error("Expected byte[] payload. Ignoring " + message + ".");
			}
			return;
		}

		StompHeaderAccessor accessor = getStompHeaderAccessor(message);
		StompCommand command = accessor.getCommand();

		if (StompCommand.MESSAGE.equals(command)) {
			if (accessor.getSubscriptionId() == null && logger.isWarnEnabled()) {
				logger.warn("No STOMP \"subscription\" header in " + message);
			}
			String origDestination = accessor.getFirstNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
			if (origDestination != null) {
				accessor = toMutableAccessor(accessor, message);
				accessor.removeNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
				accessor.setDestination(origDestination);
			}
		}
		else if (StompCommand.CONNECTED.equals(command)) {
			this.stats.incrementConnectedCount();
			accessor = afterStompSessionConnected(message, accessor, session);
			if (this.eventPublisher != null && StompCommand.CONNECTED.equals(command)) {
				try {
					SimpAttributes simpAttributes = new SimpAttributes(session.getId(), session.getAttributes());
					SimpAttributesContextHolder.setAttributes(simpAttributes);
					Principal user = getUser(session);
					publishEvent(new SessionConnectedEvent(this, (Message<byte[]>) message, user));
				}
				finally {
					SimpAttributesContextHolder.resetAttributes();
				}
			}
		}

		byte[] payload = (byte[]) message.getPayload();
		if (StompCommand.ERROR.equals(command) && getErrorHandler() != null) {
			Message<byte[]> errorMessage = getErrorHandler().handleErrorMessageToClient((Message<byte[]>) message);
			accessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);
			Assert.state(accessor != null, "Expected STOMP headers");
			payload = errorMessage.getPayload();
		}
		sendToClient(session, accessor, payload);
	}

	private void sendToClient(WebSocketSession session, StompHeaderAccessor stompAccessor, byte[] payload) {
		StompCommand command = stompAccessor.getCommand();
		try {
			byte[] bytes = this.stompEncoder.encode(stompAccessor.getMessageHeaders(), payload);
			boolean useBinary = (payload.length > 0 && !(session instanceof SockJsSession) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(stompAccessor.getContentType()));
			if (useBinary) {
				session.sendMessage(new BinaryMessage(bytes));
			}
			else {
				session.sendMessage(new TextMessage(bytes));
			}
		}
		catch (SessionLimitExceededException ex) {
			// Bad session, just get out
			throw ex;
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to send WebSocket message to client in session " + session.getId(), ex);
			}
			command = StompCommand.ERROR;
		}
		finally {
			if (StompCommand.ERROR.equals(command)) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}
	}

	private StompHeaderAccessor getStompHeaderAccessor(Message<?> message) {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor instanceof StompHeaderAccessor) {
			return (StompHeaderAccessor) accessor;
		}
		else {
			StompHeaderAccessor stompAccessor = StompHeaderAccessor.wrap(message);
			SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
			if (SimpMessageType.CONNECT_ACK.equals(messageType)) {
				stompAccessor = convertConnectAcktoStompConnected(stompAccessor);
			}
			else if (SimpMessageType.DISCONNECT_ACK.equals(messageType)) {
				String receipt = getDisconnectReceipt(stompAccessor);
				if (receipt != null) {
					stompAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
					stompAccessor.setReceiptId(receipt);
				}
				else {
					stompAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
					stompAccessor.setMessage("Session closed.");
				}
			}
			else if (SimpMessageType.HEARTBEAT.equals(messageType)) {
				stompAccessor = StompHeaderAccessor.createForHeartbeat();
			}
			else if (stompAccessor.getCommand() == null || StompCommand.SEND.equals(stompAccessor.getCommand())) {
				stompAccessor.updateStompCommandAsServerMessage();
			}
			return stompAccessor;
		}
	}

	/**
	 * 简单代理生成{@code SimpMessageType.CONNECT_ACK}, 它不是特定于STOMP的, 需要转换为STOMP CONNECTED帧.
	 */
	private StompHeaderAccessor convertConnectAcktoStompConnected(StompHeaderAccessor connectAckHeaders) {
		String name = StompHeaderAccessor.CONNECT_MESSAGE_HEADER;
		Message<?> message = (Message<?>) connectAckHeaders.getHeader(name);
		if (message == null) {
			throw new IllegalStateException("Original STOMP CONNECT not found in " + connectAckHeaders);
		}

		StompHeaderAccessor connectHeaders = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaderAccessor connectedHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);

		Set<String> acceptVersions = connectHeaders.getAcceptVersion();
		if (acceptVersions.contains("1.2")) {
			connectedHeaders.setVersion("1.2");
		}
		else if (acceptVersions.contains("1.1")) {
			connectedHeaders.setVersion("1.1");
		}
		else if (!acceptVersions.isEmpty()) {
			throw new IllegalArgumentException("Unsupported STOMP version '" + acceptVersions + "'");
		}

		long[] heartbeat = (long[]) connectAckHeaders.getHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER);
		if (heartbeat != null) {
			connectedHeaders.setHeartbeat(heartbeat[0], heartbeat[1]);
		}
		else {
			connectedHeaders.setHeartbeat(0, 0);
		}

		return connectedHeaders;
	}

	private String getDisconnectReceipt(SimpMessageHeaderAccessor simpHeaders) {
		String name = StompHeaderAccessor.DISCONNECT_MESSAGE_HEADER;
		Message<?> message = (Message<?>) simpHeaders.getHeader(name);
		if (message != null) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			return accessor.getReceipt();
		}
		return null;
	}

	protected StompHeaderAccessor toMutableAccessor(StompHeaderAccessor headerAccessor, Message<?> message) {
		return (headerAccessor.isMutable() ? headerAccessor : StompHeaderAccessor.wrap(message));
	}

	@SuppressWarnings("deprecation")
	private StompHeaderAccessor afterStompSessionConnected(Message<?> message, StompHeaderAccessor accessor,
			WebSocketSession session) {

		Principal principal = getUser(session);
		if (principal != null) {
			accessor = toMutableAccessor(accessor, message);
			accessor.setNativeHeader(CONNECTED_USER_HEADER, principal.getName());
			if (this.userSessionRegistry != null) {
				String userName = getSessionRegistryUserName(principal);
				this.userSessionRegistry.registerSessionId(userName, session.getId());
			}
		}

		long[] heartbeat = accessor.getHeartbeat();
		if (heartbeat[1] > 0) {
			session = WebSocketSessionDecorator.unwrap(session);
			if (session instanceof SockJsSession) {
				((SockJsSession) session).disableHeartbeat();
			}
		}

		return accessor;
	}

	private String getSessionRegistryUserName(Principal principal) {
		String userName = principal.getName();
		if (principal instanceof DestinationUserNameProvider) {
			userName = ((DestinationUserNameProvider) principal).getDestinationUserName();
		}
		return userName;
	}

	@Override
	public String resolveSessionId(Message<?> message) {
		return SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) {
		if (session.getTextMessageSizeLimit() < MINIMUM_WEBSOCKET_MESSAGE_SIZE) {
			session.setTextMessageSizeLimit(MINIMUM_WEBSOCKET_MESSAGE_SIZE);
		}
		this.decoders.put(session.getId(), new BufferingStompDecoder(this.stompDecoder, getMessageSizeLimit()));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel) {
		this.decoders.remove(session.getId());

		Principal principal = getUser(session);
		if (principal != null && this.userSessionRegistry != null) {
			String userName = getSessionRegistryUserName(principal);
			this.userSessionRegistry.unregisterSessionId(userName, session.getId());
		}

		Message<byte[]> message = createDisconnectMessage(session);
		SimpAttributes simpAttributes = SimpAttributes.fromMessage(message);
		try {
			SimpAttributesContextHolder.setAttributes(simpAttributes);
			if (this.eventPublisher != null) {
				Principal user = getUser(session);
				publishEvent(new SessionDisconnectEvent(this, message, session.getId(), closeStatus, user));
			}
			outputChannel.send(message);
		}
		finally {
			this.stompAuthentications.remove(session.getId());
			SimpAttributesContextHolder.resetAttributes();
			simpAttributes.sessionCompleted();
		}
	}

	private Message<byte[]> createDisconnectMessage(WebSocketSession session) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
		headerAccessor.setSessionId(session.getId());
		headerAccessor.setSessionAttributes(session.getAttributes());
		headerAccessor.setUser(getUser(session));
		return MessageBuilder.createMessage(EMPTY_PAYLOAD, headerAccessor.getMessageHeaders());
	}

	@Override
	public String toString() {
		return "StompSubProtocolHandler" + getSupportedProtocols();
	}


	private static class Stats {

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
			return "processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")";
		}
	}

}
