package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ProfileValueSourceConfiguration} is a class-level annotation which
 * is used to specify what type of {@link ProfileValueSource} to use when
 * retrieving <em>profile values</em> configured via the {@link IfProfileValue
 * &#064;IfProfileValue} annotation.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ProfileValueSourceConfiguration {

	/**
	 * The type of {@link ProfileValueSource} to use when retrieving
	 * <em>profile values</em>.
	 */
	Class<? extends ProfileValueSource> value() default SystemProfileValueSource.class;

}
