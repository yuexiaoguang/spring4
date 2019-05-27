package org.springframework.test.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.*;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.*;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods annotated with the
 * {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>This listener supports test methods with the
 * {@linkplain DirtiesContext#methodMode method mode} set to
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD} and test classes with the
 * {@linkplain DirtiesContext#classMode() class mode} set to
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD} or
 * {@link ClassMode#AFTER_CLASS AFTER_CLASS}. For support for <em>BEFORE</em>
 * modes, see {@link DirtiesContextBeforeModesTestExecutionListener}.
 *
 * <p>When {@linkplain TestExecutionListeners#mergeMode merging}
 * {@code TestExecutionListeners} with the defaults, this listener will
 * automatically be ordered after the {@link DependencyInjectionTestExecutionListener};
 * otherwise, this listener must be manually configured to execute after the
 * {@code DependencyInjectionTestExecutionListener}.
 */
public class DirtiesContextTestExecutionListener extends AbstractDirtiesContextTestExecutionListener {

	/**
	 * Returns {@code 3000}.
	 */
	@Override
	public final int getOrder() {
		return 3000;
	}

	/**
	 * If the current test method of the supplied {@linkplain TestContext test
	 * context} is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#methodMode() method mode} is set to {@link
	 * MethodMode#AFTER_METHOD AFTER_METHOD}, or if the test class is
	 * annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}, the
	 * {@linkplain ApplicationContext application context} of the test context
	 * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, AFTER_METHOD, AFTER_EACH_TEST_METHOD);
	}

	/**
	 * If the test class of the supplied {@linkplain TestContext test context}
	 * is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#AFTER_CLASS AFTER_CLASS}, the {@linkplain ApplicationContext
	 * application context} of the test context will be
	 * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, AFTER_CLASS);
	}

}
