package org.springframework.web.socket.config.annotation;

import java.util.List;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

/**
 * 定义使用来自WebSocket客户端的简单消息协议 (e.g. STOMP) 配置消息处理的方法.
 *
 * <p>通常用于自定义通过{@link EnableWebSocketMessageBroker @EnableWebSocketMessageBroker}提供的配置.
 */
public interface WebSocketMessageBrokerConfigurer {

	/**
	 * 注册STOMP端点, 将每个端点映射到特定URL, 并(可选地)启用和配置SockJS后备选项.
	 */
	void registerStompEndpoints(StompEndpointRegistry registry);

	/**
	 * 配置与从WebSocket客户端接收和发送到WebSocket客户端的消息的处理相关的选项.
	 */
	void configureWebSocketTransport(WebSocketTransportRegistration registry);

	/**
	 * 配置用于来自WebSocket客户端的传入消息的{@link org.springframework.messaging.MessageChannel}.
	 * 默认情况下, Channel由大小为1的线程池支持.
	 * 建议自定义线程池设置以供生产使用.
	 */
	void configureClientInboundChannel(ChannelRegistration registration);

	/**
	 * 配置用于发送到WebSocket客户端的出站消息的{@link org.springframework.messaging.MessageChannel}.
	 * 默认情况下, Channel由大小为1的线程池支持.
	 * 建议自定义线程池设置以供生产使用.
	 */
	void configureClientOutboundChannel(ChannelRegistration registration);

	/**
	 * 添加解析器以支持自定义控制器方法参数类型.
	 * <p>这不会覆盖对解析处理器方法参数的内置支持.
	 * 要自定义内置的参数解析的支持, 直接配置{@code SimpAnnotationMethodMessageHandler}.
	 * 
	 * @param argumentResolvers 要注册的解析器 (最初为空列表)
	 */
	void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers);

	/**
	 * 添加处理器以支持自定义控制器方法返回值类型.
	 * <p>使用此选项不会覆盖处理返回值的内置支持.
	 * 要自定义处理返回值的内置支持, 直接配置{@code SimpAnnotationMethodMessageHandler}.
	 * 
	 * @param returnValueHandlers 要注册的处理器 (最初是一个空列表)
	 */
	void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers);

	/**
	 * 配置在注解方法中提取消息的有效负载时以及在发送消息时使用的消息转换器 (e.g. 通过"broker" SimpMessagingTemplate).
	 * <p>提供的列表, 最初为空, 可用于添加消息转换器, 而boolean返回值用于确定是否还应添加默认消息转换器.
	 * 
	 * @param messageConverters 要配置的转换器 (最初是一个空列表)
	 * 
	 * @return 是否还要添加默认转换器
	 */
	boolean configureMessageConverters(List<MessageConverter> messageConverters);

	/**
	 * 配置消息代理选项.
	 */
	void configureMessageBroker(MessageBrokerRegistry registry);

}
