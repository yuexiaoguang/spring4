package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将方法标记为<i>异步</i>执行的候选的注解.
 * 也可以在类型级别使用, 在这种情况下, 所有类型的方法都被视为异步.
 *
 * <p>就目标方法签名而言, 支持任何参数类型.
 * 但是, 返回类型被约束为{@code void}或{@link java.util.concurrent.Future}.
 * 在后一种情况下, 可以声明更具体的{@link org.springframework.util.concurrent.ListenableFuture}
 * 或{@link java.util.concurrent.CompletableFuture}类型, 允许与异步任务进行更丰富的交互, 并与进一步的处理步骤直接进行组合.
 *
 * <p>从代理返回的{@code Future}句柄将是一个实际的异步{@code Future}, 可用于跟踪异步方法执行的结果.
 * 但是, 由于目标方法需要实现相同的签名, 因此必须返回一个临时的{@code Future}句柄, 该句柄只传递一个值:
 * e.g. Spring's {@link AsyncResult}, EJB 3.1's {@link javax.ejb.AsyncResult},
 * {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {

	/**
	 * 指定异步操作的限定符值.
	 * <p>可用于确定执行此方法时要使用的目标执行器,
	 * 匹配特定{@link java.util.concurrent.Executor Executor}或
	 * {@link org.springframework.core.task.TaskExecutor TaskExecutor} bean定义的限定符值(或bean名称).
	 * <p>在类级{@code @Async}注解上指定时, 表示给定的执行器应该用于类中的所有方法.
	 * 方法级别使用的{@code Async#value}始终覆盖在类级别设置的任何值.
	 */
	String value() default "";

}
