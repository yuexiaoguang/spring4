package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Rollback} is a test annotation that is used to indicate whether
 * a <em>test-managed transaction</em> should be <em>rolled back</em> after
 * the test method has completed.
 *
 * <p>Consult the class-level Javadoc for
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * for an explanation of <em>test-managed transactions</em>.
 *
 * <p>When declared as a class-level annotation, {@code @Rollback} defines
 * the default rollback semantics for all test methods within the test class
 * hierarchy. When declared as a method-level annotation, {@code @Rollback}
 * defines rollback semantics for the specific test method, potentially
 * overriding class-level default commit or rollback semantics.
 *
 * <p>As of Spring Framework 4.2, {@code @Commit} can be used as direct
 * replacement for {@code @Rollback(false)}.
 *
 * <p><strong>Warning</strong>: Declaring {@code @Commit} and {@code @Rollback}
 * on the same test method or on the same test class is unsupported and may
 * lead to unpredictable results.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. Consult the source code for
 * {@link Commit @Commit} for a concrete example.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Rollback {

	/**
	 * Whether the <em>test-managed transaction</em> should be rolled back
	 * after the test method has completed.
	 * <p>If {@code true}, the transaction will be rolled back; otherwise,
	 * the transaction will be committed.
	 * <p>Defaults to {@code true}.
	 */
	boolean value() default true;

}
