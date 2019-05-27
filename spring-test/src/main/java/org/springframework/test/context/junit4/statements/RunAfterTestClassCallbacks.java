package org.springframework.test.context.junit4.statements;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunAfterTestClassCallbacks} is a custom JUnit {@link Statement} which allows
 * the <em>Spring TestContext Framework</em> to be plugged into the JUnit execution chain
 * by calling {@link TestContextManager#afterTestClass afterTestClass()} on the supplied
 * {@link TestContextManager}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 */
public class RunAfterTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * Construct a new {@code RunAfterTestClassCallbacks} statement.
	 * @param next the next {@code Statement} in the execution chain
	 * @param testContextManager the TestContextManager upon which to call
	 * {@code afterTestClass()}
	 */
	public RunAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}


	/**
	 * Evaluate the next {@link Statement} in the execution chain (typically an instance of
	 * {@link org.junit.internal.runners.statements.RunAfters RunAfters}), catching any
	 * exceptions thrown, and then invoke {@link TestContextManager#afterTestClass()}.
	 * <p>If the invocation of {@code afterTestClass()} throws an exception, it will also
	 * be tracked. Multiple exceptions will be combined into a {@link MultipleFailureException}.
	 */
	@Override
	public void evaluate() throws Throwable {
		List<Throwable> errors = new ArrayList<Throwable>();
		try {
			this.next.evaluate();
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		try {
			this.testContextManager.afterTestClass();
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		MultipleFailureException.assertEmpty(errors);
	}

}
