package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation to indicate that a test method should be invoked repeatedly.
 *
 * <p>Note that the scope of execution to be repeated includes execution of the
 * test method itself as well as any <em>set up</em> or <em>tear down</em> of
 * the test fixture.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Repeat {

	/**
	 * The number of times that the annotated test method should be repeated.
	 */
	int value() default 1;

}
