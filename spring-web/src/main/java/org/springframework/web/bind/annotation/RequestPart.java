package org.springframework.web.bind.annotation;

import java.beans.PropertyEditor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;

/**
 * 可用于将"multipart/form-data"请求的一部分与方法参数相关联.
 *
 * <p>支持的方法参数类型包括{@link MultipartFile}结合Spring的{@link MultipartResolver}抽象,
 * {@code javax.servlet.http.Part}结合Servlet 3.0 multipart请求, 或者其他任何方法参数,
 * 考虑到请求部分的'Content-Type' header, 部分的内容通过{@link HttpMessageConverter}传递.
 * 这与 @{@link RequestBody}根据非multipart常规请求的内容解析参数的行为类似.
 *
 * <p>请注意, @{@link RequestParam}注解也可用于将"multipart/form-data"请求的一部分与支持相同方法参数类型的方法参数相关联.
 * 主要区别在于, 当method参数不是String时, @{@link RequestParam}依赖于通过注册的{@link Converter}
 * 或{@link PropertyEditor}进行类型转换,
 * 而@{@link RequestPart}依赖于{@link HttpMessageConverter}, 考虑请求部分的'Content-Type' header.
 * @{@link RequestParam}可能与name-value表单字段一起使用,
 * 而@{@link RequestPart}可能与包含更复杂内容的部分一起使用 (e.g. JSON, XML).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPart {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的{@code "multipart/form-data"}请求中的部分名称.
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 是否必须.
	 * <p>默认{@code true}, 如果请求中缺少该部分, 则会抛出异常.
	 * 如果请求中不存在该部分, 则为{@code false}, 将其切换为{@code false}.
	 */
	boolean required() default true;

}
