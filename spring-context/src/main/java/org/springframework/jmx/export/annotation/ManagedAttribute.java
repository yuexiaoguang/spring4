package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级注解, 指示将给定的bean属性公开为JMX属性, 对应于{@code ManagedAttribute}属性.
 * 仅在JavaBean getter或setter上使用时有效.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedAttribute {

	String defaultValue() default "";

	String description() default "";

	int currencyTimeLimit() default -1;

	String persistPolicy() default "";

	int persistPeriod() default -1;

}
