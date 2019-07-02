package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Rollback}是一个测试注解, 用于指示在测试方法完成后<em>测试管理的事务</em>是否应<em>回滚</em>.
 *
 * <p>有关<em>测试管理的事务</em>的说明, 请参阅
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}的类级Javadoc.
 *
 * <p>声明为类级注解时, {@code @Rollback}定义测试类层次结构中所有测试方法的默认回滚语义.
 * 声明为方法级注解时, {@code @Rollback}定义特定测试方法的回滚语义, 可能会覆盖类级别的默认提交或回滚语义.
 *
 * <p>从Spring Framework 4.2开始, {@code @Commit}可以直接替代{@code @Rollback(false)}.
 *
 * <p><strong>Warning</strong>: 在同一测试方法或同一测试类上声明{@code @Commit}和{@code @Rollback}不受支持,
 * 可能导致不可预测的结果.
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</ em>.
 * 有关具体示例, 请参阅{@link Commit @Commit}的源代码.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Rollback {

	/**
	 * 是否应在测试方法完成后回滚<em>测试管理的事务</em>.
	 * <p>如果{@code true}, 事务将被回滚; 否则, 事务将被提交.
	 * <p>默认{@code true}.
	 */
	boolean value() default true;

}
