package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.converter.HttpMessageConverter;

/**
 * 指示方法参数应绑定到Web请求的主体的注解.
 * 请求的主体通过{@link HttpMessageConverter}传递, 以根据请求的内容类型解析方法参数.
 * 可选地, 参数可以通过使用{@code @Valid}注解来应用自动验证.
 *
 * <p>支持Servlet环境中带注解的处理器方法.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {

	/**
	 * 主体内容是否必需.
	 * <p>默认{@code true}, 如果没有正文内容, 将抛出异常.
	 * 如果希望在正文内容为{@code null}时传递{@code null}, 将其切换为{@code false}.
	 */
	boolean required() default true;

}
