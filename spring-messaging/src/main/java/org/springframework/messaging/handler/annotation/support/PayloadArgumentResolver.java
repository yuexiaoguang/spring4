package org.springframework.messaging.handler.annotation.support;

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * 使用{@link MessageConverter}提取和转换消息有效负载的解析器.
 * 如果参数使用验证注解, 它还使用{@link Validator}验证有效负载.
 *
 * <p>这个{@link HandlerMethodArgumentResolver}应该在最后, 因为它支持所有类型, 不需要{@link Payload}注解.
 */
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	private final Validator validator;

	private final boolean useDefaultResolution;


	/**
	 * @param messageConverter 要使用的MessageConverter (必须)
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * @param messageConverter 要使用的MessageConverter (必须)
	 * @param validator 要使用的 Validator (可选)
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
		this(messageConverter, validator, true);
	}

	/**
	 * @param messageConverter 要使用的MessageConverter (必须)
	 * @param validator 要使用的 Validator (可选)
	 * @param useDefaultResolution 如果是"true" (默认), 此解析器支持所有参数;
	 * 如果为"false", 则仅支持带有{@code @Payload}注解的参数.
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, Validator validator,
			boolean useDefaultResolution) {

		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
		this.validator = validator;
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(Payload.class) || this.useDefaultResolution);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Payload ann = parameter.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.expression())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver");
		}

		Object payload = message.getPayload();
		if (isEmptyPayload(payload)) {
			if (ann == null || ann.required()) {
				String paramName = getParameterName(parameter);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload, paramName);
				bindingResult.addError(new ObjectError(paramName, "Payload value must not be empty"));
				throw new MethodArgumentNotValidException(message, parameter, bindingResult);
			}
			else {
				return null;
			}
		}

		Class<?> targetClass = parameter.getParameterType();
		Class<?> payloadClass = payload.getClass();
		if (ClassUtils.isAssignable(targetClass, payloadClass)) {
			validate(message, parameter, payload);
			return payload;
		}
		else {
			if (this.converter instanceof SmartMessageConverter) {
				SmartMessageConverter smartConverter = (SmartMessageConverter) this.converter;
				payload = smartConverter.fromMessage(message, targetClass, parameter);
			}
			else {
				payload = this.converter.fromMessage(message, targetClass);
			}
			if (payload == null) {
				throw new MessageConversionException(message, "Cannot convert from [" +
						payloadClass.getName() + "] to [" + targetClass.getName() + "] for " + message);
			}
			validate(message, parameter, payload);
			return payload;
		}
	}

	private String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return (paramName != null ? paramName : "Arg " + param.getParameterIndex());
	}

	/**
	 * 指定给定的{@code payload}是否为空.
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

	/**
	 * 如果适用, 验证有效负载.
	 * <p>默认实现检查{@code @javax.validation.Valid}, Spring的{@link org.springframework.validation.annotation.Validated},
	 * 以及名称以"Valid"开头的自定义注解.
	 * 
	 * @param message 当前处理的消息
	 * @param parameter 方法参数
	 * @param target 目标有效负载对象
	 * 
	 * @throws MethodArgumentNotValidException 绑定错误
	 */
	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				BeanPropertyBindingResult bindingResult =
						new BeanPropertyBindingResult(target, getParameterName(parameter));
				if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
				}
				else {
					this.validator.validate(target, bindingResult);
				}
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter, bindingResult);
				}
				break;
			}
		}
	}
}
