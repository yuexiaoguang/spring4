package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunAfterTestMethodCallbacks}是一个自定义的JUnit {@link Statement},
 * 它允许通过在提供的{@link TestContextManager}上调用{@link TestContextManager#afterTestMethod afterTestMethod()},
 * 将<em>Spring TestContext Framework</em>插入到JUnit执行链中.
 *
 * <p><strong>NOTE:</strong> 此类需要JUnit 4.9或更高版本.
 */
public class RunAfterTestMethodCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final Method testMethod;

	private final TestContextManager testContextManager;


	/**
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testInstance 当前的测试实例 (never {@code null})
	 * @param testMethod 刚刚在测试实例上执行的测试方法
	 * @param testContextManager 要调用{@code afterTestMethod()}的TestContextManager
	 */
	public RunAfterTestMethodCallbacks(Statement next, Object testInstance, Method testMethod,
			TestContextManager testContextManager) {

		this.next = next;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testContextManager = testContextManager;
	}


	/**
	 * 评估执行链中的下一个{@link Statement},
	 * (通常是{@link org.junit.internal.runners.statements.RunAfters RunAfters}的一个实例),
	 * 捕获抛出的任何异常, 然后调用
	 * {@link TestContextManager#afterTestMethod(Object, Method, Throwable)}提供第一个捕获的异常.
	 * <p>如果{@code afterTestMethod()}的调用抛出异常, 那么也将跟踪该异常. 多个异常将合并到{@link MultipleFailureException}.
	 */
	@Override
	public void evaluate() throws Throwable {
		Throwable testException = null;
		List<Throwable> errors = new ArrayList<Throwable>();
		try {
			this.next.evaluate();
		}
		catch (Throwable ex) {
			testException = ex;
			errors.add(ex);
		}

		try {
			this.testContextManager.afterTestMethod(this.testInstance, this.testMethod, testException);
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		MultipleFailureException.assertEmpty(errors);
	}

}
