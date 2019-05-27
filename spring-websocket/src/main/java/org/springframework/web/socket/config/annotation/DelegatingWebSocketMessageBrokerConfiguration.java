package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.CollectionUtils;

/**
 * A {@link WebSocketMessageBrokerConfigurationSupport} extension that detects
 * beans of type {@link WebSocketMessageBrokerConfigurer} and delegates to all
 * of them allowing callback style customization of the configuration provided
 * in {@link WebSocketMessageBrokerConfigurationSupport}.
 *
 * <p>This class is typically imported via {@link EnableWebSocketMessageBroker}.
 */
@Configuration
public class DelegatingWebSocketMessageBrokerConfiguration extends WebSocketMessageBrokerConfigurationSupport {

	private final List<WebSocketMessageBrokerConfigurer> configurers = new ArrayList<WebSocketMessageBrokerConfigurer>();


	@Autowired(required = false)
	public void setConfigurers(List<WebSocketMessageBrokerConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}


	@Override
	protected void registerStompEndpoints(StompEndpointRegistry registry) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.registerStompEndpoints(registry);
		}
	}

	@Override
	protected void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.configureWebSocketTransport(registration);
		}
	}

	@Override
	protected void configureClientInboundChannel(ChannelRegistration registration) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.configureClientInboundChannel(registration);
		}
	}

	@Override
	protected void configureClientOutboundChannel(ChannelRegistration registration) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.configureClientOutboundChannel(registration);
		}
	}

	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.addArgumentResolvers(argumentResolvers);
		}
	}

	@Override
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.addReturnValueHandlers(returnValueHandlers);
		}
	}

	@Override
	protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		boolean registerDefaults = true;
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			if (!configurer.configureMessageConverters(messageConverters)) {
				registerDefaults = false;
			}
		}
		return registerDefaults;
	}

	@Override
	protected void configureMessageBroker(MessageBrokerRegistry registry) {
		for (WebSocketMessageBrokerConfigurer configurer : this.configurers) {
			configurer.configureMessageBroker(registry);
		}
	}

}
