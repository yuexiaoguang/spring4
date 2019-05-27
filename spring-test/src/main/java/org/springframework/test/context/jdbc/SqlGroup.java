package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link Sql @Sql} annotations.
 *
 * <p>Can be used natively, declaring several nested {@code @Sql} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable
 * annotations, where {@code @Sql} can simply be declared several times on the
 * same class or method, implicitly generating this container annotation.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlGroup {

	Sql[] value();

}
