package org.springframework.test.context.support;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener}实现的抽象基类, 支持将与测试关联的{@code ApplicationContext}标记为<em>dirty</em>,
 * 用于使用{@link DirtiesContext @DirtiesContext}注解的测试类和测试方法.
 *
 * <p>此类的核心功能是从Spring Framework 4.2中的{@link DirtiesContextTestExecutionListener}中提取的.
 */
public abstract class AbstractDirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(AbstractDirtiesContextTestExecutionListener.class);


	@Override
	public abstract int getOrder();

	/**
	 * 将提供的{@linkplain TestContext 测试上下文} 的{@linkplain ApplicationContext 应用程序上下文}标记为
	 * {@linkplain TestContext#markApplicationContextDirty(DirtiesContext.HierarchyMode) dirty},
	 * 并将测试上下文中的
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * 设置为{@code true}.
	 * 
	 * @param testContext 其应用程序上下文应标记为脏的测试上下文
	 * @param hierarchyMode 如果上下文是层次结构的一部分, 要应用上下文缓存清除模式; may be {@code null}
	 */
	protected void dirtyContext(TestContext testContext, HierarchyMode hierarchyMode) {
		testContext.markApplicationContextDirty(hierarchyMode);
		testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, Boolean.TRUE);
	}

	/**
	 * 如果合适(i.e., 根据所需模式), 通过弄脏上下文, 实际执行{@link #beforeTestMethod}和{@link #afterTestMethod}.
	 * 
	 * @param testContext 应用程序上下文可能被标记为脏的测试上下文; never {@code null}
	 * @param requiredMethodMode 在当前阶段将上下文标记为脏的方法模式; never {@code null}
	 * @param requiredClassMode 在当前阶段将上下文标记为脏的类模式; never {@code null}
	 * 
	 * @throws Exception allows any exception to propagate
	 */
	protected void beforeOrAfterTestMethod(TestContext testContext, MethodMode requiredMethodMode,
			ClassMode requiredClassMode) throws Exception {

		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(requiredMethodMode, "requiredMethodMode must not be null");
		Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

		Class<?> testClass = testContext.getTestClass();
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		DirtiesContext methodAnn = AnnotatedElementUtils.findMergedAnnotation(testMethod, DirtiesContext.class);
		DirtiesContext classAnn = AnnotatedElementUtils.findMergedAnnotation(testClass, DirtiesContext.class);
		boolean methodAnnotated = (methodAnn != null);
		boolean classAnnotated = (classAnn != null);
		MethodMode methodMode = (methodAnnotated ? methodAnn.methodMode() : null);
		ClassMode classMode = (classAnnotated ? classAnn.classMode() : null);

		if (logger.isDebugEnabled()) {
			String phase = (requiredClassMode.name().startsWith("BEFORE") ? "Before" : "After");
			logger.debug(String.format("%s test method: context %s, class annotated with @DirtiesContext [%s] "
					+ "with mode [%s], method annotated with @DirtiesContext [%s] with mode [%s].", phase, testContext,
				classAnnotated, classMode, methodAnnotated, methodMode));
		}

		if ((methodMode == requiredMethodMode) || (classMode == requiredClassMode)) {
			HierarchyMode hierarchyMode = (methodAnnotated ? methodAnn.hierarchyMode() : classAnn.hierarchyMode());
			dirtyContext(testContext, hierarchyMode);
		}
	}

	/**
	 * 通过在适当时弄脏上下文(i.e., 根据所需模式), 实际执行{@link #beforeTestClass} 和 {@link #afterTestClass}.
	 * 
	 * @param testContext 应用程序上下文可能被标记为脏的测试上下文; never {@code null}
	 * @param requiredClassMode 在当前阶段将上下文标记为脏的类模式; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	protected void beforeOrAfterTestClass(TestContext testContext, ClassMode requiredClassMode) throws Exception {
		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");

		DirtiesContext dirtiesContext = AnnotatedElementUtils.findMergedAnnotation(testClass, DirtiesContext.class);
		boolean classAnnotated = (dirtiesContext != null);
		ClassMode classMode = (classAnnotated ? dirtiesContext.classMode() : null);

		if (logger.isDebugEnabled()) {
			String phase = (requiredClassMode.name().startsWith("BEFORE") ? "Before" : "After");
			logger.debug(String.format(
				"%s test class: context %s, class annotated with @DirtiesContext [%s] with mode [%s].", phase,
				testContext, classAnnotated, classMode));
		}

		if (classMode == requiredClassMode) {
			dirtyContext(testContext, dirtiesContext.hierarchyMode());
		}
	}

}
