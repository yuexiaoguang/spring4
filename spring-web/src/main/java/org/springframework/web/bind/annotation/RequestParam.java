package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示应将方法参数绑定到Web请求参数的注解.
 *
 * <p>支持Servlet和Portlet环境中带注解的处理器方法.
 *
 * <p>如果方法参数类型为{@link Map}且指定了请求参数名称, 则假定适当的转换策略可用, 请求参数值将转换为{@link Map}.
 *
 * <p>如果方法参数是{@link java.util.Map Map&lt;String, String&gt;}
 * 或{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 并且未指定参数名称, 然后使用所有请求参数名称和值填充map参数.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求参数的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 参数是否必需.
	 * <p>默认{@code true}, 如果请求中缺少参数, 则会抛出异常.
	 * 如果请求中不存在该参数则为{@code null}, 将其切换为{@code false}.
	 * <p>或者, 提供{@link #defaultValue}, 隐式将此标志设置为{@code false}.
	 */
	boolean required() default true;

	/**
	 * 未提供请求参数或为空值时, 用作回退的默认值.
	 * <p>提供默认值会隐式将{@link #required}设置为{@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
