package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet render requests onto handler methods.
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
	 * The window state that the annotated render method applies for.
	 * <p>If not specified, the render method will be invoked for any
	 * window state within its general mapping.
	 * <p>Standard Portlet specification values are supported: {@code "NORMAL"},
	 * {@code "MAXIMIZED"}, {@code "MINIMIZED"}.
	 * <p>Custom window states can be used as well, as supported by the portal.
	 * @since 4.2
	 */
	@AliasFor("value")
	String windowState() default "";

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of {@code "myParam=myValue"}
	 * style expressions, with a request only mapped if each such parameter is found
	 * to have the given value. {@code "myParam"} style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, {@code "!myParam"} style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 */
	String[] params() default {};

}
