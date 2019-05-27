package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet action requests onto handler methods.
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
	 * The name of the action, according to the Portlet 2.0
	 * {@code javax.portlet.action} parameter.
	 * <p>If not specified, the annotated method will be used as a default
	 * handler: i.e. for action requests where no specific action mapping
	 * was found.
	 * <p>Note that all such annotated action methods only apply within the
	 * {@code @RequestMapping} constraints of the containing handler class.
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

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
