package org.springframework.messaging.simp;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.messaging.simp.SimpMessageSendingOperations}的实现.
 *
 * <p>还提供了向用户发送消息的方法.
 * 有关用户目标的更多信息,
 * 请参阅{@link org.springframework.messaging.simp.user.UserDestinationResolver UserDestinationResolver}.
 */
public class SimpMessagingTemplate extends AbstractMessageSendingTemplate<String>
		implements SimpMessageSendingOperations {

	private final MessageChannel messageChannel;

	private String destinationPrefix = "/user/";

	private volatile long sendTimeout = -1;

	private MessageHeaderInitializer headerInitializer;


	/**
	 * @param messageChannel 消息频道 (never {@code null})
	 */
	public SimpMessagingTemplate(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "MessageChannel must not be null");
		this.messageChannel = messageChannel;
	}


	/**
	 * 返回配置的消息频道.
	 */
	public MessageChannel getMessageChannel() {
		return this.messageChannel;
	}

	/**
	 * 配置用于针对特定用户的目标的前缀.
	 * <p>默认 "/user/".
	 */
	public void setUserDestinationPrefix(String prefix) {
		Assert.hasText(prefix, "User destination prefix must not be empty");
		this.destinationPrefix = (prefix.endsWith("/") ? prefix : prefix + "/");

	}

	/**
	 * 返回配置的用户目标前缀.
	 */
	public String getUserDestinationPrefix() {
		return this.destinationPrefix;
	}

	/**
	 * 指定用于发送操作的超时值 (以毫秒为单位).
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * 返回配置的发送超时 (以毫秒为单位).
	 */
	public long getSendTimeout() {
		return this.sendTimeout;
	}

	/**
	 * 配置{@link MessageHeaderInitializer}, 应用于通过{@code SimpMessagingTemplate}创建的所有消息的header.
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


	/**
	 * 如果给定消息的header已包含
	 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor#DESTINATION_HEADER SimpMessageHeaderAccessor#DESTINATION_HEADER},
	 * 则会发送消息而不进行进一步更改.
	 * <p>如果目标header尚不存在, 则将消息发送到已配置的{@link #setDefaultDestination(Object) 默认目标},
	 * 或者如果未配置, 则会引发{@code IllegalStateException}异常.
	 * 
	 * @param message 要发送的消息 (never {@code null})
	 */
	@Override
	public void send(Message<?> message) {
		Assert.notNull(message, "Message is required");
		String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		if (destination != null) {
			sendInternal(message);
			return;
		}
		doSend(getRequiredDefaultDestination(), message);
	}

	@Override
	protected void doSend(String destination, Message<?> message) {
		Assert.notNull(destination, "Destination must not be null");

		SimpMessageHeaderAccessor simpAccessor =
				MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

		if (simpAccessor != null) {
			if (simpAccessor.isMutable()) {
				simpAccessor.setDestination(destination);
				simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
				simpAccessor.setImmutable();
				sendInternal(message);
				return;
			}
			else {
				// 尝试并保留原始访问者类型
				simpAccessor = (SimpMessageHeaderAccessor) MessageHeaderAccessor.getMutableAccessor(message);
				initHeaders(simpAccessor);
			}
		}
		else {
			simpAccessor = SimpMessageHeaderAccessor.wrap(message);
			initHeaders(simpAccessor);
		}

		simpAccessor.setDestination(destination);
		simpAccessor.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
		message = MessageBuilder.createMessage(message.getPayload(), simpAccessor.getMessageHeaders());
		sendInternal(message);
	}

	private void sendInternal(Message<?> message) {
		String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
		Assert.notNull(destination, "Destination header required");

		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0 ? this.messageChannel.send(message, timeout) : this.messageChannel.send(message));

		if (!sent) {
			throw new MessageDeliveryException(message,
					"Failed to send message to destination '" + destination + "' within timeout: " + timeout);
		}
	}

	private void initHeaders(SimpMessageHeaderAccessor simpAccessor) {
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(simpAccessor);
		}
	}


	@Override
	public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
		convertAndSendToUser(user, destination, payload, (MessagePostProcessor) null);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload,
			Map<String, Object> headers) throws MessagingException {

		convertAndSendToUser(user, destination, payload, headers, null);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload,
			MessagePostProcessor postProcessor) throws MessagingException {

		convertAndSendToUser(user, destination, payload, null, postProcessor);
	}

	@Override
	public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException {

		Assert.notNull(user, "User must not be null");
		user = StringUtils.replace(user, "/", "%2F");
		destination = destination.startsWith("/") ? destination : "/" + destination;
		super.convertAndSend(this.destinationPrefix + user + destination, payload, headers, postProcessor);
	}


	/**
	 * 创建一个Map, 并将给定的header放在键
	 * {@link org.springframework.messaging.support.NativeMessageHeaderAccessor#NATIVE_HEADERS NATIVE_HEADERS NATIVE_HEADERS NATIVE_HEADERS}下.
	 * 有效地将输入header Map视为要发送到目标的header.
	 * <p>但是, 如果给定header已包含键{@code NATIVE_HEADERS NATIVE_HEADERS}, 则返回相同的header实例而不进行更改.
	 * <p>此外, 如果使用{@link SimpMessageHeaderAccessor#getMessageHeaders()}准备并获得给定的header, 则返回相同的header实例而不进行更改.
	 */
	@Override
	protected Map<String, Object> processHeadersToSend(Map<String, Object> headers) {
		if (headers == null) {
			SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			initHeaders(headerAccessor);
			headerAccessor.setLeaveMutable(true);
			return headerAccessor.getMessageHeaders();
		}
		if (headers.containsKey(NativeMessageHeaderAccessor.NATIVE_HEADERS)) {
			return headers;
		}
		if (headers instanceof MessageHeaders) {
			SimpMessageHeaderAccessor accessor =
					MessageHeaderAccessor.getAccessor((MessageHeaders) headers, SimpMessageHeaderAccessor.class);
			if (accessor != null) {
				return headers;
			}
		}

		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		initHeaders(headerAccessor);
		for (Map.Entry<String, Object> headerEntry : headers.entrySet()) {
			Object value = headerEntry.getValue();
			headerAccessor.setNativeHeader(headerEntry.getKey(), (value != null ? value.toString() : null));
		}
		return headerAccessor.getMessageHeaders();
	}

}
