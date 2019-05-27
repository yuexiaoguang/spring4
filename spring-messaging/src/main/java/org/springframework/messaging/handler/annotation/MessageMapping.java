package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.Message;

/**
 * 通过匹配消息目标将{@link Message}映射到消息处理方法的注解.
 * 此注解也可以在类型级别上使用, 在这种情况下, 它为所有方法级别注解定义公共目标前缀或模式, 包括方法级别
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping @SubscribeMapping}注解.
 *
 * <p>使用此注解的处理程序方法允许具有灵活的签名.
 * 它们可以以任意顺序具有以下类型的参数:
 * <ul>
 * <li>{@link Message}可以访问正在处理的完整消息.</li>
 * <li>带{@link Payload}注解的方法参数, 用于提取消息的有效负载,
 * 并可选择使用{@link org.springframework.messaging.converter.MessageConverter}进行转换.
 * 不需要存在注解, 因为默认情况下假定方法参数没有注解.
 * 使用带验证注解的Payload方法参数 (如{@link org.springframework.validation.annotation.Validated}) 将受JSR-303验证的约束.</li>
 * <li>带{@link Header}注解的方法参数, 提取特定header值,
 * 并使用{@link org.springframework.core.convert.converter.Converter}进行类型转换.</li>
 * <li>带{@link Headers}注解的方法参数, 也必须可分配给{@link java.util.Map}以获取对所有header的访问权限.</li>
 * <li>用于访问所有header的{@link org.springframework.messaging.MessageHeaders}参数.</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor}或STOMP通过WebSocket也支持子类,
 * 例如{@link org.springframework.messaging.simp.SimpMessageHeaderAccessor}以方便访问所有方法参数.</li>
 * <li>带{@link DestinationVariable}注解的参数, 用于访问从消息目标中提取的模板变量值 (e.g. /hotels/{hotel}).
 * 变量值将转换为声明的方法参数类型.</li>
 * <li>STOMP通过WebSocket消息支持{@link java.security.Principal}方法参数.
 * 它反映了登录到接收消息的WebSocket会话的用户.
 * 基于HTTP的常规身份验证 (e.g. 基于Spring Security) 可用于保护启动WebSocket会话的HTTP握手.</li>
 * </ul>
 *
 * <p>返回值将作为消息包装, 并发送到默认响应目标或使用{@link SendTo @SendTo}方法级注解指定的自定义目标.
 * 这样的响应也可以通过{@link org.springframework.util.concurrent.ListenableFuture}返回类型
 * 或相应的JDK 8 {@link java.util.concurrent.CompletableFuture} / {@link java.util.concurrent.CompletionStage}句柄异步提供.
 *
 * <h3>STOMP over WebSocket</h3>
 * <p>不严格要求{@link SendTo @SendTo}注解 &mdash;
 * 默认情况下, 消息将发送到与传入消息相同的目标, 但具有附加前缀 (默认为{@code "/topic"}).
 * 也可以使用{@link org.springframework.messaging.simp.annotation.SendToUser}注解将消息定向到特定用户, 如果已连接.
 * 使用{@link org.springframework.messaging.converter.MessageConverter}转换返回值.
 *
 * <p><b>NOTE:</b> 使用controller接口时 (e.g. 用于AOP代理), 确保放入<i>所有</i>映射注解
 *  - 例如{@code @MessageMapping}和{@code @SubscribeMapping} - 在controller<i>接口</i>上而不是在实现类上.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageMapping {

	/**
	 * 由此注解表示的基于目标的映射.
	 * <p>对于STOMP over WebSocket消息: 这是STOMP消息的目标 (e.g. {@code "/positions"}).
	 * 还支持Ant风格的路径模式 (e.g. {@code "/price.stock.*"}) 和路径模板变量 (e.g. <code>"/price.stock.{ticker}"</code>).
	 */
	String[] value() default {};

}
