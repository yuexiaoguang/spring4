package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet resource requests onto handler methods.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping()
public @interface ResourceMapping {

	/**
	 * The id of the resource to be handled.
	 * This id uniquely identifies a resource within a portlet mode.
	 * <p>If not specified, the handler method will be invoked for any
	 * resource request within its general mapping.
	 */
	String value() default "";

}
