package org.springframework.test.context.junit4.statements;

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunPrepareTestInstanceCallbacks}是一个自定义的JUnit {@link Statement},
 * 它允许通过在提供的{@link TestContextManager}上调用{@link TestContextManager#prepareTestInstance(Object) prepareTestInstance()},
 * 将<em>Spring TestContext Framework</em>插入到JUnit执行链中.
 */
public class RunPrepareTestInstanceCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final TestContextManager testContextManager;


	/**
	 * @param next 执行链中的下一个{@code Statement}; never {@code null}
	 * @param testInstance 当前的测试实例; never {@code null}
	 * @param testContextManager 要调用{{@code prepareTestInstance()}的{@code TestContextManager}; never {@code null}
	 */
	public RunPrepareTestInstanceCallbacks(Statement next, Object testInstance, TestContextManager testContextManager) {
		this.next = next;
		this.testInstance = testInstance;
		this.testContextManager = testContextManager;
	}


	/**
	 * 调用{@link TestContextManager#prepareTestInstance(Object)}, 然后评估执行链中的下一个{@link Statement}
	 * (通常是{@link RunAfterTestMethodCallbacks}实例).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.prepareTestInstance(testInstance);
		this.next.evaluate();
	}

}
