package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到HTTP cookie的注解.
 *
 * <p>支持Servlet和Portlet环境中带注解的处理器方法.
 *
 * <p>方法参数可以声明为类型{@link javax.servlet.http.Cookie} 或cookie值类型 (String, int, etc.).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的cookie的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * cookie是否必需.
	 * <p>默认为{@code true}, 如果请求中缺少cookie, 则会抛出异常.
	 * 如果在请求中不存在cookie时更喜欢{@code null}值, 将其切换为{@code false}.
	 * <p>或者, 提供{@link #defaultValue}, 隐式将此标志设置为{@code false}.
	 */
	boolean required() default true;

	/**
	 * 用作后备的默认值.
	 * <p>提供默认值会隐式将{@link #required}设置为{@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
