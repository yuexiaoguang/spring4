package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code TransactionConfiguration}定义用于配置事务测试的类级元数据.
 *
 * <p>从Spring Framework 4.0开始, 此注解可用作<em>元注解</em>来创建自定义<em>组合注解</em>.
 *
 * @deprecated 从Spring Framework 4.2开始, 在类级别使用{@code @Rollback}或{@code @Commit},
 * 在{@code @Transactional}中使用{@code transactionManager}限定符.
 */
@Deprecated
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransactionConfiguration {

	/**
	 * 应用于驱动<em>测试管理的事务</em>的
	 * {@link org.springframework.transaction.PlatformTransactionManager PlatformTransactionManager}的bean名称.
	 *
	 * <p>只有在测试{@code ApplicationContext}中有多个{@code PlatformTransactionManager}类型的bean时才使用该名称.
	 * 如果只有一个这样的bean, 则不必指定bean名称.
	 *
	 * <p>默认为空字符串, 要求满足以下条件之一:
	 * <ol>
	 * <li>测试的{@code ApplicationContext}中只有一个{@code PlatformTransactionManager}类型的bean.</li>
	 * <li>已经实现了
	 * {@link org.springframework.transaction.annotation.TransactionManagementConfigurer TransactionManagementConfigurer}
	 * 来指定哪个{@code PlatformTransactionManager} bean应该用于注解驱动的事务管理.</li>
	 * <li>要使用的{@code PlatformTransactionManager}名为{@code "transactionManager"}.</li>
	 * </ol>
	 *
	 * <p><b>NOTE:</b> XML {@code <tx:annotation-driven>}元素也默认引用名为{@code "transactionManager"}的bean.
	 * 如果要组合使用这两个功能, 请确保指向相同的事务管理器bean &mdash;
	 * 在{@code @TransactionConfiguration}以及{@code <tx:annotation-driven transaction-manager="...">}中.
	 */
	String transactionManager() default "";

	/**
	 * 是否应默认回滚<em>测试管理的事务</em>.
	 */
	boolean defaultRollback() default true;

}
