package org.springframework.messaging.handler.annotation.support;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.ValueConstants;

/**
 * 解析使用{@link org.springframework.messaging.handler.annotation.DestinationVariable @DestinationVariable}注解的方法参数.
 */
public class DestinationVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public static final String DESTINATION_TEMPLATE_VARIABLES_HEADER =
			DestinationVariableMethodArgumentResolver.class.getSimpleName() + ".templateVariables";


	public DestinationVariableMethodArgumentResolver(ConversionService cs) {
		super(cs, null);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(DestinationVariable.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		DestinationVariable annotation = parameter.getParameterAnnotation(DestinationVariable.class);
		return new DestinationVariableNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name)
			throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, String> vars =
				(Map<String, String>) message.getHeaders().get(DESTINATION_TEMPLATE_VARIABLES_HEADER);
		return (vars != null ? vars.get(name) : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter, Message<?> message) {
		throw new MessageHandlingException(message, "Missing path template variable '" + name +
				"' for method parameter type [" + parameter.getParameterType() + "]");
	}


	private static class DestinationVariableNamedValueInfo extends NamedValueInfo {

		private DestinationVariableNamedValueInfo(DestinationVariable annotation) {
			super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
		}
	}

}
