package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * 提供使用简单消息协议(如STOMP)处理消息的基本配置.
 *
 * <p>{@link #clientInboundChannel()} 和 {@link #clientOutboundChannel()}
 * 发送消息到, 以及将消息从远程客户端传递到多个消息处理器, 例如
 * <ul>
 * <li>{@link #simpAnnotationMethodMessageHandler()}</li>
 * <li>{@link #simpleBrokerMessageHandler()}</li>
 * <li>{@link #stompBrokerRelayMessageHandler()}</li>
 * <li>{@link #userDestinationMessageHandler()}</li>
 * </ul>
 * 而{@link #brokerChannel()}将应用程序内的消息传递给相应的消息处理器.
 * {@link #brokerMessagingTemplate()}可以注入到任何应用程序组件以发送消息.
 *
 * <p>子类负责为客户端入站/出站通道提供消息的配置部分 (e.g. 通过WebSocket的STOMP).
 */
public abstract class AbstractMessageBrokerConfiguration implements ApplicationContextAware {

	private static final String MVC_VALIDATOR_NAME = "mvcValidator";

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", AbstractMessageBrokerConfiguration.class.getClassLoader());


	private ApplicationContext applicationContext;

	private ChannelRegistration clientInboundChannelRegistration;

	private ChannelRegistration clientOutboundChannelRegistration;

	private MessageBrokerRegistry brokerRegistry;


	/**
	 * Protected constructor.
	 */
	protected AbstractMessageBrokerConfiguration() {
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Bean
	public AbstractSubscribableChannel clientInboundChannel() {
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(clientInboundChannelExecutor());
		ChannelRegistration reg = getClientInboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public ThreadPoolTaskExecutor clientInboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientInboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientInboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientInboundChannelRegistration() {
		if (this.clientInboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientInboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientInboundChannelRegistration = registration;
		}
		return this.clientInboundChannelRegistration;
	}

	/**
	 * 用于子类的挂钩, 用于为来自WebSocket客户端的入站消息自定义消息通道.
	 */
	protected void configureClientInboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel clientOutboundChannel() {
		ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(clientOutboundChannelExecutor());
		ChannelRegistration reg = getClientOutboundChannelRegistration();
		if (reg.hasInterceptors()) {
			channel.setInterceptors(reg.getInterceptors());
		}
		return channel;
	}

	@Bean
	public ThreadPoolTaskExecutor clientOutboundChannelExecutor() {
		TaskExecutorRegistration reg = getClientOutboundChannelRegistration().taskExecutor();
		ThreadPoolTaskExecutor executor = reg.getTaskExecutor();
		executor.setThreadNamePrefix("clientOutboundChannel-");
		return executor;
	}

	protected final ChannelRegistration getClientOutboundChannelRegistration() {
		if (this.clientOutboundChannelRegistration == null) {
			ChannelRegistration registration = new ChannelRegistration();
			configureClientOutboundChannel(registration);
			registration.interceptors(new ImmutableMessageChannelInterceptor());
			this.clientOutboundChannelRegistration = registration;
		}
		return this.clientOutboundChannelRegistration;
	}

	/**
	 * 子类的挂钩, 用于为从应用程序或消息代理到WebSocket客户端的消息自定义消息通道.
	 */
	protected void configureClientOutboundChannel(ChannelRegistration registration) {
	}

	@Bean
	public AbstractSubscribableChannel brokerChannel() {
		ChannelRegistration reg = getBrokerRegistry().getBrokerChannelRegistration();
		ExecutorSubscribableChannel channel = (reg.hasTaskExecutor() ?
				new ExecutorSubscribableChannel(brokerChannelExecutor()) : new ExecutorSubscribableChannel());
		reg.interceptors(new ImmutableMessageChannelInterceptor());
		channel.setInterceptors(reg.getInterceptors());
		return channel;
	}

	@Bean
	public ThreadPoolTaskExecutor brokerChannelExecutor() {
		ChannelRegistration reg = getBrokerRegistry().getBrokerChannelRegistration();
		ThreadPoolTaskExecutor executor;
		if (reg.hasTaskExecutor()) {
			executor = reg.taskExecutor().getTaskExecutor();
		}
		else {
			// Should never be used
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(0);
			executor.setMaxPoolSize(1);
			executor.setQueueCapacity(0);
		}
		executor.setThreadNamePrefix("brokerChannel-");
		return executor;
	}

	/**
	 * {@link MessageBrokerRegistry}的访问器, 通过{@link #configureMessageBroker(MessageBrokerRegistry)}确保其一次性创建和初始化.
	 */
	protected final MessageBrokerRegistry getBrokerRegistry() {
		if (this.brokerRegistry == null) {
			MessageBrokerRegistry registry = new MessageBrokerRegistry(clientInboundChannel(), clientOutboundChannel());
			configureMessageBroker(registry);
			this.brokerRegistry = registry;
		}
		return this.brokerRegistry;
	}

	/**
	 * 子类的挂钩, 用于通过提供的{@link MessageBrokerRegistry}实例自定义消息代理配置.
	 */
	protected void configureMessageBroker(MessageBrokerRegistry registry) {
	}

	/**
	 * 提供对配置的PatchMatcher的访问, 以便从其他配置类进行访问.
	 */
	public final PathMatcher getPathMatcher() {
		return getBrokerRegistry().getPathMatcher();
	}

	@Bean
	public SimpAnnotationMethodMessageHandler simpAnnotationMethodMessageHandler() {
		SimpAnnotationMethodMessageHandler handler = createAnnotationMethodMessageHandler();
		handler.setDestinationPrefixes(getBrokerRegistry().getApplicationDestinationPrefixes());
		handler.setMessageConverter(brokerMessageConverter());
		handler.setValidator(simpValidator());

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
		addArgumentResolvers(argumentResolvers);
		handler.setCustomArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
		addReturnValueHandlers(returnValueHandlers);
		handler.setCustomReturnValueHandlers(returnValueHandlers);

		PathMatcher pathMatcher = getBrokerRegistry().getPathMatcher();
		if (pathMatcher != null) {
			handler.setPathMatcher(pathMatcher);
		}
		return handler;
	}

	/**
	 * 用于插入
	 * {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler SimpAnnotationMethodMessageHandler}
	 * 的自定义子类的方法.
	 */
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler() {
		return new SimpAnnotationMethodMessageHandler(clientInboundChannel(),
				clientOutboundChannel(), brokerMessagingTemplate());
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	@Bean
	public AbstractBrokerMessageHandler simpleBrokerMessageHandler() {
		SimpleBrokerMessageHandler handler = getBrokerRegistry().getSimpleBroker(brokerChannel());
		if (handler == null) {
			return new NoOpBrokerMessageHandler();
		}
		updateUserDestinationResolver(handler);
		return handler;
	}

	private void updateUserDestinationResolver(AbstractBrokerMessageHandler handler) {
		Collection<String> prefixes = handler.getDestinationPrefixes();
		if (!prefixes.isEmpty() && !prefixes.iterator().next().startsWith("/")) {
			((DefaultUserDestinationResolver) userDestinationResolver()).setRemoveLeadingSlash(true);
		}
	}

	@Bean
	public AbstractBrokerMessageHandler stompBrokerRelayMessageHandler() {
		StompBrokerRelayMessageHandler handler = getBrokerRegistry().getStompBrokerRelay(brokerChannel());
		if (handler == null) {
			return new NoOpBrokerMessageHandler();
		}
		Map<String, MessageHandler> subscriptions = new HashMap<String, MessageHandler>(1);
		String destination = getBrokerRegistry().getUserDestinationBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userDestinationMessageHandler());
		}
		destination = getBrokerRegistry().getUserRegistryBroadcast();
		if (destination != null) {
			subscriptions.put(destination, userRegistryMessageHandler());
		}
		handler.setSystemSubscriptions(subscriptions);
		updateUserDestinationResolver(handler);
		return handler;
	}

	@Bean
	public UserDestinationMessageHandler userDestinationMessageHandler() {
		UserDestinationMessageHandler handler = new UserDestinationMessageHandler(clientInboundChannel(),
				brokerChannel(), userDestinationResolver());
		String destination = getBrokerRegistry().getUserDestinationBroadcast();
		handler.setBroadcastDestination(destination);
		return handler;
	}

	@Bean
	public MessageHandler userRegistryMessageHandler() {
		if (getBrokerRegistry().getUserRegistryBroadcast() == null) {
			return new NoOpMessageHandler();
		}
		SimpUserRegistry userRegistry = userRegistry();
		Assert.isInstanceOf(MultiServerUserRegistry.class, userRegistry, "MultiServerUserRegistry required");
		return new UserRegistryMessageHandler((MultiServerUserRegistry) userRegistry,
				brokerMessagingTemplate(), getBrokerRegistry().getUserRegistryBroadcast(),
				messageBrokerTaskScheduler());
	}

	// Expose alias for 4.1 compatibility
	@Bean(name = {"messageBrokerTaskScheduler", "messageBrokerSockJsTaskScheduler"})
	public ThreadPoolTaskScheduler messageBrokerTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MessageBroker-");
		scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Bean
	public SimpMessagingTemplate brokerMessagingTemplate() {
		SimpMessagingTemplate template = new SimpMessagingTemplate(brokerChannel());
		String prefix = getBrokerRegistry().getUserDestinationPrefix();
		if (prefix != null) {
			template.setUserDestinationPrefix(prefix);
		}
		template.setMessageConverter(brokerMessageConverter());
		return template;
	}

	@Bean
	public CompositeMessageConverter brokerMessageConverter() {
		List<MessageConverter> converters = new ArrayList<MessageConverter>();
		boolean registerDefaults = configureMessageConverters(converters);
		if (registerDefaults) {
			converters.add(new StringMessageConverter());
			converters.add(new ByteArrayMessageConverter());
			if (jackson2Present) {
				converters.add(createJacksonConverter());
			}
		}
		return new CompositeMessageConverter(converters);
	}

	protected MappingJackson2MessageConverter createJacksonConverter() {
		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
		resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setContentTypeResolver(resolver);
		return converter;
	}

	/**
	 * 重写此方法以添加自定义消息转换器.
	 * 
	 * @param messageConverters 添加转换器的列表, 最初为空
	 * 
	 * @return {@code true} 如果应将默认消息转换器添加到列表中, {@code false} 如果不应添加更多转换器.
	 */
	protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return true;
	}

	@Bean
	public UserDestinationResolver userDestinationResolver() {
		DefaultUserDestinationResolver resolver = new DefaultUserDestinationResolver(userRegistry());
		String prefix = getBrokerRegistry().getUserDestinationPrefix();
		if (prefix != null) {
			resolver.setUserDestinationPrefix(prefix);
		}
		return resolver;
	}

	@Bean
	public SimpUserRegistry userRegistry() {
		return (getBrokerRegistry().getUserRegistryBroadcast() != null ?
				new MultiServerUserRegistry(createLocalUserRegistry()) : createLocalUserRegistry());
	}

	/**
	 * 创建用户注册表, 以提供对本地用户的访问.
	 */
	protected abstract SimpUserRegistry createLocalUserRegistry();

	/**
	 * 从4.2开始, {@code UserSessionRegistry}不推荐使用{@link SimpUserRegistry}公开所有连接用户的信息.
	 * {@link MultiServerUserRegistry}实现与{@link UserRegistryMessageHandler}结合使用, 可用于跨多个服务器共享用户注册表.
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	protected org.springframework.messaging.simp.user.UserSessionRegistry userSessionRegistry() {
		return null;
	}

	/**
	 * 返回{@link org.springframework.validation.Validator}实例以验证{@code @Payload}方法参数.
	 * <p>按顺序, 此方法尝试获取Validator实例:
	 * <ul>
	 * <li>首先委托给 getValidator()</li>
	 * <li>如果没有返回, 则获取一个现有实例, 其名称为"mvcValidator", 由MVC配置创建</li>
	 * <li>如果没有返回, 则在创建{@code OptionalValidatorFactoryBean}之前检查类路径是否存在JSR-303实现</li>
	 * <li>返回一个无操作的Validator实例</li>
	 * </ul>
	 */
	protected Validator simpValidator() {
		Validator validator = getValidator();
		if (validator == null) {
			if (this.applicationContext.containsBean(MVC_VALIDATOR_NAME)) {
				validator = this.applicationContext.getBean(MVC_VALIDATOR_NAME, Validator.class);
			}
			else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
				Class<?> clazz;
				try {
					String className = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
					clazz = ClassUtils.forName(className, AbstractMessageBrokerConfiguration.class.getClassLoader());
				}
				catch (Throwable ex) {
					throw new BeanInitializationException("Could not find default validator class", ex);
				}
				validator = (Validator) BeanUtils.instantiateClass(clazz);
			}
			else {
				validator = new Validator() {
					@Override
					public boolean supports(Class<?> clazz) {
						return false;
					}
					@Override
					public void validate(Object target, Errors errors) {
					}
				};
			}
		}
		return validator;
	}

	/**
	 * 重写此方法以提供自定义{@link Validator}.
	 */
	public Validator getValidator() {
		return null;
	}


	private static class NoOpMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) {
		}
	}


	private class NoOpBrokerMessageHandler extends AbstractBrokerMessageHandler {

		public NoOpBrokerMessageHandler() {
			super(clientInboundChannel(), clientOutboundChannel(), brokerChannel());
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}

		@Override
		public void handleMessage(Message<?> message) {
		}

		@Override
		protected void handleMessageInternal(Message<?> message) {
		}
	}

}
