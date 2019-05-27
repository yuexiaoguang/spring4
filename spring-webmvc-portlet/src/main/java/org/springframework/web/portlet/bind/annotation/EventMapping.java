package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet event requests onto handler methods.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping()
public @interface EventMapping {

	/**
	 * The name of the event to be handled.
	 * This name uniquely identifies an event within a portlet mode.
	 * <p>Typically the local name of the event, but fully qualified names
	 * with a "{...}" namespace part will be mapped correctly as well.
	 * <p>If not specified, the handler method will be invoked for any
	 * event request within its general mapping.
	 */
	String value() default "";

}
