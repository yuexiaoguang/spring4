package org.springframework.messaging.simp.annotation.support;

import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringUtils;

/**
 * 用于发送到{@link SendTo}或{@link SendToUser}方法级注解中指定的目标的{@link HandlerMethodReturnValueHandler}.
 *
 * <p>该方法返回的值将被转换为{@link Message}, 并通过提供的{@link MessageChannel}发送.
 * 然后使用输入消息的会话ID以及注解中的目标来丰富消息.
 * 如果指定了多个目标, 则会将消息的副本发送到每个目标.
 */
public class SendToMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final SimpMessageSendingOperations messagingTemplate;

	private final boolean annotationRequired;

	private String defaultDestinationPrefix = "/topic";

	private String defaultUserDestinationPrefix = "/queue";

	private PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("{", "}", null, false);

	private MessageHeaderInitializer headerInitializer;


	public SendToMethodReturnValueHandler(SimpMessageSendingOperations messagingTemplate, boolean annotationRequired) {
		Assert.notNull(messagingTemplate, "'messagingTemplate' must not be null");
		this.messagingTemplate = messagingTemplate;
		this.annotationRequired = annotationRequired;
	}


	/**
	 * 如果方法未使用{@link SendTo @SendTo}注解或未通过注解的value属性指定任何目标, 请配置默认前缀以添加到消息目标.
	 * <p>默认前缀为 "/topic".
	 */
	public void setDefaultDestinationPrefix(String defaultDestinationPrefix) {
		this.defaultDestinationPrefix = defaultDestinationPrefix;
	}

	/**
	 * 返回配置的默认目标前缀.
	 */
	public String getDefaultDestinationPrefix() {
		return this.defaultDestinationPrefix;
	}

	/**
	 * 在使用{@link SendToUser @SendToUser}注解方法但未通过注解的value属性指定任何目标的情况下, 配置要添加到消息目标的默认前缀.
	 * <p>默认前缀为 "/queue".
	 */
	public void setDefaultUserDestinationPrefix(String prefix) {
		this.defaultUserDestinationPrefix = prefix;
	}

	/**
	 * 返回配置的默认用户目标前缀.
	 */
	public String getDefaultUserDestinationPrefix() {
		return this.defaultUserDestinationPrefix;
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
		return (returnType.hasMethodAnnotation(SendTo.class) ||
				AnnotatedElementUtils.hasAnnotation(returnType.getDeclaringClass(), SendTo.class) ||
				returnType.hasMethodAnnotation(SendToUser.class) ||
				AnnotatedElementUtils.hasAnnotation(returnType.getDeclaringClass(), SendToUser.class) ||
				!this.annotationRequired);
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		PlaceholderResolver varResolver = initVarResolver(headers);
		Object annotation = findAnnotation(returnType);

		if (annotation instanceof SendToUser) {
			SendToUser sendToUser = (SendToUser) annotation;
			boolean broadcast = sendToUser.broadcast();
			String user = getUserName(message, headers);
			if (user == null) {
				if (sessionId == null) {
					throw new MissingSessionUserException(message);
				}
				user = sessionId;
				broadcast = false;
			}
			String[] destinations = getTargetDestinations(sendToUser, message, this.defaultUserDestinationPrefix);
			for (String destination : destinations) {
				destination = this.placeholderHelper.replacePlaceholders(destination, varResolver);
				if (broadcast) {
					this.messagingTemplate.convertAndSendToUser(
							user, destination, returnValue, createHeaders(null, returnType));
				}
				else {
					this.messagingTemplate.convertAndSendToUser(
							user, destination, returnValue, createHeaders(sessionId, returnType));
				}
			}
		}
		else {
			SendTo sendTo = (SendTo) annotation;  // possibly null
			String[] destinations = getTargetDestinations(sendTo, message, this.defaultDestinationPrefix);
			for (String destination : destinations) {
				destination = this.placeholderHelper.replacePlaceholders(destination, varResolver);
				this.messagingTemplate.convertAndSend(destination, returnValue, createHeaders(sessionId, returnType));
			}
		}
	}

	private Object findAnnotation(MethodParameter returnType) {
		Annotation[] anns = new Annotation[4];
		anns[0] = AnnotatedElementUtils.findMergedAnnotation(returnType.getMethod(), SendToUser.class);
		anns[1] = AnnotatedElementUtils.findMergedAnnotation(returnType.getMethod(), SendTo.class);
		anns[2] = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendToUser.class);
		anns[3] = AnnotatedElementUtils.findMergedAnnotation(returnType.getDeclaringClass(), SendTo.class);

		if (anns[0] != null && !ObjectUtils.isEmpty(((SendToUser) anns[0]).value())) {
			return anns[0];
		}
		if (anns[1] != null && !ObjectUtils.isEmpty(((SendTo) anns[1]).value())) {
			return anns[1];
		}
		if (anns[2] != null && !ObjectUtils.isEmpty(((SendToUser) anns[2]).value())) {
			return anns[2];
		}
		if (anns[3] != null && !ObjectUtils.isEmpty(((SendTo) anns[3]).value())) {
			return anns[3];
		}

		for (int i=0; i < 4; i++) {
			if (anns[i] != null) {
				return anns[i];
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private PlaceholderResolver initVarResolver(MessageHeaders headers) {
		String name = DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER;
		Map<String, String> vars = (Map<String, String>) headers.get(name);
		return new DestinationVariablePlaceholderResolver(vars);
	}

	protected String getUserName(Message<?> message, MessageHeaders headers) {
		Principal principal = SimpMessageHeaderAccessor.getUser(headers);
		if (principal != null) {
			return (principal instanceof DestinationUserNameProvider ?
					((DestinationUserNameProvider) principal).getDestinationUserName() : principal.getName());
		}
		return null;
	}

	protected String[] getTargetDestinations(Annotation annotation, Message<?> message, String defaultPrefix) {
		if (annotation != null) {
			String[] value = (String[]) AnnotationUtils.getValue(annotation);
			if (!ObjectUtils.isEmpty(value)) {
				return value;
			}
		}
		String name = DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER;
		String destination = (String) message.getHeaders().get(name);
		if (!StringUtils.hasText(destination)) {
			throw new IllegalStateException("No lookup destination header in " + message);
		}

		return (destination.startsWith("/") ?
				new String[] {defaultPrefix + destination} : new String[] {defaultPrefix + '/' + destination});
	}

	private MessageHeaders createHeaders(String sessionId, MethodParameter returnType) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}
		if (sessionId != null) {
			headerAccessor.setSessionId(sessionId);
		}
		headerAccessor.setHeader(SimpMessagingTemplate.CONVERSION_HINT_HEADER, returnType);
		headerAccessor.setLeaveMutable(true);
		return headerAccessor.getMessageHeaders();
	}


	@Override
	public String toString() {
		return "SendToMethodReturnValueHandler [annotationRequired=" + annotationRequired + "]";
	}


	private static class DestinationVariablePlaceholderResolver implements PlaceholderResolver {

		private final Map<String, String> vars;

		public DestinationVariablePlaceholderResolver(Map<String, String> vars) {
			this.vars = vars;
		}

		@Override
		public String resolvePlaceholder(String placeholderName) {
			return (this.vars != null ? this.vars.get(placeholderName) : null);
		}
	}

}
