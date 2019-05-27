package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于{@link Message}方法参数的{@code HandlerMethodArgumentResolver}.
 * 验证有效负载的泛型类型是否与消息值匹配, 或以其他方式应用{@link MessageConverter}以转换为预期的有效负载类型.
 */
public class MessageMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;


	/**
	 * 创建没有消息转换的默认解析器实例.
	 */
	public MessageMethodArgumentResolver() {
		this(null);
	}

	/**
	 * @param converter 要使用的MessageConverter (may be {@code null})
	 */
	public MessageMethodArgumentResolver(MessageConverter converter) {
		this.converter = converter;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Message.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Class<?> targetMessageType = parameter.getParameterType();
		Class<?> targetPayloadType = getPayloadType(parameter);

		if (!targetMessageType.isAssignableFrom(message.getClass())) {
			throw new MethodArgumentTypeMismatchException(message, parameter, "Actual message type '" +
					ClassUtils.getDescriptiveType(message) + "' does not match expected type '" +
					ClassUtils.getQualifiedName(targetMessageType) + "'");
		}

		Object payload = message.getPayload();
		if (payload == null || targetPayloadType.isInstance(payload)) {
			return message;
		}

		if (isEmptyPayload(payload)) {
			throw new MessageConversionException(message, "Cannot convert from actual payload type '" +
					ClassUtils.getDescriptiveType(payload) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "' when payload is empty");
		}

		payload = convertPayload(message, parameter, targetPayloadType);
		return MessageBuilder.createMessage(payload, message.getHeaders());
	}

	private Class<?> getPayloadType(MethodParameter parameter) {
		Type genericParamType = parameter.getGenericParameterType();
		ResolvableType resolvableType = ResolvableType.forType(genericParamType).as(Message.class);
		return resolvableType.getGeneric(0).resolve(Object.class);
	}

	/**
	 * 检查给定的{@code payload}是否为空.
	 * 
	 * @param payload 要检查的有效负载 (can be {@code null})
	 */
	protected boolean isEmptyPayload(Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[]) {
			return ((byte[]) payload).length == 0;
		}
		else if (payload instanceof String) {
			return !StringUtils.hasText((String) payload);
		}
		else {
			return false;
		}
	}

	private Object convertPayload(Message<?> message, MethodParameter parameter, Class<?> targetPayloadType) {
		Object result = null;
		if (this.converter instanceof SmartMessageConverter) {
			SmartMessageConverter smartConverter = (SmartMessageConverter) this.converter;
			result = smartConverter.fromMessage(message, targetPayloadType, parameter);
		}
		else if (this.converter != null) {
			result = this.converter.fromMessage(message, targetPayloadType);
		}

		if (result == null) {
			throw new MessageConversionException(message, "No converter found from actual payload type '" +
					ClassUtils.getDescriptiveType(message.getPayload()) + "' to expected payload type '" +
					ClassUtils.getQualifiedName(targetPayloadType) + "'");
		}
		return result;
	}

}
