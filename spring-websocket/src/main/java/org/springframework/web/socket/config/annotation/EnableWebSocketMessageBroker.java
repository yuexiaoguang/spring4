package org.springframework.web.socket.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到{@code @Configuration}类, 以使用更高级别的消息传递子协议在WebSocket上启用支持代理的消息传递.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 *
 * <p>通过实现{@link WebSocketMessageBrokerConfigurer}接口自定义导入的配置,
 * 或者更可能扩展方便的基类{@link AbstractWebSocketMessageBrokerConfigurer}:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyConfiguration extends AbstractWebSocketMessageBrokerConfigurer {
 *
 *     &#064;Override
 *     public void registerStompEndpoints(StompEndpointRegistry registry) {
 *         registry.addEndpoint("/portfolio").withSockJS();
 *     }
 *
 *     &#064;Bean
 *     public void configureMessageBroker(MessageBrokerRegistry registry) {
 *         registry.enableStompBrokerRelay("/queue/", "/topic/");
 *         registry.setApplicationDestinationPrefixes("/app/");
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebSocketMessageBrokerConfiguration.class)
public @interface EnableWebSocketMessageBroker {

}
