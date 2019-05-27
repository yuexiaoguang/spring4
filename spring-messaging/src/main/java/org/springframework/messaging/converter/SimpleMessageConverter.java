package org.springframework.messaging.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.ClassUtils;

/**
 * 一个简单的转换器, 只要它与预期的目标类匹配, 就可以简单地解包消息有效负载.
 * 或者相反, 只需将有效负载包装在消息中.
 *
 * <p>请注意, 此转换器会忽略消息header中可能存在的任何内容类型信息,
 * 如果实际需要进行有效负载转换, 则不应使用该信息.
 */
public class SimpleMessageConverter implements MessageConverter {

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		if (targetClass == null) {
			return payload;
		}
		return (ClassUtils.isAssignableValue(targetClass, payload) ? payload : null);
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		if (payload == null) {
			return null;
		}
		if (headers != null) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
			if (accessor != null && accessor.isMutable()) {
				return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
			}
		}
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

}
