package org.springframework.test.context.testng;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * 抽象基础测试类, 它将<em>Spring TestContext Framework</em>
 * 与<strong>TestNG</strong>环境中的显式{@link ApplicationContext}测试支持集成在一起.
 *
 * <p>具体的子类:
 * <ul>
 * <li>通常声明一个类级{@link ContextConfiguration &#064;ContextConfiguration}注解,
 * 来配置{@link ApplicationContext 应用程序上下文} {@link ContextConfiguration#locations() 资源位置}
 * 或{@link ContextConfiguration#classes() 带注解的类}.
 * <em>如果测试不需要加载应用程序上下文, 可以选择省略{@link ContextConfiguration &#064;ContextConfiguration}声明,
 * 手动配置相应的 {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}.</em></li>
 * <li>必须具有隐式或显式委托给{@code super();}的构造函数.</li>
 * </ul>
 *
 * <p>默认情况下配置以下{@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * </ul>
 */
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
	DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
public abstract class AbstractTestNGSpringContextTests implements IHookable, ApplicationContextAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 通过{@link #setApplicationContext(ApplicationContext)}注入到此测试实例的{@link ApplicationContext}.
	 */
	protected ApplicationContext applicationContext;

	private final TestContextManager testContextManager;

	private Throwable testException;


	/**
	 * 为当前测试初始化​​内部{@link TestContextManager}.
	 */
	public AbstractTestNGSpringContextTests() {
		this.testContextManager = new TestContextManager(getClass());
	}

	/**
	 * 设置此测试实例使用的{@link ApplicationContext}, 通过{@link ApplicationContextAware}语义提供.
	 *
	 * @param applicationContext 此测试运行的ApplicationContext
	 */
	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 委托给配置的{@link TestContextManager}调用{@link TestContextManager#beforeTestClass() '在测试类之前'}回调.
	 *
	 * @throws Exception 如果注册的TestExecutionListener抛出异常
	 */
	@BeforeClass(alwaysRun = true)
	protected void springTestContextBeforeTestClass() throws Exception {
		this.testContextManager.beforeTestClass();
	}

	/**
	 * 在执行任何单个测试之前, 委托给配置的{@link TestContextManager}
	 * {@link TestContextManager#prepareTestInstance(Object) 准备}此测试实例, 例如注入依赖项等.
	 *
	 * @throws Exception 如果注册的TestExecutionListener抛出异常
	 */
	@BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
	protected void springTestContextPrepareTestInstance() throws Exception {
		this.testContextManager.prepareTestInstance(this);
	}

	/**
	 * 在执行实际测试之前, 委托给配置的{@link TestContextManager}
	 * {@link TestContextManager#beforeTestMethod(Object,Method) 预处理}测试方法.
	 *
	 * @param testMethod 即将执行的测试方法.
	 * 
	 * @throws Exception 允许所有异常传播.
	 */
	@BeforeMethod(alwaysRun = true)
	protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
		this.testContextManager.beforeTestMethod(this, testMethod);
	}

	/**
	 * 委托给提供的{@code callback}中的{@link IHookCallBack#runTestMethod(ITestResult) 测试方法}来执行实际测试,
	 * 然后跟踪测试执行期间抛出的异常.
	 */
	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		callBack.runTestMethod(testResult);

		Throwable testResultException = testResult.getThrowable();
		if (testResultException instanceof InvocationTargetException) {
			testResultException = ((InvocationTargetException) testResultException).getCause();
		}
		this.testException = testResultException;
	}

	/**
	 * 在实际测试执行后, 委托给配置的{@link TestContextManager}
	 * {@link TestContextManager#afterTestMethod(Object, Method, Throwable) 后处理}测试方法.
	 *
	 * @param testMethod 刚刚在测试实例上执行的测试方法
	 * 
	 * @throws Exception 允许所有异常传播
	 */
	@AfterMethod(alwaysRun = true)
	protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
		try {
			this.testContextManager.afterTestMethod(this, testMethod, this.testException);
		}
		finally {
			this.testException = null;
		}
	}

	/**
	 * 委托给配置的{@link TestContextManager}调用{@link TestContextManager#afterTestClass() '在测试类之后'}回调.
	 *
	 * @throws Exception 如果已注册的TestExecutionListener抛出异常
	 */
	@AfterClass(alwaysRun = true)
	protected void springTestContextAfterTestClass() throws Exception {
		this.testContextManager.afterTestClass();
	}
}
