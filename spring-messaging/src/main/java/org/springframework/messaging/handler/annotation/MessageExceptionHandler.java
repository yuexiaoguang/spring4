package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于处理特定处理器类中的消息处理方法抛出的异常的注解.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageExceptionHandler {

	/**
	 * 带注解的方法处理的异常.
	 * 如果为空, 则默认为方法参数列表中列出的任何异常.
	 */
	Class<? extends Throwable>[] value() default {};

}
