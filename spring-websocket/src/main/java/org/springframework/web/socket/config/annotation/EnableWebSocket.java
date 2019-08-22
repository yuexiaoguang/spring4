package org.springframework.web.socket.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到{@code @Configuration}类以配置处理WebSocket请求:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocket
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 *
 * <p>通过实现{@link WebSocketConfigurer}接口自定义导入的配置:
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
