package org.springframework.messaging.converter;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link SimpleMessageConverter}的扩展, 使用{@link ConversionService}将消息的有效负载转换为请求的类型.
 *
 * <p>如果转换服务无法从有效负载类型转换为请求的类型, 则返回{@code null}.
 */
public class GenericMessageConverter extends SimpleMessageConverter {

	private final ConversionService conversionService;


	public GenericMessageConverter() {
		this.conversionService = DefaultConversionService.getSharedInstance();
	}

	public GenericMessageConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}


	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		Object payload = message.getPayload();
		if (targetClass == null) {
			return payload;
		}
		if (payload != null && this.conversionService.canConvert(payload.getClass(), targetClass)) {
			try {
				return this.conversionService.convert(payload, targetClass);
			}
			catch (ConversionException ex) {
				throw new MessageConversionException(message, "Failed to convert message payload '" +
						payload + "' to '" + targetClass.getName() + "'", ex);
			}
		}
		return (ClassUtils.isAssignableValue(targetClass, payload) ? payload : null);
	}

}
