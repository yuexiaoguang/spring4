package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.test.context.TestContext;

/**
 * {@code TestExecutionListener}, 它提供对依赖注入和测试实例初始化的支持.
 */
public class DependencyInjectionTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * {@link TestContext}属性的属性名称，用于指示测试实例的依赖关系
	 * 是否应在{@link #beforeTestMethod(TestContext) beforeTestMethod()}中<em>重新注入</em>.
	 * 请注意, 在任何情况下都会在
	 * {@link #prepareTestInstance(TestContext) prepareTestInstance()}中注入依赖项.
	 * <p>因此, {@link TestContext}的客户端 (e.g., 其他
	 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners})
	 * 可以选择设置此属性以表示应该在执行各个测试方法<em>之间</em>重新注入依赖项.
	 * <p>允许的值包括{@link Boolean#TRUE} 和 {@link Boolean#FALSE}.
	 */
	public static final String REINJECT_DEPENDENCIES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
		DependencyInjectionTestExecutionListener.class, "reinjectDependencies");

	private static final Log logger = LogFactory.getLog(DependencyInjectionTestExecutionListener.class);


	/**
	 * Returns {@code 2000}.
	 */
	@Override
	public final int getOrder() {
		return 2000;
	}

	/**
	 * 对所提供的{@link TestContext 测试上下文}的{@link TestContext#getTestInstance() 测试实例}执行依赖注入,
	 * 通过{@link AutowireCapableBeanFactory#autowireBeanProperties(Object, int, boolean) 自动注入}
	 * 和 {@link AutowireCapableBeanFactory#initializeBean(Object, String) 初始化}测试实例,
	 * 通过它自己的{@link TestContext#getApplicationContext() 应用程序上下文} (不检查依赖项).
	 * <p>{@link #REINJECT_DEPENDENCIES_ATTRIBUTE}随后将从测试上下文中删除, 无论其值如何.
	 */
	@Override
	public void prepareTestInstance(final TestContext testContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Performing dependency injection for test context [" + testContext + "].");
		}
		injectDependencies(testContext);
	}

	/**
	 * 如果提供的{@link TestContext 测试上下文}中的{@link #REINJECT_DEPENDENCIES_ATTRIBUTE}的值为{@link Boolean#TRUE},
	 * 则此方法将与{@link #prepareTestInstance(TestContext) prepareTestInstance()}具有相同的效果;
	 * 否则, 此方法将无效.
	 */
	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Reinjecting dependencies for test context [" + testContext + "].");
			}
			injectDependencies(testContext);
		}
	}

	/**
	 * 对提供的{@link TestContext}执行依赖注入和bean初始化,
	 * 如{@link #prepareTestInstance(TestContext) prepareTestInstance()}中所述.
	 * <p>{@link #REINJECT_DEPENDENCIES_ATTRIBUTE}随后将从测试上下文中删除, 无论其值如何.
	 * 
	 * @param testContext 应该执行依赖注入的测试上下文 (never {@code null})
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	protected void injectDependencies(final TestContext testContext) throws Exception {
		Object bean = testContext.getTestInstance();
		AutowireCapableBeanFactory beanFactory = testContext.getApplicationContext().getAutowireCapableBeanFactory();
		beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
		beanFactory.initializeBean(bean, testContext.getTestClass().getName());
		testContext.removeAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE);
	}
}
