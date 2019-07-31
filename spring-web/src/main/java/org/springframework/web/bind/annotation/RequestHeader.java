package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示应将方法参数绑定到Web请求header的注解.
 *
 * <p>支持Servlet和Portlet环境中带注解的处理器方法.
 *
 * <p>如果方法参数是{@link java.util.Map Map&lt;String, String&gt;},
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;},
 * 或{@link org.springframework.http.HttpHeaders HttpHeaders}, 然后使用所有header名称和值填充Map.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeader {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求header的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * header是否必需.
	 * <p>默认{@code true}, 如果请求中缺少header, 则会抛出异常.
	 * 如果在请求中不存在header时更喜欢{@code null}值, 将其切换为{@code false}.
	 * <p>或者, 提供{@link #defaultValue}, 隐式将此标志设置为{@code false}.
	 */
	boolean required() default true;

	/**
	 * 用作后备的默认值.
	 * <p>提供默认值会隐式将{@link #required}设置为{@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
