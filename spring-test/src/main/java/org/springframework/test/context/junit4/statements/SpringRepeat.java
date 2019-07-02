package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.TestAnnotationUtils;

/**
 * {@code SpringRepeat}是一个自定义的JUnit {@link Statement},
 * 通过重复测试指定的次数, 添加对Spring的{@link org.springframework.test.annotation.Repeat @Repeat}注解的支持.
 */
public class SpringRepeat extends Statement {

	protected static final Log logger = LogFactory.getLog(SpringRepeat.class);

	private final Statement next;

	private final Method testMethod;

	private final int repeat;


	/**
	 * 为提供的{@code testMethod}构造一个新的{@code SpringRepeat}语句,
	 * 从提供的方法上的{@code @Repeat}注解中检索配置的重复计数.
	 * 
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testMethod 当前的测试方法
	 */
	public SpringRepeat(Statement next, Method testMethod) {
		this(next, testMethod, TestAnnotationUtils.getRepeatCount(testMethod));
	}

	/**
	 * 为提供的{@code testMethod}和{@code repeat}计数构造一个新的{@code SpringRepeat}语句.
	 * 
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testMethod 当前的测试方法
	 * @param repeat 当前测试方法的配置重复计数
	 */
	public SpringRepeat(Statement next, Method testMethod, int repeat) {
		this.next = next;
		this.testMethod = testMethod;
		this.repeat = Math.max(1, repeat);
	}


	/**
	 * 使用指定的重复计数重复执行执行链中的下一个{@link Statement 语句}.
	 */
	@Override
	public void evaluate() throws Throwable {
		for (int i = 0; i < this.repeat; i++) {
			if (this.repeat > 1 && logger.isInfoEnabled()) {
				logger.info(String.format("Repetition %d of test %s#%s()", (i + 1),
						this.testMethod.getDeclaringClass().getSimpleName(), this.testMethod.getName()));
			}
			this.next.evaluate();
		}
	}
}
