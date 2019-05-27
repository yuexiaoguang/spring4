package org.springframework.test.context.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @WebAppConfiguration} is a class-level annotation that is used to
 * declare that the {@code ApplicationContext} loaded for an integration test
 * should be a {@link org.springframework.web.context.WebApplicationContext
 * WebApplicationContext}.
 *
 * <p>The presence of {@code @WebAppConfiguration} on a test class indicates that
 * a {@code WebApplicationContext} should be loaded for the test using a default
 * for the path to the root of the web application. To override the default,
 * specify an explicit resource path via the {@link #value} attribute.
 *
 * <p>Note that {@code @WebAppConfiguration} must be used in conjunction with
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration},
 * either within a single test class or within a test class hierarchy.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WebAppConfiguration {

	/**
	 * The resource path to the root directory of the web application.
	 * <p>A path that does not include a Spring resource prefix (e.g., {@code classpath:},
	 * {@code file:}, etc.) will be interpreted as a file system resource, and a
	 * path should not end with a slash.
	 * <p>Defaults to {@code "src/main/webapp"} as a file system resource. Note
	 * that this is the standard directory for the root of a web application in
	 * a project that follows the standard Maven project layout for a WAR.
	 */
	String value() default "src/main/webapp";

}
