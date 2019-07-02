package org.springframework.test.context.junit4.statements;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunAfterTestClassCallbacks}是一个自定义的JUnit {@link Statement},
 * 它允许通过在提供的{@link TestContextManager}上调用{@link TestContextManager#afterTestClass afterTestClass()}
 * 将<em>Spring TestContext Framework</em>插入到JUnit执行链中.
 *
 * <p><strong>NOTE:</strong> 此类需要JUnit 4.9或更高版本.
 */
public class RunAfterTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * @param next 执行链中的下一个{@code Statement}
	 * @param testContextManager 要调用{@code afterTestClass()}的TestContextManager
	 */
	public RunAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}


	/**
	 * 评估执行链中的下一个{@link Statement}(通常是
	 * {@link org.junit.internal.runners.statements.RunAfters RunAfters}的一个实例),
	 * 捕获抛出的任何异常, 然后调用{@link TestContextManager#afterTestClass()}.
	 * <p>如果{@code afterTestClass()}的调用抛出异常, 它也将被跟踪. 多个异常将合并到{@link MultipleFailureException}.
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
