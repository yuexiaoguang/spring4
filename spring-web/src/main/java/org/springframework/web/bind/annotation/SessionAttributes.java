package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示特定处理器使用的会话属性.
 *
 * <p>这通常会列出model属性的名称, 这些属性应该透明地存储在会话或某些会话存储中, 作为表单支持bean.
 * <b>在类型级别声明</b>, 应用于带注解的处理器类操作的model属性.
 *
 * <p><b>NOTE:</b> 使用此注解指示的会话属性对应于特定处理器的模型属性, 透明地存储在会话中.
 * 一旦处理器指示其会话完成, 将删除这些属性.
 * 因此，将此工具用于此类会话属性, 这些属性应该在特定处理器的交互过程中, 临时存储在会话中.
 *
 * <p>对于永久会话属性, e.g. 用户身份验证对象, 请改用传统的{@code session.setAttribute}方法.
 * 或者, 考虑使用通用{@link org.springframework.web.context.request.WebRequest}接口的属性管理功能.
 *
 * <p><b>NOTE:</b> 使用controller接口(e.g. 用于AOP代理)时, 确保始终放入<i>所有</i>映射注解 &mdash;
 * 例如{@code @RequestMapping}和{@code @SessionAttributes} &mdash; 在controller <i>接口</i>上, 而不是在实现类上.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SessionAttributes {

	/**
	 * Alias for {@link #names}.
	 */
	@AliasFor("names")
	String[] value() default {};

	/**
	 * 应存储在会话或某些会话存储中的会话属性的名称.
	 * <p><strong>Note</strong>: 这表示<em>模型属性名称</em>.
	 * <em>会话属性名称</em>可能与模型属性名称匹配, 也可能不匹配.
	 * 因此, 应用程序不应依赖会话属性名称, 而应仅对模型进行操作.
	 */
	@AliasFor("value")
	String[] names() default {};

	/**
	 * 应存储在会话或某些会话存储中的会话属性类型.
	 * <p>无论属性名称如何, 这些类型的所有模型属性都将存储在会话中.
	 */
	Class<?>[] types() default {};

}
