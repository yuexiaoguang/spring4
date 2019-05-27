package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test-specific annotation to indicate that a test method has to finish
 * execution in a {@linkplain #millis() specified time period}.
 *
 * <p>If the text execution takes longer than the specified time period, then
 * the test is considered to have failed.
 *
 * <p>Note that the time period includes execution of the test method itself,
 * any {@linkplain Repeat repetitions} of the test, and any <em>set up</em> or
 * <em>tear down</em> of the test fixture.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

	/**
	 * The maximum amount of time (in milliseconds) that a test execution can
	 * take without being marked as failed due to taking too long.
	 */
	long millis();

}
