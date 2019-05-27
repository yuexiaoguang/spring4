package org.springframework.test.context.junit4.statements;

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunPrepareTestInstanceCallbacks} is a custom JUnit {@link Statement} which
 * allows the <em>Spring TestContext Framework</em> to be plugged into the JUnit
 * execution chain by calling {@link TestContextManager#prepareTestInstance(Object)
 * prepareTestInstance()} on the supplied {@link TestContextManager}.
 */
public class RunPrepareTestInstanceCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final TestContextManager testContextManager;


	/**
	 * Construct a new {@code RunPrepareTestInstanceCallbacks} statement.
	 * @param next the next {@code Statement} in the execution chain; never {@code null}
	 * @param testInstance the current test instance; never {@code null}
	 * @param testContextManager the {@code TestContextManager} upon which to call
	 * {@code prepareTestInstance()}; never {@code null}
	 */
	public RunPrepareTestInstanceCallbacks(Statement next, Object testInstance, TestContextManager testContextManager) {
		this.next = next;
		this.testInstance = testInstance;
		this.testContextManager = testContextManager;
	}


	/**
	 * Invoke {@link TestContextManager#prepareTestInstance(Object)} and
	 * then evaluate the next {@link Statement} in the execution chain
	 * (typically an instance of {@link RunAfterTestMethodCallbacks}).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.prepareTestInstance(testInstance);
		this.next.evaluate();
	}

}
