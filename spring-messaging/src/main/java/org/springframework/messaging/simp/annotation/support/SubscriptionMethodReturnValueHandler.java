package org.springframework.messaging.simp.annotation.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;

/**
 * 用于直接回复订阅的{@code HandlerMethodReturnValueHandler}.
 * 使用{@link org.springframework.messaging.simp.annotation.SubscribeMapping SubscribeMapping}注解的方法支持它,
 * 以便将返回值视为在会话上直接发送回的响应.
 * 这允许客户端实现请求-响应模式, 并使用它, 例如在初始化时获得一些数据.
 *
 * <p>从方法返回的值被转换为{@link Message}, 然后使用sessionId, subscriptionId和输入消息的目标进行丰富.
 *
 * <p><strong>Note:</strong>
 * 可以通过使用{@link SendTo}或{@link SendToUser}注解来覆盖解释来自{@code @SubscribeMapping}方法的返回值的默认行为,
 * 在这种情况下, 将准备消息并将其发送给代理.
 */
public class SubscriptionMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final Log logger = LogFactory.getLog(SubscriptionMethodReturnValueHandler.class);


	private final MessageSendingOperations<String> messagingTemplate;

	private MessageHeaderInitializer headerInitializer;


	/**
	 * @param template 用于发送消息的消息模板, 很可能是"clientOutboundChannel" (不能是{@code null})
	 */
	public SubscriptionMethodReturnValueHandler(MessageSendingOperations<String> template) {
		Assert.notNull(template, "messagingTemplate must not be null");
		this.messagingTemplate = template;
	}


	/**
	 * 配置{@link MessageHeaderInitializer}以应用于发送到客户端出站频道的所有消息的header.
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
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(SubscribeMapping.class) &&
				!returnType.hasMethodAnnotation(SendTo.class) &&
				!returnType.hasMethodAnnotation(SendToUser.class));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		String destination = SimpMessageHeaderAccessor.getDestination(headers);

		if (subscriptionId == null) {
			throw new IllegalStateException("No subscription id in " + message +
					" returned by: " + returnType.getMethod());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Reply to @SubscribeMapping: " + returnValue);
		}
		MessageHeaders headersToSend = createHeaders(sessionId, subscriptionId, returnType);
		this.messagingTemplate.convertAndSend(destination, returnValue, headersToSend);
	}

	private MessageHeaders createHeaders(String sessionId, String subscriptionId, MethodParameter returnType) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
		accessor.setSessionId(sessionId);
		accessor.setSubscriptionId(subscriptionId);
		accessor.setHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER, returnType);
		accessor.setLeaveMutable(true);
		return accessor.getMessageHeaders();
	}

}
