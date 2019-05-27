package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Collection of utility methods for working with Spring's core testing annotations.
 */
public class TestAnnotationUtils {

	/**
	 * Get the {@code timeout} configured via the {@link Timed @Timed}
	 * annotation on the supplied {@code method}.
	 * <p>Negative configured values will be converted to {@code 0}.
	 * @return the configured timeout, or {@code 0} if the method is not
	 * annotated with {@code @Timed}
	 */
	public static long getTimeout(Method method) {
		Timed timed = AnnotatedElementUtils.findMergedAnnotation(method, Timed.class);
		if (timed == null) {
			return 0;
		}
		return Math.max(0, timed.millis());
	}

	/**
	 * Get the repeat count configured via the {@link Repeat @Repeat}
	 * annotation on the supplied {@code method}.
	 * <p>Non-negative configured values will be converted to {@code 1}.
	 * @return the configured repeat count, or {@code 1} if the method is
	 * not annotated with {@code @Repeat}
	 */
	public static int getRepeatCount(Method method) {
		Repeat repeat = AnnotatedElementUtils.findMergedAnnotation(method, Repeat.class);
		if (repeat == null) {
			return 1;
		}
		return Math.max(1, repeat.value());
	}

}
