package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 用于将方法参数绑定到会话属性的注解.
 *
 * <p>主要动机是通过可选/必需的检查和对目标方法参数类型的强制转换, 提供对现有的永久会话属性(如用户认证对象)的方便访问.
 *
 * <p>对于需要添加或删除会话属性的用例, 将{@code org.springframework.web.context.request.WebRequest}
 * 或{@code javax.servlet.http.HttpSession}注入控制器方法.
 *
 * <p>要在会话中临时存储model属性作为控制器工作流的一部分, 使用{@link SessionAttributes}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SessionAttribute {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的会话属性的名称.
	 * <p>默认名称是从方法参数名称推断出来的.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 会话属性是否必需.
	 * <p>默认{@code true}, 如果会话中缺少该属性或没有会话, 则会抛出异常.
	 * 切换为{@code false}, 如果该属性不存在, 则为{@code null}或Java 8 {@code java.util.Optional}.
	 */
	boolean required() default true;

}
