package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.jmx.support.MetricType;

/**
 * 方法级注解, 指示将给定的bean属性公开为JMX属性, 并添加描述符属性以指示它是度量标准.
 * 仅在JavaBean getter上使用时有效.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedMetric {

	String category() default "";

	int currencyTimeLimit() default -1;

	String description() default "";

	String displayName() default "";

	MetricType metricType() default MetricType.GAUGE;

	int persistPeriod() default -1;

	String persistPolicy() default "";

	String unit() default "";

}
