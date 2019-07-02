package org.springframework.test.context.junit4.statements;

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunBeforeTestClassCallbacks}是一个自定义的JUnit {@link Statement},
 * 它允许通过在提供的{@link TestContextManager}上调用{@link TestContextManager#beforeTestClass() beforeTestClass()},
 * 将<em>Spring TestContext Framework</em>插入到JUnit执行链中.
 */
public class RunBeforeTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testContextManager 要调用{@code beforeTestClass()}的TestContextManager
	 */
	public RunBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}


	/**
	 * 调用{@link TestContextManager#beforeTestClass()}, 然后评估执行链中的下一个{@link Statement}
	 * (通常是{@link org.junit.internal.runners.statements.RunBefores RunBefores}实例).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestClass();
		this.next.evaluate();
	}

}
