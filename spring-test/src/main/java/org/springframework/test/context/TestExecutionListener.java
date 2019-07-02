package org.springframework.test.context;

/**
 * {@code TestExecutionListener}定义了一个<em>监听器</em> API,
 * 用于对监听器注册的{@link TestContextManager}发布的测试执行事件做出反应.
 *
 * <p>具体实现必须提供{@code public} 无参构造函数, 以便可以通过工具和配置机制透明地实例化监听器.
 *
 * <p>实现可以选择通过{@link org.springframework.core.Ordered Ordered}接口或
 * {@link org.springframework.core.annotation.Order @Order}注解声明它们应该在默认监听器链中排序的位置.
 * See {@link TestContextBootstrapper#getTestExecutionListeners()} for details.
 *
 * <p>Spring提供了以下开箱即用的实现 (所有这些都实现了{@code Ordered}):
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener ServletTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener DirtiesContextBeforeModesTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener DependencyInjectionTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener SqlScriptsTestExecutionListener}</li>
 * </ul>
 */
public interface TestExecutionListener {

	/**
	 * 在执行类中的所有测试之前预处理测试类.
	 * <p>应该在类生命周期回调之前的特定于框架之前调用此方法.
	 * <p>如果给定的测试框架不支持在类生命周期回调之前, 则不会为该框架调用此方法.
	 * 
	 * @param testContext 测试的测试上下文; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	void beforeTestClass(TestContext testContext) throws Exception;

	/**
	 * 准备所提供的{@link TestContext 测试上下文}的{@link Object 测试实例}, 例如通过注入依赖项.
	 * <p>应该在实例化测试实例之后, 但在任何特定于框架的生命周期回调之前, 立即调用此方法.
	 * 
	 * @param testContext 测试的测试上下文; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	void prepareTestInstance(TestContext testContext) throws Exception;

	/**
	 * 在提供的{@link TestContext 测试上下文}中的{@link java.lang.reflect.Method 测试方法}执行之前,
	 * 预处理测试, 例如通过设置测试环境.
	 * <p>应该在生命周期回调之前的特定框架之前立即调用此方法.
	 * 
	 * @param testContext 测试上下文, 测试方法将在其中执行; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	void beforeTestMethod(TestContext testContext) throws Exception;

	/**
	 * 在提供的{@link TestContext 测试上下文}中执行{@link java.lang.reflect.Method 测试方法}之后对测试进行后处理,
	 * 例如清理测试环境.
	 * <p>应该在生命周期回调后的特定于框架之后立即调用此方法.
	 * 
	 * @param testContext 执行测试方法的测试上下文; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	void afterTestMethod(TestContext testContext) throws Exception;

	/**
	 * 在执行类中的所有测试之后对测试类进行后处理.
	 * <p>应该在特定于框架的类生命周期回调之后立即调用此方法.
	 * <p>如果给定的测试框架不支持类后生命周期回调, 则不会为该框架调用此方法.
	 * 
	 * @param testContext 测试的测试上下文; never {@code null}
	 * 
	 * @throws Exception 允许任何异常传播
	 */
	void afterTestClass(TestContext testContext) throws Exception;

}
