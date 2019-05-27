package org.springframework.messaging.simp.user;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code MessageHandler}, 支持"user"目标.
 *
 * <p>监听具有"user"目标的消息, 将其目标转换为用户的活动会话中唯一的实际目标, 然后将已解析的消息发送到要传递的代理通道.
 */
public class UserDestinationMessageHandler implements MessageHandler, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(UserDestinationMessageHandler.class);


	private final SubscribableChannel clientInboundChannel;

	private final SubscribableChannel brokerChannel;

	private final UserDestinationResolver destinationResolver;

	private final MessageSendingOperations<String> messagingTemplate;

	private BroadcastHandler broadcastHandler;

	private MessageHeaderInitializer headerInitializer;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	/**
	 * @param clientInboundChannel 从客户端收到的消息
	 * @param brokerChannel 要发送到的代理.
	 * @param resolver "user"目标的解析器.
	 */
	public UserDestinationMessageHandler(SubscribableChannel clientInboundChannel,
			SubscribableChannel brokerChannel, UserDestinationResolver resolver) {

		Assert.notNull(clientInboundChannel, "'clientInChannel' must not be null");
		Assert.notNull(brokerChannel, "'brokerChannel' must not be null");
		Assert.notNull(resolver, "resolver must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.brokerChannel = brokerChannel;
		this.messagingTemplate = new SimpMessagingTemplate(brokerChannel);
		this.destinationResolver = resolver;
	}


	/**
	 * 返回配置的{@link UserDestinationResolver}.
	 */
	public UserDestinationResolver getUserDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * 设置目标以广播尚未解析的消息, 因为用户未连接.
	 * 在多应用程序服务器场景中, 这为其他应用程序服务器提供了尝试的机会.
	 * <p>默认不设置.
	 * 
	 * @param destination 目标.
	 */
	public void setBroadcastDestination(String destination) {
		this.broadcastHandler = (StringUtils.hasText(destination) ?
				new BroadcastHandler(this.messagingTemplate, destination) : null);
	}

	/**
	 * 返回未解析消息已配置的目标.
	 */
	public String getBroadcastDestination() {
		return (this.broadcastHandler != null ? this.broadcastHandler.getBroadcastDestination() : null);
	}

	/**
	 * 返回用于将已解析消息发送到代理通道的消息模板.
	 */
	public MessageSendingOperations<String> getBrokerMessagingTemplate() {
		return this.messagingTemplate;
	}

	/**
	 * 配置自定义{@link MessageHeaderInitializer}以初始化已解析的目标消息的header.
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


	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.brokerChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
			this.brokerChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		if (this.broadcastHandler != null) {
			message = this.broadcastHandler.preHandle(message);
			if (message == null) {
				return;
			}
		}
		UserDestinationResult result = this.destinationResolver.resolveDestination(message);
		if (result == null) {
			return;
		}
		if (result.getTargetDestinations().isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No active sessions for user destination: " + result.getSourceDestination());
			}
			if (this.broadcastHandler != null) {
				this.broadcastHandler.handleUnresolved(message);
			}
			return;
		}
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
		initHeaders(accessor);
		accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, result.getSubscribeDestination());
		accessor.setLeaveMutable(true);
		message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Translated " + result.getSourceDestination() + " -> " + result.getTargetDestinations());
		}
		for (String target : result.getTargetDestinations()) {
			this.messagingTemplate.send(target, message);
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor headerAccessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
	}

	@Override
	public String toString() {
		return "UserDestinationMessageHandler[" + this.destinationResolver + "]";
	}


	/**
	 * 处理器, 它将本地未解析的消息广播到代理, 并处理从代理接收的类似广播.
	 */
	private static class BroadcastHandler {

		private static final List<String> NO_COPY_LIST = Arrays.asList("subscription", "message-id");

		private final MessageSendingOperations<String> messagingTemplate;

		private final String broadcastDestination;

		public BroadcastHandler(MessageSendingOperations<String> template, String destination) {
			this.messagingTemplate = template;
			this.broadcastDestination = destination;
		}

		public String getBroadcastDestination() {
			return this.broadcastDestination;
		}

		public Message<?> preHandle(Message<?> message) throws MessagingException {
			String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
			if (!getBroadcastDestination().equals(destination)) {
				return message;
			}
			SimpMessageHeaderAccessor accessor =
					SimpMessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);
			if (accessor.getSessionId() == null) {
				// Our own broadcast
				return null;
			}
			destination = accessor.getFirstNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
			if (logger.isTraceEnabled()) {
				logger.trace("Checking unresolved user destination: " + destination);
			}
			SimpMessageHeaderAccessor newAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			for (String name : accessor.toNativeHeaderMap().keySet()) {
				if (NO_COPY_LIST.contains(name)) {
					continue;
				}
				newAccessor.setNativeHeader(name, accessor.getFirstNativeHeader(name));
			}
			newAccessor.setDestination(destination);
			newAccessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true); // ensure send doesn't block
			return MessageBuilder.createMessage(message.getPayload(), newAccessor.getMessageHeaders());
		}

		public void handleUnresolved(Message<?> message) {
			MessageHeaders headers = message.getHeaders();
			if (SimpMessageHeaderAccessor.getFirstNativeHeader(
					SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, headers) != null) {
				// Re-broadcast
				return;
			}
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
			String destination = accessor.getDestination();
			accessor.setNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION, destination);
			accessor.setLeaveMutable(true);
			message = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Translated " + destination + " -> " + getBroadcastDestination());
			}
			this.messagingTemplate.send(getBroadcastDestination(), message);
		}
	}

}
