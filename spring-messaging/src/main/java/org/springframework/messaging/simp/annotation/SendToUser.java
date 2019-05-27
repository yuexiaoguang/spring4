package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示消息处理方法的返回值应作为{@link org.springframework.messaging.Message}发送到指定目标,
 * 目标的前缀为<code>"/user/{username}"</code>, 其中用户名是从正在处理的输入消息的header中提取的.
 *
 * <p>注解也可以放在类级别, 在这种情况下, 应用注解的类中的所有方法都将继承它.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendToUser {

	/**
	 * {@link #destinations}的别名.
	 */
	@AliasFor("destinations")
	String[] value() default {};

	/**
	 * 一个或多个目标.
	 * <p>如果未指定, 则根据正在处理的输入消息的目标选择默认目标.
	 */
	@AliasFor("value")
	String[] destinations() default {};

	/**
	 * 是否应将消息发送到与用户关联的所有会话, 还是仅发送到正在处理的输入消息的会话.
	 * <p>默认{@code true}, 消息将广播到所有会话.
     */
    boolean broadcast() default true;

}
