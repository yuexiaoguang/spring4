package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 用于将方法参数绑定到请求属性的注解.
 *
 * <p>主要动机是使用可选/必需的检查和转换为目标方法参数类型, 从控制器方法提供对请求属性的方便访问.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestAttribute {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求属性的名称.
	 * <p>默认名称是从方法参数名称推断出来的.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 请求属性是否必需.
	 * <p>默认{@code true}, 如果缺少该属性, 则会抛出异常.
	 * 切换为{@code false}, 如果更喜欢{@code null}或Java 8 {@code java.util.Optional}.
	 */
	boolean required() default true;

}
