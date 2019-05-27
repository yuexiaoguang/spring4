package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.CompletableFutureReturnValueHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.ListenableFutureReturnValueHandler;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageTypeMessageCondition;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;

/**
 * 委托给带{@link MessageMapping @MessageMapping}和{@link SubscribeMapping @SubscribeMapping}注解的方法的消息处理器.
 *
 * <p>支持带有模板变量的Ant样式路径模式.
 */
public class SimpAnnotationMethodMessageHandler extends AbstractMethodMessageHandler<SimpMessageMappingInfo>
		implements EmbeddedValueResolverAware, SmartLifecycle {

	private static final boolean completableFuturePresent = ClassUtils.isPresent(
			"java.util.concurrent.CompletableFuture", SimpAnnotationMethodMessageHandler.class.getClassLoader());


	private final SubscribableChannel clientInboundChannel;

	private final SimpMessageSendingOperations clientMessagingTemplate;

	private final SimpMessageSendingOperations brokerTemplate;

	private MessageConverter messageConverter;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private boolean slashPathSeparator = true;

	private Validator validator;

	private StringValueResolver valueResolver;

	private MessageHeaderInitializer headerInitializer;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	/**
	 * @param clientInboundChannel 用于从客户端 (e.g. WebSocket客户端)接收消息的通道
	 * @param clientOutboundChannel 用于发送到客户端 (e.g. WebSocket客户端)的通道
	 * @param brokerTemplate 用于将应用消息发送到代理的消息模板
	 */
	public SimpAnnotationMethodMessageHandler(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, SimpMessageSendingOperations brokerTemplate) {

		Assert.notNull(clientInboundChannel, "clientInboundChannel must not be null");
		Assert.notNull(clientOutboundChannel, "clientOutboundChannel must not be null");
		Assert.notNull(brokerTemplate, "brokerTemplate must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.clientMessagingTemplate = new SimpMessagingTemplate(clientOutboundChannel);
		this.brokerTemplate = brokerTemplate;

		Collection<MessageConverter> converters = new ArrayList<MessageConverter>();
		converters.add(new StringMessageConverter());
		converters.add(new ByteArrayMessageConverter());
		this.messageConverter = new CompositeMessageConverter(converters);
	}


	/**
	 * {@inheritDoc}
	 * <p>目标前缀应该是斜杠分隔的字符串, 因此在缺失的地方自动附加斜杠以确保正确的基于前缀的匹配 (i.e. 匹配完整的段).
	 * <p>但请注意, 前缀后的目标的剩余部分可能使用不同的分隔符 (e.g. 消息中常见的"."), 具体取决于配置的{@code PathMatcher}.
	 */
	@Override
	public void setDestinationPrefixes(Collection<String> prefixes) {
		super.setDestinationPrefixes(appendSlashes(prefixes));
	}

	private static Collection<String> appendSlashes(Collection<String> prefixes) {
		if (CollectionUtils.isEmpty(prefixes)) {
			return prefixes;
		}
		Collection<String> result = new ArrayList<String>(prefixes.size());
		for (String prefix : prefixes) {
			if (!prefix.endsWith("/")) {
				prefix = prefix + "/";
			}
			result.add(prefix);
		}
		return result;
	}

	/**
	 * 配置{@link MessageConverter}, 用于将消息的有效负载从具有特定MIME类型的序列化形式转换为与目标方法参数匹配的对象.
	 * 在向消息代理发送消息时也使用转换器.
	 */
	public void setMessageConverter(MessageConverter converter) {
		this.messageConverter = converter;
		if (converter != null) {
			((AbstractMessageSendingTemplate<?>) this.clientMessagingTemplate).setMessageConverter(converter);
		}
	}

	/**
	 * 返回配置的{@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * 配置{@link ConversionService}以在解析方法参数时使用, 例如消息header值.
	 * <p>默认使用{@link DefaultFormattingConversionService}.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回配置的{@link ConversionService}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 设置用于匹配已配置目标模式的目标的PathMatcher实现.
	 * <p>默认使用{@link AntPathMatcher}.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		this.slashPathSeparator = this.pathMatcher.combine("a", "a").equals("a/a");
	}

	/**
	 * 返回用于匹配目标的PathMatcher实现.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 返回配置的Validator实例.
	 */
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * 设置用于验证@Payload参数的Validator实例
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.valueResolver = resolver;
	}

	/**
	 * 配置{@link MessageHeaderInitializer}, 以传递到从控制器返回值发送消息的
	 * {@link org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler}.
	 * <p>默认未设置此属性.
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
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}


	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		ConfigurableBeanFactory beanFactory = (getApplicationContext() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getApplicationContext()).getBeanFactory() : null);

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

		// Type-based argument resolution
		resolvers.add(new PrincipalMethodArgumentResolver());
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		resolvers.addAll(getCustomArgumentResolvers());
		resolvers.add(new PayloadArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();

		// Single-purpose return value types
		handlers.add(new ListenableFutureReturnValueHandler());
		if (completableFuturePresent) {
			handlers.add(new CompletableFutureReturnValueHandler());
		}

		// Annotation-based return value types
		SendToMethodReturnValueHandler sendToHandler =
				new SendToMethodReturnValueHandler(this.brokerTemplate, true);
		sendToHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(sendToHandler);

		SubscriptionMethodReturnValueHandler subscriptionHandler =
				new SubscriptionMethodReturnValueHandler(this.clientMessagingTemplate);
		subscriptionHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(subscriptionHandler);

		// custom return value types
		handlers.addAll(getCustomReturnValueHandlers());

		// catch-all
		sendToHandler = new SendToMethodReturnValueHandler(this.brokerTemplate, false);
		sendToHandler.setHeaderInitializer(this.headerInitializer);
		handlers.add(sendToHandler);

		return handlers;
	}


	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
	}

	@Override
	protected SimpMessageMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		MessageMapping messageAnn = AnnotatedElementUtils.findMergedAnnotation(method, MessageMapping.class);
		if (messageAnn != null) {
			MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
			// 如果指定了目标, 则仅实际注册;
			// 否则@MessageMapping只是用作 (元注解)标记.
			if (messageAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
				SimpMessageMappingInfo result = createMessageMappingCondition(messageAnn.value());
				if (typeAnn != null) {
					result = createMessageMappingCondition(typeAnn.value()).combine(result);
				}
				return result;
			}
		}

		SubscribeMapping subscribeAnn = AnnotatedElementUtils.findMergedAnnotation(method, SubscribeMapping.class);
		if (subscribeAnn != null) {
			MessageMapping typeAnn = AnnotatedElementUtils.findMergedAnnotation(handlerType, MessageMapping.class);
			// 如果指定了目标, 则仅实际注册;
			// 否则@SubscribeMapping只是用作 (元注解)标记.
			if (subscribeAnn.value().length > 0 || (typeAnn != null && typeAnn.value().length > 0)) {
				SimpMessageMappingInfo result = createSubscribeMappingCondition(subscribeAnn.value());
				if (typeAnn != null) {
					result = createMessageMappingCondition(typeAnn.value()).combine(result);
				}
				return result;
			}
		}

		return null;
	}

	private SimpMessageMappingInfo createMessageMappingCondition(String[] destinations) {
		String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.MESSAGE,
				new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
	}

	private SimpMessageMappingInfo createSubscribeMappingCondition(String[] destinations) {
		String[] resolvedDestinations = resolveEmbeddedValuesInDestinations(destinations);
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.SUBSCRIBE,
				new DestinationPatternsMessageCondition(resolvedDestinations, this.pathMatcher));
	}

	/**
	 * 在给定的目标数组中解析占位符值.
	 * 
	 * @return 已更新的目标的数组
	 */
	protected String[] resolveEmbeddedValuesInDestinations(String[] destinations) {
		if (this.valueResolver == null) {
			return destinations;
		}
		String[] result = new String[destinations.length];
		for (int i = 0; i < destinations.length; i++) {
			result[i] = this.valueResolver.resolveStringValue(destinations[i]);
		}
		return result;
	}

	@Override
	protected Set<String> getDirectLookupDestinations(SimpMessageMappingInfo mapping) {
		Set<String> result = new LinkedHashSet<String>();
		for (String pattern : mapping.getDestinationConditions().getPatterns()) {
			if (!this.pathMatcher.isPattern(pattern)) {
				result.add(pattern);
			}
		}
		return result;
	}

	@Override
	protected String getDestination(Message<?> message) {
		return SimpMessageHeaderAccessor.getDestination(message.getHeaders());
	}

	@Override
	protected String getLookupDestination(String destination) {
		if (destination == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(getDestinationPrefixes())) {
			return destination;
		}
		for (String prefix : getDestinationPrefixes()) {
			if (destination.startsWith(prefix)) {
				if (this.slashPathSeparator) {
					return destination.substring(prefix.length() - 1);
				}
				else {
					return destination.substring(prefix.length());
				}
			}
		}
		return null;
	}

	@Override
	protected SimpMessageMappingInfo getMatchingMapping(SimpMessageMappingInfo mapping, Message<?> message) {
		return mapping.getMatchingCondition(message);

	}

	@Override
	protected Comparator<SimpMessageMappingInfo> getMappingComparator(final Message<?> message) {
		return new Comparator<SimpMessageMappingInfo>() {
			@Override
			public int compare(SimpMessageMappingInfo info1, SimpMessageMappingInfo info2) {
				return info1.compareTo(info2, message);
			}
		};
	}

	@Override
	protected void handleMatch(SimpMessageMappingInfo mapping, HandlerMethod handlerMethod,
			String lookupDestination, Message<?> message) {

		Set<String> patterns = mapping.getDestinationConditions().getPatterns();
		if (!CollectionUtils.isEmpty(patterns)) {
			String pattern = patterns.iterator().next();
			Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(pattern, lookupDestination);
			if (!CollectionUtils.isEmpty(vars)) {
				MessageHeaderAccessor mha = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
				Assert.state(mha != null && mha.isMutable(), "Mutable MessageHeaderAccessor required");
				mha.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
			}
		}

		try {
			SimpAttributesContextHolder.setAttributesFromMessage(message);
			super.handleMatch(mapping, handlerMethod, lookupDestination, message);
		}
		finally {
			SimpAttributesContextHolder.resetAttributes();
		}
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

}
