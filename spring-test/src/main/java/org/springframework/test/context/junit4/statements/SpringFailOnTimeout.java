package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import org.junit.runners.model.Statement;

import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@code SpringFailOnTimeout}是一个自定义的JUnit {@link Statement},
 * 如果执行链中的下一个语句超过指定的毫秒数,
 * 则通过抛出异常来添加对Spring的{@link org.springframework.test.annotation.Timed @Timed}注解的支持.
 *
 * <p>与JUnit的
 * {@link org.junit.internal.runners.statements.FailOnTimeout FailOnTimeout}相反,
 * 下一个{@code statement}将在与调用者相同的线程中执行, 因此不会被抢先中止.
 */
public class SpringFailOnTimeout extends Statement {

	private final Statement next;

	private final long timeout;


	/**
	 * 为提供的{@code testMethod}构造一个新的{@code SpringFailOnTimeout}语句,
	 * 从提供的方法上的{@code @Timed}注解中检索配置的超时.
	 * 
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testMethod 目前的测试方法
	 */
	public SpringFailOnTimeout(Statement next, Method testMethod) {
		this(next, TestAnnotationUtils.getTimeout(testMethod));
	}

	/**
	 * <p>如果提供的{@code timeout}是{@code 0}, 那么{@code next}语句的执行将不会被定时.
	 * 
	 * @param next 执行链中的下一个{@code Statement}; never {@code null}
	 * @param timeout 当前测试的已配置{@code timeout}, 以毫秒为单位; 不能为负数
	 */
	public SpringFailOnTimeout(Statement next, long timeout) {
		Assert.notNull(next, "next statement must not be null");
		Assert.isTrue(timeout >= 0, "timeout must be non-negative");
		this.next = next;
		this.timeout = timeout;
	}


	/**
	 * 评估执行链中的下一个{@link Statement statement}(通常是{@link SpringRepeat}的实例),
	 * 如果下一个{@code statement}执行的时间超过指定的{@code timeout}, 则抛出{@link TimeoutException}.
	 */
	@Override
	public void evaluate() throws Throwable {
		if (this.timeout == 0) {
			this.next.evaluate();
		}
		else {
			long startTime = System.currentTimeMillis();
			this.next.evaluate();
			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > this.timeout) {
				throw new TimeoutException(
						String.format("Test took %s ms; limit was %s ms.", elapsed, this.timeout));
			}
		}
	}

}
