package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ProfileValueSourceConfiguration}是一个类级注解,
 * 用于指定在检索通过{@link IfProfileValue &#064;IfProfileValue} 注解配置的<em>配置文件值</em>时,
 * 要使用的{@link ProfileValueSource}类型.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ProfileValueSourceConfiguration {

	/**
	 * 检索<em>配置文件值</em>时, 要使用的{@link ProfileValueSource}类型.
	 */
	Class<? extends ProfileValueSource> value() default SystemProfileValueSource.class;

}
