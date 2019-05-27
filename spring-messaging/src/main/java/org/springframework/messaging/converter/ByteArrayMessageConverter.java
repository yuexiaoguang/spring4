package org.springframework.messaging.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

/**
 * {@link MessageConverter}, 支持MIME类型"application/octet-stream", 其中有效负载与 byte[]相互转换.
 */
public class ByteArrayMessageConverter extends AbstractMessageConverter {

	public ByteArrayMessageConverter() {
		super(MimeTypeUtils.APPLICATION_OCTET_STREAM);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return (byte[].class == clazz);
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		return message.getPayload();
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		return payload;
	}

}
