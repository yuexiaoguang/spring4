package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到URI模板变量的注解.
 * 支持Servlet环境中带{@link RequestMapping}注解的处理器方法.
 *
 * <p>如果方法参数是{@link java.util.Map Map&lt;String, String&gt;}, 那么将使用所有路径变量名称和值填充Map.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的路径变量的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 路径变量是否必需.
	 * <p>默认{@code true}, 如果传入请求中缺少路径变量, 则会导致抛出异常.
	 * 如果您喜欢{@code null}或Java 8 {@code java.util.Optional}, 将此切换为{@code false}.
	 * e.g. 在{@code ModelAttribute}方法上, 该方法用于不同的请求.
	 */
	boolean required() default true;

}
