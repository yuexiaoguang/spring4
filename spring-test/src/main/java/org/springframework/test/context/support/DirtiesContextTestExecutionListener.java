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
 * {@code TestExecutionListener}, 它支持将与测试关联的{@code ApplicationContext}标记为<em>dirty</em>,
 * 用于使用{@link DirtiesContext @DirtiesContext}注解的测试类和测试方法.
 *
 * <p>此监听器支持将 {@linkplain DirtiesContext#methodMode 方法模式}设置为
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD}的测试方法,
 * 以及将 {@linkplain DirtiesContext#classMode() 类模式}设置为
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}
 * 或{@link ClassMode#AFTER_CLASS AFTER_CLASS}的测试类.
 * 有关<em>BEFORE</em>模式的支持, 请参阅{@link DirtiesContextBeforeModesTestExecutionListener}.
 *
 * <p>当{@linkplain TestExecutionListeners#mergeMode 合并的} {@code TestExecutionListeners}具有默认值时,
 * 此监听器将在{@link DependencyInjectionTestExecutionListener}之后自动排序;
 * 否则, 必须手动配置此监听器以在{@code DependencyInjectionTestExecutionListener}之后执行.
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
	 * 如果提供的{@linkplain TestContext 测试上下文}的当前测试方法使用{@code @DirtiesContext}注解,
	 * 并且{@linkplain DirtiesContext#methodMode() 方法模式}设置为{@link MethodMode#AFTER_METHOD AFTER_METHOD},
	 * 或者如果测试类使用{@code @DirtiesContext}注解, 并且{@linkplain DirtiesContext#classMode() 类模式}
	 * 设置为{@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD},
	 * 则测试上下文的{@linkplain ApplicationContext 应用程序上下文}将{@linkplain TestContext#markApplicationContextDirty 标记为脏},
	 * 并且测试上下文中的
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * 将设置为{@code true}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, AFTER_METHOD, AFTER_EACH_TEST_METHOD);
	}

	/**
	 * 如果提供的{@linkplain TestContext 测试上下文}的测试类使用{@code @DirtiesContext}注解,
	 * 并且{@linkplain DirtiesContext#classMode() 类模式}设置为{@link ClassMode#AFTER_CLASS AFTER_CLASS},
	 * 则测试上下文的{@linkplain ApplicationContext 应用程序上下文}将{@linkplain TestContext#markApplicationContextDirty 标记为脏},
	 * 并且测试上下文中的
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * 将设置为{@code true}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, AFTER_CLASS);
	}

}
