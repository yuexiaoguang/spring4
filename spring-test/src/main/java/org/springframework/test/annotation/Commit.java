package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Commit}是一个测试注解, 用于指示在测试方法完成后<em>测试管理的事务</em>应<em>提交</em>.
 *
 * <p>有关<em>测试管理的事务</em>的说明, 请参阅
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}的类级Javadoc.
 *
 * <p>当声明为类级注解时, {@code @Commit}定义测试类层次结构中所有测试方法的默认提交语义.
 * 声明为方法级注释时, {@code @Commit}定义特定测试方法的提交语义, 可能会覆盖类级别的默认提交或回滚语义.
 *
 * <p><strong>Warning</strong>: {@code @Commit}可以直接替代{@code @Rollback(false)};
 * 但是, 它<strong>不</strong>应该与{@code @Rollback}一起声明.
 * 在同一测试方法或同一测试类上声明{@code @Commit}和{@code @Rollback}不受支持, 可能导致不可预测的结果.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Rollback(false)
public @interface Commit {
}
