package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code TransactionConfiguration} defines class-level metadata for configuring
 * transactional tests.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @deprecated As of Spring Framework 4.2, use {@code @Rollback} or
 * {@code @Commit} at the class level and the {@code transactionManager}
 * qualifier in {@code @Transactional}.
 */
@Deprecated
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransactionConfiguration {

	/**
	 * The bean name of the {@link org.springframework.transaction.PlatformTransactionManager
	 * PlatformTransactionManager} that should be used to drive <em>test-managed transactions</em>.
	 *
	 * <p>The name is only used if there is more than one bean of type
	 * {@code PlatformTransactionManager} in the test's {@code ApplicationContext}.
	 * If there is only one such bean, it is not necessary to specify a bean name.
	 *
	 * <p>Defaults to an empty string, requiring that one of the following is
	 * true:
	 * <ol>
	 * <li>There is only one bean of type {@code PlatformTransactionManager} in
	 * the test's {@code ApplicationContext}.</li>
	 * <li>{@link org.springframework.transaction.annotation.TransactionManagementConfigurer
	 * TransactionManagementConfigurer} has been implemented to specify which
	 * {@code PlatformTransactionManager} bean should be used for annotation-driven
	 * transaction management.</li>
	 * <li>The {@code PlatformTransactionManager} to use is named
	 * {@code "transactionManager"}.</li>
	 * </ol>
	 *
	 * <p><b>NOTE:</b> The XML {@code <tx:annotation-driven>} element also refers
	 * to a bean named {@code "transactionManager"} by default. If you are using both
	 * features in combination, make sure to point to the same transaction manager
	 * bean &mdash; here in {@code @TransactionConfiguration} and also in
	 * {@code <tx:annotation-driven transaction-manager="...">}.
	 */
	String transactionManager() default "";

	/**
	 * Whether <em>test-managed transactions</em> should be rolled back by default.
	 */
	boolean defaultRollback() default true;

}
