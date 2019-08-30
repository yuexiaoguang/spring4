package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

/**
 * 用于将Portlet操作请求映射到处理器方法的注解.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ActionMapping {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 操作的名称, 根据Portlet 2.0 {@code javax.portlet.action}参数.
	 * <p>如果未指定, 则带注解的方法将用作默认处理器: i.e. 用于未找到特定操作映射的操作请求.
	 * <p>请注意, 所有这些带注解的操作方法仅适用于包含处理器类的{@code @RequestMapping}约束.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 映射请求的参数, 缩小主映射.
	 * <p>任何环境的格式相同: {@code "myParam=myValue"}样式表达式的序列, 只有在发现每个此类参数具有给定值时才会映射请求.
	 * {@code "myParam"}样式表达式也受支持, 这些参数必须存在于请求中 (允许具有任何值).
	 * 最后, {@code "!myParam"}样式表达式表明指定的参数<i>不应该</i>出现在请求中.
	 */
	String[] params() default {};

}
