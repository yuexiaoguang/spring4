package org.springframework.web.socket.config.annotation;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpSessionScope;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.config.AbstractMessageBrokerConfiguration;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserSessionRegistryAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

/**
 * 扩展{@link AbstractMessageBrokerConfiguration}, 并添加用于从WebSocket客户端接收和响应STOMP消息的配置.
 *
 * <p>通常与{@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker}一起使用, 但也可以直接扩展.
 */
public abstract class WebSocketMessageBrokerConfigurationSupport extends AbstractMessageBrokerConfiguration {

	private WebSocketTransportRegistration transportRegistration;


	@Override
	protected SimpAnnotationMethodMessageHandler createAnnotationMethodMessageHandler() {
		return new WebSocketAnnotationMethodMessageHandler(
				clientInboundChannel(), clientOutboundChannel(), brokerMessagingTemplate());
	}

	@Override
	@SuppressWarnings("deprecation")
	protected SimpUserRegistry createLocalUserRegistry() {
		org.springframework.messaging.simp.user.UserSessionRegistry sessionRegistry = userSessionRegistry();
		if (sessionRegistry != null) {
			return new UserSessionRegistryAdapter(sessionRegistry);
		}
		return new DefaultSimpUserRegistry();
	}

	@Bean
	@SuppressWarnings("deprecation")
	public HandlerMapping stompWebSocketHandlerMapping() {
		WebSocketHandler handler = decorateWebSocketHandler(subProtocolWebSocketHandler());
		WebMvcStompEndpointRegistry registry = new WebMvcStompEndpointRegistry(
				handler, getTransportRegistration(), userSessionRegistry(), messageBrokerTaskScheduler());
		registry.setApplicationContext(getApplicationContext());
		registerStompEndpoints(registry);
		return registry.getHandlerMapping();
	}

	@Bean
	public WebSocketHandler subProtocolWebSocketHandler() {
		return new SubProtocolWebSocketHandler(clientInboundChannel(), clientOutboundChannel());
	}

	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		for (WebSocketHandlerDecoratorFactory factory : getTransportRegistration().getDecoratorFactories()) {
			handler = factory.decorate(handler);
		}
		return handler;
	}

	protected final WebSocketTransportRegistration getTransportRegistration() {
		if (this.transportRegistration == null) {
			this.transportRegistration = new WebSocketTransportRegistration();
			configureWebSocketTransport(this.transportRegistration);
		}
		return this.transportRegistration;
	}

	protected void configureWebSocketTransport(WebSocketTransportRegistration registry) {
	}

	protected abstract void registerStompEndpoints(StompEndpointRegistry registry);

	@Bean
	public static CustomScopeConfigurer webSocketScopeConfigurer() {
		CustomScopeConfigurer configurer = new CustomScopeConfigurer();
		configurer.addScope("websocket", new SimpSessionScope());
		return configurer;
	}

	@Bean
	public WebSocketMessageBrokerStats webSocketMessageBrokerStats() {
		AbstractBrokerMessageHandler relayBean = stompBrokerRelayMessageHandler();
		StompBrokerRelayMessageHandler brokerRelay = (relayBean instanceof StompBrokerRelayMessageHandler ?
				(StompBrokerRelayMessageHandler) relayBean : null);

		// 确保已注册STOMP端点
		stompWebSocketHandlerMapping();

		WebSocketMessageBrokerStats stats = new WebSocketMessageBrokerStats();
		stats.setSubProtocolWebSocketHandler((SubProtocolWebSocketHandler) subProtocolWebSocketHandler());
		stats.setStompBrokerRelay(brokerRelay);
		stats.setInboundChannelExecutor(clientInboundChannelExecutor());
		stats.setOutboundChannelExecutor(clientOutboundChannelExecutor());
		stats.setSockJsTaskScheduler(messageBrokerTaskScheduler());
		return stats;
	}

	@Override
	protected MappingJackson2MessageConverter createJacksonConverter() {
		MappingJackson2MessageConverter messageConverter = super.createJacksonConverter();
		// 使用Jackson构建器以自动注册JSR-310和Joda-Time模块
		messageConverter.setObjectMapper(Jackson2ObjectMapperBuilder.json()
				.applicationContext(getApplicationContext()).build());
		return messageConverter;
	}

}
