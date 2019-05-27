package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Commit} is a test annotation that is used to indicate that a
 * <em>test-managed transaction</em> should be <em>committed</em> after
 * the test method has completed.
 *
 * <p>Consult the class-level Javadoc for
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * for an explanation of <em>test-managed transactions</em>.
 *
 * <p>When declared as a class-level annotation, {@code @Commit} defines
 * the default commit semantics for all test methods within the test class
 * hierarchy. When declared as a method-level annotation, {@code @Commit}
 * defines commit semantics for the specific test method, potentially
 * overriding class-level default commit or rollback semantics.
 *
 * <p><strong>Warning</strong>: {@code @Commit} can be used as direct
 * replacement for {@code @Rollback(false)}; however, it should
 * <strong>not</strong> be declared alongside {@code @Rollback}. Declaring
 * {@code @Commit} and {@code @Rollback} on the same test method or on the
 * same test class is unsupported and may lead to unpredictable results.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Rollback(false)
public @interface Commit {
}
