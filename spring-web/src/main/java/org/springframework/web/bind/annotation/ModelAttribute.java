package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.ui.Model;

/**
 * 将方法参数或方法返回值绑定到命名model属性, 公开给Web视图.
 * 支持带有{@link RequestMapping @RequestMapping}方法的控制器类.
 *
 * <p>可以通过注解{@link RequestMapping @RequestMapping}方法的相应参数, 使用特定的属性名称将命令对象公开给Web视图.
 *
 * <p>也可以通过在带有{@link RequestMapping @RequestMapping}方法的控制器类中注解访问器方法,
 * 将引用数据公开给Web视图.
 * 允许这样的访问器方法具有{@link RequestMapping @RequestMapping}方法支持的参数, 返回要公开的model属性值.
 *
 * <p>但请注意, 当请求处理导致{@code Exception}时, Web视图无法使用引用数据和所有其他model内容,
 * 因为可能会在任何时候引发异常, 从而使model的内容不可靠.
 * 因此{@link ExceptionHandler @ExceptionHandler}方法无法访问{@link Model}参数.
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelAttribute {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的model属性的名称.
	 * <p>默认模型属性名称是根据声明的属性类型 (i.e. 方法参数类型或方法返回类型)推断出来的, 基于非限定的类名:
	 * e.g. "orderAddress"用于类"mypackage.OrderAddress", 或"orderAddressList"用于"List&lt;mypackage.OrderAddress&gt;".
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 允许直接在{@code @ModelAttribute}方法参数或从{@code @ModelAttribute}方法返回的属性上禁用数据绑定,
	 * 这两种方法都会阻止该属性的数据绑定.
	 * <p>默认{@code true}, 应用数据绑定.
	 * 将其设置为{@code false}以禁用数据绑定.
	 */
	boolean binding() default true;

}
