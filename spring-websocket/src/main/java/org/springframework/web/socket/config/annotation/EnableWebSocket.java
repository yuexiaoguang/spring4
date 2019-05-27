package org.springframework.web.socket.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Add this annotation to an {@code @Configuration} class to configure
 * processing WebSocket requests:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocket
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 *
 * <p>Customize the imported configuration by implementing the
 * {@link WebSocketConfigurer} interface:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocket
 * public class MyConfiguration implements WebSocketConfigurer {
 *
 * 	   &#064;Override
 * 	   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
 *         registry.addHandler(echoWebSocketHandler(), "/echo").withSockJS();
 * 	   }
 *
 *	   &#064;Bean
 *	   public WebSocketHandler echoWebSocketHandler() {
 *         return new EchoWebSocketHandler();
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebSocketConfiguration.class)
public @interface EnableWebSocket {
}
