package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

/**
 * 用于将Portlet渲染请求映射到处理器方法的注解.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RenderMapping {

	/**
	 * Alias for {@link #windowState}.
	 */
	@AliasFor("windowState")
	String value() default "";

	/**
	 * 带注解的render方法适用的窗口状态.
	 * <p>如果未指定, 将为其常规映射中的任何窗口状态调用render方法.
	 * <p>支持标准Portlet规范值: {@code "NORMAL"}, {@code "MAXIMIZED"}, {@code "MINIMIZED"}.
	 * <p>也可以使用自定义窗口状态.
	 */
	@AliasFor("value")
	String windowState() default "";

	/**
	 * 映射请求的参数, 缩小主映射.
	 * <p>任何环境的格式相同: {@code "myParam=myValue"}样式表达式的序列, 只有在发现每个此类参数具有给定值时才会映射请求.
	 * {@code "myParam"}样式表达式也受支持, 这些参数必须存在于请求中 (允许具有任何值).
	 * 最后, {@code "!myParam"}样式表达式表明指定的参数<i>不应该</i>出现在请求中.
	 */
	String[] params() default {};

}
