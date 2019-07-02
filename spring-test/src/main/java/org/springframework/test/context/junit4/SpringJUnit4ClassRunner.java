package org.springframework.test.context.junit4;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringJUnit4ClassRunner}是JUnit的{@link BlockJUnit4ClassRunner}的自定义扩展,
 * 它通过{@link TestContextManager}以及相关的支持类和注解, 为标准JUnit测试提供<em>Spring TestContext Framework</em>的功能.
 *
 * <p>要使用此类，只需添加
 * {@code @RunWith(SpringJUnit4ClassRunner.class)}或{@code @RunWith(SpringRunner.class)}注解到基于JUnit 4的测试类.
 *
 * <p>以下列表构成了{@code SpringJUnit4ClassRunner}直接或间接支持的所有注解.
 * <em>(请注意, 各种
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
 * 或{@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}实现可能支持其他注解.)</em>
 *
 * <ul>
 * <li>{@link Test#expected() @Test(expected=...)}</li>
 * <li>{@link Test#timeout() @Test(timeout=...)}</li>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link Ignore @Ignore}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p>如果想将Spring TestContext Framework与其他运行器一起使用, 使用{@link SpringClassRule}和{@link SpringMethodRule}.
 *
 * <p><strong>NOTE:</strong> 从Spring Framework 4.3开始, 此类需要JUnit 4.12或更高版本.
 */
public class SpringJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(SpringJUnit4ClassRunner.class);

	private static final Method withRulesMethod;

	static {
		if (!ClassUtils.isPresent("org.junit.internal.Throwables", SpringJUnit4ClassRunner.class.getClassLoader())) {
			throw new IllegalStateException("SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");
		}

		withRulesMethod = ReflectionUtils.findMethod(SpringJUnit4ClassRunner.class, "withRules",
				FrameworkMethod.class, Object.class, Statement.class);
		if (withRulesMethod == null) {
			throw new IllegalStateException("SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");
		}
		ReflectionUtils.makeAccessible(withRulesMethod);
	}


	private final TestContextManager testContextManager;


	private static void ensureSpringRulesAreNotPresent(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			if (SpringClassRule.class.isAssignableFrom(field.getType())) {
				throw new IllegalStateException(String.format("Detected SpringClassRule field in test class [%s], " +
						"but SpringClassRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			}
			if (SpringMethodRule.class.isAssignableFrom(field.getType())) {
				throw new IllegalStateException(String.format("Detected SpringMethodRule field in test class [%s], " +
						"but SpringMethodRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			}
		}
	}

	/**
	 * 构造一个新的{@code SpringJUnit4ClassRunner}并初始化一个{@link TestContextManager},
	 * 为标准的JUnit测试提供Spring测试功能.
	 * 
	 * @param clazz 要运行的测试类
	 */
	public SpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		if (logger.isDebugEnabled()) {
			logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "]");
		}
		ensureSpringRulesAreNotPresent(clazz);
		this.testContextManager = createTestContextManager(clazz);
	}

	/**
	 * 为提供的测试类创建一个新的{@link TestContextManager}.
	 * <p>可以被子类覆盖.
	 * 
	 * @param clazz 要管理的测试类
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
	 * 获取与此运行器相关联的{@link TestContextManager}.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * 如果通过类级别的{@code @IfProfileValue}禁用测试, 则返回适用于忽略的测试类的描述, 否则委托给父级实现.
	 */
	@Override
	public Description getDescription() {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			return Description.createSuiteDescription(getTestClass().getJavaClass());
		}
		return super.getDescription();
	}

	/**
	 * 检查当前执行环境中是否启用了测试.
	 * <p>这可以防止具有不匹配的{@code @IfProfileValue}注解的类完全运行,
	 * 甚至可以跳过{@code TestExecutionListeners}中{@code prepareTestInstance()}方法的执行.
	 */
	@Override
	public void run(RunNotifier notifier) {
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			notifier.fireTestIgnored(getDescription());
			return;
		}
		super.run(notifier);
	}

	/**
	 * 使用{@code RunBeforeTestClassCallbacks}语句包装父级实现返回的{@link Statement},
	 * 从而保留默认的JUnit功能, 同时添加对Spring TestContext Framework的支持.
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
	}

	/**
	 * 使用{@code RunAfterTestClassCallbacks}语句包装父实现返回的{@link Statement},
	 * 从而保留默认的JUnit功能, 同时添加对Spring TestContext Framework的支持.
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement junitAfterClasses = super.withAfterClasses(statement);
		return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
	}

	/**
	 * 委托给父级实现创建测试实例, 然后允许{@link #getTestContextManager() TestContextManager}在返回之前准备测试实例.
	 */
	@Override
	protected Object createTest() throws Exception {
		Object testInstance = super.createTest();
		getTestContextManager().prepareTestInstance(testInstance);
		return testInstance;
	}

	/**
	 * 执行与
	 * {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)}相同的逻辑,
	 * 但{@link #isTestMethodIgnored(FrameworkMethod)}确定测试被<em>忽略</em>.
	 */
	@Override
	protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {
		Description description = describeChild(frameworkMethod);
		if (isTestMethodIgnored(frameworkMethod)) {
			notifier.fireTestIgnored(description);
		}
		else {
			Statement statement;
			try {
				statement = methodBlock(frameworkMethod);
			}
			catch (Throwable ex) {
				statement = new Fail(ex);
			}
			runLeaf(statement, description, notifier);
		}
	}

	/**
	 * 增加整个执行链的默认JUnit行为 {@linkplain #withPotentialRepeat 潜在的重复}.
	 * <p>此外, 对超时的支持已经沿着执行链向下移动,
	 * 以便在定时执行中包括{@link org.junit.Before @Before}和{@link org.junit.After @After}方法的执行.
	 * 请注意, 这与在主线程中执行{@code @Before}和{@code @After}方法的默认JUnit行为不同, 而在单独的线程中执行实际的测试方法.
	 * 因此, {@code @Before}和{@code @After}方法将在与测试方法相同的线程中执行.
	 * 因此, JUnit指定的超时将与Spring事务结合使用.
	 * 但是, JUnit特定的超时仍然不同于Spring特定的超时, 因为前者在单独的线程中执行, 而后者只是在主线程中执行 (如常规测试).
	 */
	@Override
	protected Statement methodBlock(FrameworkMethod frameworkMethod) {
		Object testInstance;
		try {
			testInstance = new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return createTest();
				}
			}.run();
		}
		catch (Throwable ex) {
			return new Fail(ex);
		}

		Statement statement = methodInvoker(frameworkMethod, testInstance);
		statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
		statement = withBefores(frameworkMethod, testInstance, statement);
		statement = withAfters(frameworkMethod, testInstance, statement);
		statement = withRulesReflectively(frameworkMethod, testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		return statement;
	}

	/**
	 * 使用反射调用JUnit的私有{@code withRules()}方法.
	 */
	private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return (Statement) ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance, statement);
	}

	/**
	 * 如果提供的{@linkplain FrameworkMethod 测试方法}存在{@link Ignore @Ignore},
	 * 或者通过{@code @IfProfileValue}禁用测试方法, 则返回{@code true}.
	 */
	protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
		Method method = frameworkMethod.getMethod();
		return (method.isAnnotationPresent(Ignore.class) ||
				!ProfileValueUtils.isTestEnabledInThisEnvironment(method, getTestClass().getJavaClass()));
	}

	/**
	 * 执行与
	 * {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)}相同的逻辑,
	 * 除了使用{@link #getExpectedException(FrameworkMethod)}检索<em>期望的异常</em>.
	 */
	@Override
	protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
		return (expectedException != null ? new ExpectException(next, expectedException) : next);
	}

	/**
	 * 获取所提供的{@linkplain FrameworkMethod 测试方法}应该抛出的{@code exception}.
	 * <p>支持JUnit的{@link Test#expected() @Test(expected=...)}注解.
	 * <p>可以被子类覆盖.
	 * 
	 * @return 预期的异常, 或{@code null}如果没有指定
	 */
	protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.expected() != Test.None.class ? test.expected() : null);
	}

	/**
	 * 执行与
	 * {@link BlockJUnit4ClassRunner#withPotentialTimeout(FrameworkMethod, Object, Statement)}相同的逻辑,
	 * 但对Spring的{@code @Timed}注解提供额外支持.
	 * <p>支持Spring的{@link org.springframework.test.annotation.Timed @Timed}
	 * 和JUnit的 {@link Test#timeout() @Test(timeout=...)}注解, 但不是两个同时使用.
	 * 
	 * @return 要么是{@link SpringFailOnTimeout}, {@link FailOnTimeout}, 要么是提供{@link Statement}
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Statement statement = null;
		long springTimeout = getSpringTimeout(frameworkMethod);
		long junitTimeout = getJUnitTimeout(frameworkMethod);
		if (springTimeout > 0 && junitTimeout > 0) {
			String msg = String.format("Test method [%s] has been configured with Spring's @Timed(millis=%s) and " +
							"JUnit's @Test(timeout=%s) annotations, but only one declaration of a 'timeout' is " +
							"permitted per test method.", frameworkMethod.getMethod(), springTimeout, junitTimeout);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
		else if (springTimeout > 0) {
			statement = new SpringFailOnTimeout(next, springTimeout);
		}
		else if (junitTimeout > 0) {
			statement = FailOnTimeout.builder().withTimeout(junitTimeout, TimeUnit.MILLISECONDS).build(next);
		}
		else {
			statement = next;
		}

		return statement;
	}

	/**
	 * 从提供的{@linkplain FrameworkMethod 测试方法}上的{@link Test @Test}注解中检索配置的JUnit {@code timeout}.
	 * 
	 * @return 超时时间, 或{@code 0}未指定
	 */
	protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.timeout() > 0 ? test.timeout() : 0);
	}

	/**
	 * 从提供的{@linkplain FrameworkMethod 测试方法}上的
	 * {@link org.springframework.test.annotation.Timed @Timed}注解中检索配置的特定于Spring的{@code timeout}.
	 * 
	 * @return 超时时间, 或{@code 0}未指定
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		return TestAnnotationUtils.getTimeout(frameworkMethod.getMethod());
	}

	/**
	 * 使用{@code RunBeforeTestMethodCallbacks}语句包装父实现返回的{@link Statement},
	 * 从而保留默认功能, 同时添加对Spring TestContext Framework的支持.
	 */
	@Override
	protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
		return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
	}

	/**
	 * 使用{@code RunAfterTestMethodCallbacks}语句包装父实现返回的{@link Statement},
	 * 从而保留默认功能, 同时添加对Spring TestContext Framework的支持.
	 */
	@Override
	protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
		return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(),
				getTestContextManager());
	}

	/**
	 * 使用{@code SpringRepeat}语句包装提供的{@link Statement}.
	 * <p>支持Spring的 {@link org.springframework.test.annotation.Repeat @Repeat}注解.
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		return new SpringRepeat(next, frameworkMethod.getMethod());
	}
}
