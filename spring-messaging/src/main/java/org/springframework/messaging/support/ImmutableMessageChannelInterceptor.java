package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * 一个更简单的拦截器, 它在通过preSend方法传递的消息header上调用{@link MessageHeaderAccessor#setImmutable()}.
 *
 * <p>当配置为链中的最后一个拦截器时, 它允许发送消息的组件留下可变的header,
 * 以便拦截器在实际发送消息之前进行修改, 并公开以并发访问.
 */
public class ImmutableMessageChannelInterceptor extends ChannelInterceptorAdapter {

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor != null && accessor.isMutable()) {
			accessor.setImmutable();
		}
		return message;
	}

}
