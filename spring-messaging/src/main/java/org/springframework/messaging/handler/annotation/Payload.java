package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.converter.MessageConverter;

/**
 * 将方法参数绑定到消息的有效负载的注解.
 * 也可用于将有效负载与方法调用相关联.
 * 有效负载可以通过{@link MessageConverter}传递, 以将其从具有特定MIME类型的序列化形式转换为与目标方法参数匹配的Object.
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload {

	/**
	 * {@link #expression}的别名.
	 */
	@AliasFor("expression")
	String value() default "";

	/**
	 * 要作为根上下文针对有效负载对象计算的SpEL表达式.
	 * <p>根据正在处理的消息是否包含非基本类型对象作为其有效负载, 或是否为序列化形式并且需要消息转换, 可以支持或不支持此属性.
	 * <p>通过WebSocket消息处理STOMP时, 不支持此属性.
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * 是否需要有效载荷内容.
	 * <p>默认值为{@code true}, 如果没有有效负载, 则会导致异常.
	 * 切换到{@code false}以在没有有效负载时传递{@code null}.
	 */
	boolean required() default true;

}
