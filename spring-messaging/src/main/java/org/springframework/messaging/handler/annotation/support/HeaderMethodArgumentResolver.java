package org.springframework.messaging.handler.annotation.support;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;

/**
 * 解析使用{@link Header @Header}注解的方法参数.
 */
public class HeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	private static final Log logger = LogFactory.getLog(HeaderMethodArgumentResolver.class);


	public HeaderMethodArgumentResolver(ConversionService cs, ConfigurableBeanFactory beanFactory) {
		super(cs, beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Header.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Header annotation = parameter.getParameterAnnotation(Header.class);
		return new HeaderNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name)
			throws Exception {

		Object headerValue = message.getHeaders().get(name);
		Object nativeHeaderValue = getNativeHeaderValue(message, name);

		if (headerValue != null && nativeHeaderValue != null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Message headers contain two values for the same header '" + name + "', " +
						"one in the top level header map and a second in the nested map with native headers. " +
						"Using the value from top level map. " +
						"Use 'nativeHeader.myHeader' to resolve to the value from the nested native header map." );
			}
		}

		return (headerValue != null ? headerValue : nativeHeaderValue);
	}

	private Object getNativeHeaderValue(Message<?> message, String name) {
		Map<String, List<String>> nativeHeaders = getNativeHeaders(message);
		if (name.startsWith("nativeHeaders.")) {
			name = name.substring("nativeHeaders.".length());
		}
		if (nativeHeaders == null || !nativeHeaders.containsKey(name)) {
			return null;
		}
		List<?> nativeHeaderValues = nativeHeaders.get(name);
		return (nativeHeaderValues.size() == 1 ? nativeHeaderValues.get(0) : nativeHeaderValues);
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> getNativeHeaders(Message<?> message) {
		return (Map<String, List<String>>) message.getHeaders().get(
				NativeMessageHeaderAccessor.NATIVE_HEADERS);
	}

	@Override
	protected void handleMissingValue(String headerName, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing header '" + headerName +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static class HeaderNamedValueInfo extends NamedValueInfo {

		private HeaderNamedValueInfo(Header annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
