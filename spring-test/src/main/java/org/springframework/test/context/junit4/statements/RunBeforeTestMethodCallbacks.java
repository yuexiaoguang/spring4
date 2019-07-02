package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunBeforeTestMethodCallbacks}是一个自定义的JUnit {@link Statement},
 * 它允许通过在提供的{@link TestContextManager}上调用{@link TestContextManager#beforeTestMethod(Object, Method) beforeTestMethod()},
 * 将<em>Spring TestContext Framework</em>插入到JUnit执行链中.
 */
public class RunBeforeTestMethodCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final Method testMethod;

	private final TestContextManager testContextManager;


	/**
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testInstance 当前的测试实例 (never {@code null})
	 * @param testMethod 即将在测试实例上执行的测试方法
	 * @param testContextManager 要调用{@code beforeTestMethod()}的TestContextManager
	 */
	public RunBeforeTestMethodCallbacks(Statement next, Object testInstance, Method testMethod,
			TestContextManager testContextManager) {

		this.next = next;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testContextManager = testContextManager;
	}


	/**
	 * 调用{@link TestContextManager#beforeTestMethod(Object, Method)}, 然后评估执行链中的下一个{@link Statement}
	 * (通常是{@link org.junit.internal.runners.statements.RunBefores RunBefores}实例).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestMethod(this.testInstance, this.testMethod);
		this.next.evaluate();
	}

}
