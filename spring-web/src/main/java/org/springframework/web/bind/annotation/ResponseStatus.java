package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

/**
 * 使用应返回的状态{@link #code}和{@link #reason}标记方法或异常类.
 *
 * <p>调用处理器方法时, 状态码将应用于HTTP响应, 并覆盖通过其他方式设置的状态信息,
 * 例如{@code ResponseEntity}或{@code "redirect:"}.
 *
 * <p><strong>Warning</strong>: 在异常类上使用此注解时, 或者在设置此注解的{@code reason}属性时,
 * 将使用{@code HttpServletResponse.sendError}方法.
 *
 * <p>使用{@code HttpServletResponse.sendError}, 响应被认为是完整的, 不应再写入其它内容.
 * 此外, Servlet容器通常会写入HTML错误页面, 因此使用{@code reason}不适合REST API.
 * 对于这种情况, 最好使用{@link org.springframework.http.ResponseEntity}作为返回类型,
 * 并避免使用{@code @ResponseStatus}.
 *
 * <p>请注意, 控制器类也可以使用{@code @ResponseStatus}注解, 然后由所有{@code @RequestMapping}方法继承.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

	/**
	 * Alias for {@link #code}.
	 */
	@AliasFor("code")
	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的<em>状态码</em>.
	 * <p>默认{@link HttpStatus#INTERNAL_SERVER_ERROR}, 通常应将其更改为更合适的.
	 */
	@AliasFor("value")
	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的<em>原因</em>.
	 */
	String reason() default "";

}
