package org.springframework.messaging.simp.broker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link SubscriptionRegistry}实现的抽象基类, 用于查找消息中的信息, 但委托给实际存储和检索的抽象方法.
 */
public abstract class AbstractSubscriptionRegistry implements SubscriptionRegistry {

	private static final MultiValueMap<String, String> EMPTY_MAP =
			CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<String, String>(0));

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public final void registerSubscription(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.SUBSCRIBE.equals(messageType)) {
			throw new IllegalArgumentException("Expected SUBSCRIBE: " + message);
		}

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No sessionId in  " + message);
			}
			return;
		}

		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		if (subscriptionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No subscriptionId in " + message);
			}
			return;
		}

		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No destination in " + message);
			}
			return;
		}

		addSubscriptionInternal(sessionId, subscriptionId, destination, message);
	}

	@Override
	public final void unregisterSubscription(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			throw new IllegalArgumentException("Expected UNSUBSCRIBE: " + message);
		}

		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No sessionId in " + message);
			}
			return;
		}

		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		if (subscriptionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No subscriptionId " + message);
			}
			return;
		}

		removeSubscriptionInternal(sessionId, subscriptionId, message);
	}

	@Override
	public final MultiValueMap<String, String> findSubscriptions(Message<?> message) {
		MessageHeaders headers = message.getHeaders();

		SimpMessageType type = SimpMessageHeaderAccessor.getMessageType(headers);
		if (!SimpMessageType.MESSAGE.equals(type)) {
			throw new IllegalArgumentException("Unexpected message type: " + type);
		}

		String destination = SimpMessageHeaderAccessor.getDestination(headers);
		if (destination == null) {
			if (logger.isErrorEnabled()) {
				logger.error("No destination in " + message);
			}
			return EMPTY_MAP;
		}

		return findSubscriptionsInternal(destination, message);
	}


	protected abstract void addSubscriptionInternal(
			String sessionId, String subscriptionId, String destination, Message<?> message);

	protected abstract void removeSubscriptionInternal(
			String sessionId, String subscriptionId, Message<?> message);

	protected abstract MultiValueMap<String, String> findSubscriptionsInternal(
			String destination, Message<?> message);

}
