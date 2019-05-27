package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级注解, 指示将给定方法公开为JMX操作, 对应于{@code ManagedOperation}属性.
 * 仅在非JavaBean getter或setter的方法上使用时才有效.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedOperation {

	String description() default "";

	int currencyTimeLimit() default -1;

}
