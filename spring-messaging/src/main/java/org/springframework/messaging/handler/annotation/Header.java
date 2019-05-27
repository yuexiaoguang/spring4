package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 注解, 指示方法参数应绑定到消息header.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Header {

	/**
	 * {@link #name}的别名.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求header的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 是否需要header.
	 * <p>默认值为{@code true}, 如果header丢失则会导致异常.
	 * 如果在缺少header的情况下使用{@code null}值, 将其切换为{@code false}.
	 */
	boolean required() default true;

	/**
	 * 用作后备的默认值.
	 * <p>提供默认值会隐式将{@link #required}设置为{@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
