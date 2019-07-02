package org.springframework.test.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestContextManager}是进入<em>Spring TestContext Framework</em>的主要入口点.
 *
 * <p>具体来说, {@code TestContextManager}负责管理单个{@link TestContext},
 * 并在以下测试执行点向所有已注册的{@link TestExecutionListener TestExecutionListeners}发送信号事件:
 *
 * <ul>
 * <li>{@link #beforeTestClass() 在测试类执行之前}:
 * 在特定测试框架的任何<em>类回调之前</em> (e.g., JUnit 4的 {@link org.junit.BeforeClass @BeforeClass})</li>
 * <li>{@link #prepareTestInstance(Object) 测试实例准备}: 在实例化测试实例后立即执行</li>
 * <li>{@link #beforeTestMethod(Object, Method) 在测试方法执行之前}:
 * 在特定测试框架的任何<em>方法回调之前</em> (e.g., JUnit 4的{@link org.junit.Before @Before})</li>
 * <li>{@link #afterTestMethod(Object, Method, Throwable) 测试方法执行后}:
 * 在特定测试框架的任何<em>方法回调之后</em> (e.g., JUnit 4的{@link org.junit.After @After})</li>
 * <li>{@link #afterTestClass() 在测试类执行之后}:
 * 在特定测试框架的任何<em>类回调之后</em> (e.g., JUnit 4的{@link org.junit.AfterClass @AfterClass})</li>
 * </ul>
 *
 * <p>支持加载和访问
 * {@link org.springframework.context.ApplicationContext 应用程序上下文},
 * 测试实例的依赖注入, 测试方法的
 * {@link org.springframework.transaction.annotation.Transactional 事务}执行, 等.
 * 由{@link SmartContextLoader ContextLoaders} 和 {@link TestExecutionListener TestExecutionListeners}提供,
 * 它们通过{@link ContextConfiguration @ContextConfiguration} 和 {@link TestExecutionListeners @TestExecutionListeners}配置.
 *
 * <p>{@code TestContext}, 默认{@code ContextLoader}, 默认{@code TestExecutionListeners}, 及其协作者的引导,
 * 由{@link TestContextBootstrapper}执行, 其由通过{@link BootstrapWith @BootstrapWith}配置.
 */
public class TestContextManager {

	private static final Log logger = LogFactory.getLog(TestContextManager.class);

	private final TestContext testContext;

	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<TestExecutionListener>();


	/**
	 * <p>委托给{@link #TestContextManager(TestContextBootstrapper)}, 为测试类配置{@link TestContextBootstrapper}.
	 * 如果测试类上存在{@link BootstrapWith @BootstrapWith}注解, 可以直接或作为元注解,
	 * 那么它的{@link BootstrapWith#value value}将用作引导程序类型;
	 * 否则, 将使用{@link org.springframework.test.context.support.DefaultTestContextBootstrapper DefaultTestContextBootstrapper}.
	 * 
	 * @param testClass 要管理的测试类
	 */
	public TestContextManager(Class<?> testClass) {
		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
	}

	/**
	 * <p>委托给提供的{@code TestContextBootstrapper}, 用于构建{@code TestContext}并检索{@code TestExecutionListeners}.
	 * 
	 * @param testContextBootstrapper 要使用的bootstrapper
	 */
	public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
		this.testContext = testContextBootstrapper.buildTestContext();
		registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
	}

	/**
	 * 获取由此{@code TestContextManager}管理的{@link TestContext}.
	 */
	public final TestContext getTestContext() {
		return this.testContext;
	}

	/**
	 * 注册所提供的{@link TestExecutionListener TestExecutionListeners}列表,
	 * 通过将它们附加到此{@code TestContextManager}使用的监听器列表中.
	 */
	public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
		registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[testExecutionListeners.size()]));
	}

	/**
	 * 注册所提供的{@link TestExecutionListener TestExecutionListeners}数组,
	 * 方法是将它们附加到此{@code TestContextManager}使用的监听器列表中.
	 */
	public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
		for (TestExecutionListener listener : testExecutionListeners) {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering TestExecutionListener: " + listener);
			}
			this.testExecutionListeners.add(listener);
		}
	}

	/**
	 * 获取为此{@code TestContextManager}注册的当前{@link TestExecutionListener TestExecutionListeners}.
	 * <p>允许修改, e.g. 将监听器添加到列表的开头.
	 * 但是, 确保在实际执行测试时保持列表稳定.
	 */
	public final List<TestExecutionListener> getTestExecutionListeners() {
		return this.testExecutionListeners;
	}

	/**
	 * 以相反的顺序获取为此{@code TestContextManager}注册的{@link TestExecutionListener TestExecutionListeners}的副本.
	 */
	private List<TestExecutionListener> getReversedTestExecutionListeners() {
		List<TestExecutionListener> listenersReversed = new ArrayList<TestExecutionListener>(getTestExecutionListeners());
		Collections.reverse(listenersReversed);
		return listenersReversed;
	}

	/**
	 * 在执行类中的任何测试之前, 用于预处理测试类.
	 * 应在任何特定于框架的类方法之前调用 (e.g., 使用JUnit的{@link org.junit.BeforeClass @BeforeClass}注解的方法).
	 * <p>将尝试为每个已注册的{@link TestExecutionListener}提供预处理测试类执行的机会.
	 * 但是, 如果监听器抛出异常, 则剩余的已注册监听器将<strong>不</strong>被调用.
	 * 
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	public void beforeTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestClass(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'before class' callback for test class [" + testClass + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * 用于在执行任何单独的测试方法之前准备测试实例的钩子, 例如用于注入依赖等.
	 * 应该在实例化测试实例后立即调用.
	 * <p>托管的{@link TestContext}将使用提供的{@code testInstance}进行更新.
	 * <p>将尝试为每个已注册的{@link TestExecutionListener}提供准备测试实例的机会.
	 * 但是, 如果监听器抛出异常, 则剩余的已注册监听器将<strong>不</strong>被调用.
	 * 
	 * @param testInstance 要准备的测试实例 (never {@code null})
	 * 
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	public void prepareTestInstance(Object testInstance) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
		}
		getTestContext().updateState(testInstance, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.prepareTestInstance(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to prepare test instance [" + testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * 在执行提供的{@link Method 测试方法}之前预先处理测试的钩子, 例如用于设置测试环境, 启动事务等.
	 * 应该在任何特定于框架的方法之前调用(e.g., 使用JUnit的 {@link org.junit.Before @Before}注解的方法).
	 * <p>托管的{@link TestContext}将使用提供的{@code testInstance}和{@code testMethod}进行更新.
	 * <p>将尝试为每个已注册的{@link TestExecutionListener}提供预处理测试方法执行的机会.
	 * 但是, 如果监听器抛出异常, 则剩余的已注册监听器将<strong>不</strong>被调用.
	 * 
	 * @param testInstance 当前的测试实例 (never {@code null})
	 * @param testMethod 即将在测试实例上执行的测试方法
	 * 
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestMethod(): instance [" + testInstance + "], method [" + testMethod + "]");
		}
		getTestContext().updateState(testInstance, testMethod, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'before' execution of test method [" + testMethod + "] for test instance [" +
							testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * 在执行提供的{@link Method 测试方法}之后进行后处理的钩子, 例如用于销毁测试环境, 结束事务等.
	 * 应在任何特定于框架的方法之后调用 (e.g., 使用JUnit的{@link org.junit.After @After}注解的方法).
	 * <p>托管的{@link TestContext}将使用提供的{@code testInstance}, {@code testMethod}, 和 {@code exception}进行更新.
	 * <p>每个注册的{@link TestExecutionListener}都有机会对测试方法执行进行后处理.
	 * 如果监听器抛出异常, 则仍将调用剩余的已注册监听器, 但在所有监听器执行后将跟踪并重新抛出所引发的第一个异常.
	 * 请注意, 已注册的监听器将按其注册的相反顺序执行.
	 * 
	 * @param testInstance 当前测试实例 (never {@code null})
	 * @param testMethod 刚刚在测试实例上执行的测试方法
	 * @param exception 执行测试方法期间抛出的异常或TestExecutionListener抛出的异常, 或{@code null} 如果没有抛出
	 * 
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	public void afterTestMethod(Object testInstance, Method testMethod, Throwable exception) throws Exception {
		Assert.notNull(testInstance, "Test instance must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestMethod(): instance [" + testInstance + "], method [" + testMethod +
					"], exception [" + exception + "]");
		}
		getTestContext().updateState(testInstance, testMethod, exception);

		Throwable afterTestMethodException = null;
		// 以相反的顺序遍历TestExecutionListeners以确保正确的"wrapper"式监听器执行.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'after' execution for test: method [" + testMethod + "], instance [" +
							testInstance + "], exception [" + exception + "]", ex);
				}
				if (afterTestMethodException == null) {
					afterTestMethodException = ex;
				}
			}
		}
		if (afterTestMethodException != null) {
			ReflectionUtils.rethrowException(afterTestMethodException);
		}
	}

	/**
	 * 在执行类中的所有测试之后, 用于对测试类进行后处理.
	 * 应该在任何特定于框架的类方法之后调用 (e.g., 使用JUnit的{@link org.junit.AfterClass @AfterClass}注解的方法).
	 * <p>每个注册的{@link TestExecutionListener}都有机会对测试类进行后处理.
	 * 如果监听器抛出异常, 则仍将调用剩余的已注册监听器, 但在所有监听器执行后将跟踪并重新抛出所引发的第一个异常.
	 * 请注意, 已注册的侦听器将按其注册的相反顺序执行.
	 * 
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	public void afterTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		Throwable afterTestClassException = null;
		// 以相反的顺序遍历TestExecutionListeners以确保正确的"wrapper"式监听器执行.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestClass(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to process 'after class' callback for test class [" + testClass + "]", ex);
				}
				if (afterTestClassException == null) {
					afterTestClassException = ex;
				}
			}
		}
		if (afterTestClassException != null) {
			ReflectionUtils.rethrowException(afterTestClassException);
		}
	}
}
