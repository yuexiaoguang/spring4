package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Test annotation which indicates that the annotated {@code void} method
 * should be executed <em>after</em> a transaction is ended for a test method
 * configured to run within a transaction via Spring's {@code @Transactional}
 * annotation.
 *
 * <p>{@code @AfterTransaction} methods declared in superclasses or as interface
 * default methods will be executed after those of the current test class.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * <p>As of Spring Framework 4.3, {@code @AfterTransaction} may also be
 * declared on Java 8 based interface default methods.
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterTransaction {
}
