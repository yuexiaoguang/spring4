package org.springframework.jmx.export.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 类级别注解, 指示使用JMX服务器注册类的实例, 对应于{@code ManagedResource}属性.
 *
 * <p><b>Note:</b> 此注解标记为已继承, 允许通用管理感知基类.
 * 在这种情况下, 建议不要指定对象名称值, 因为这会导致在多个子类注册的情况下命名冲突.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedResource {

	/**
	 * {@link #objectName}属性的别名, 用于简单的默认用法.
	 */
	@AliasFor("objectName")
	String value() default "";

	@AliasFor("value")
	String objectName() default "";

	String description() default "";

	int currencyTimeLimit() default -1;

	boolean log() default false;

	String logFile() default "";

	String persistPolicy() default "";

	int persistPeriod() default -1;

	String persistName() default "";

	String persistLocation() default "";

}
