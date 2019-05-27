package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.Message;

/**
 * 注解, 指示方法返回值应转换为{@link Message}并发送到指定目标.
 *
 * <p>在典型的请求/回复场景中, 传入的{@link Message}可以传达用于回复的目标.
 * 在这种情况下, 该目标应该优先.
 *
 * <p>如果提供者支持注解, 则注解也可以放在类级别, 如果没有指定目标, 则表示所有相关方法都应使用此目标。.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendTo {

	/**
	 * 根据方法的返回值创建的消息的目标.
	 */
	String[] value() default {};

}
