package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示方法参数应绑定到路径段中的name-value对.
 * 支持Servlet环境中的带{@link RequestMapping}注解的处理器方法.
 *
 * <p>如果方法参数类型是{@link java.util.Map}, 并且指定了矩阵变量名,
 * 那么假设有适当的转换策略, 则矩阵变量值将转换为{@link java.util.Map}.
 *
 * <p>如果方法参数是{@link java.util.Map Map&lt;String, String&gt;}
 * 或{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 并且未指定变量名称, 然后使用所有矩阵变量名称和值填充map.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MatrixVariable {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 矩阵变量的名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 矩阵变量所在的URI路径变量的名称, 如果需要消除歧义 (e.g. 在多个路径段中存在同名的矩阵变量).
	 */
	String pathVar() default ValueConstants.DEFAULT_NONE;

	/**
	 * 矩阵变量是否必需.
	 * <p>默认{@code true}, 如果在请求中缺少变量时, 抛出异常.
	 * 切换为{@code false}, 如果缺少该变量, 则为{@code null}.
	 * <p>或者, 提供{@link #defaultValue}, 隐式将此标志设置为{@code false}.
	 */
	boolean required() default true;

	/**
	 * 用作后备的默认值.
	 * <p>提供默认值会隐式将{@link #required}设置为{@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
