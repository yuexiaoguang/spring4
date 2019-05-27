package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于根据订阅目标将订阅消息映射到特定处理器方法.
 * 仅通过WebSocket支持STOMP (e.g. STOMP SUBSCRIBE帧).
 *
 * <p>这是一个方法级注解, 可以与类型级{@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}组合使用.
 *
 * <p>支持与{@code @MessageMapping}相同的方法参数; 但是, 订阅消息通常没有正文.
 *
 * <p>返回值也遵循与{@code @MessageMapping}相同的规则,
 * 除非该方法未使用{@link org.springframework.messaging.handler.annotation.SendTo SendTo}或 {@link SendToUser},
 * 消息将直接发送回连接的用户, 并且不会通过消息代理.
 * 这对于实现请求-回复模式很有用.
 *
 * <p><b>NOTE:</b> 使用控制器接口时 (e.g. 用于AOP代理), 确保放入<i>所有</i>映射注解
 *  - 例如{@code @MessageMapping}和{@code @SubscribeMapping} - 在控制器<i>接口</i>而不是在实现类上.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeMapping {

	/**
	 * 由此注解表示的基于目标的映射.
	 * <p>这是STOMP消息的目标 (e.g. {@code "/positions"}).
	 * 还支持Ant风格的路径模式 (e.g. {@code "/price.stock.*"}) 和路径模板变量 (e.g. <code>"/price.stock.{ticker}"</code>).
	 */
	String[] value() default {};

}
