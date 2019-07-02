package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.ClassRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringMethodRule}是一个自定义的JUnit {@link MethodRule},
 * 它通过{@link TestContextManager}以及相关的支持类和注解,
 * 支持标准JUnit测试中<em>Spring TestContext Framework</em>的实例级和方法级功能.
 *
 * <p>与{@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner SpringJUnit4ClassRunner}相比,
 * Spring基于规则的JUnit支持的优势在于它独立于任何{@link org.junit.runner.Runner Runner},
 * 因此可以与现有的替代运行器(如JUnit的{@code Parameterized})或第三方运行器(例如{@code MockitoJUnitRunner})结合使用.
 *
 * <p>但是, 为了实现与{@code SpringJUnit4ClassRunner}相同的功能, {@code SpringMethodRule}必须与{@link SpringClassRule}结合使用,
 * 因为{@code SpringMethodRule}只支持{@code SpringJUnit4ClassRunner}的实例级和方法级功能.
 *
 * <h3>用法实例</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule();
 *
 *    // ...
 * }</code></pre>
 *
 * <p>以下列表构成了{@code SpringMethodRule}目前直接或间接支持的所有注解.
 * <em>(请注意, 各种
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}或
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * 实现可能支持其他注解.)</em>
 *
 * <ul>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> 从Spring Framework 4.3开始, 此类需要JUnit 4.12或更高版本.
 */
public class SpringMethodRule implements MethodRule {

	private static final Log logger = LogFactory.getLog(SpringMethodRule.class);

	static {
		if (!ClassUtils.isPresent("org.junit.internal.Throwables", SpringMethodRule.class.getClassLoader())) {
			throw new IllegalStateException("SpringMethodRule requires JUnit 4.12 or higher.");
		}
	}


	/**
	 * 将<em>Spring TestContext Framework</em>的<em>实例级</em>和<em>方法级</em>功能应用于提供的{@code base}语句.
	 * <p>具体来说, 此方法调用{@code TestContextManager}上的
	 * {@link TestContextManager#prepareTestInstance prepareTestInstance()},
	 * {@link TestContextManager#beforeTestMethod beforeTestMethod()},
	 * 和{@link TestContextManager#afterTestMethod afterTestMethod()}方法, 可能使用Spring超时和重复.
	 * <p>此外, 此方法还会检查当前执行环境中是否启用了测试.
	 * 这可以防止带有不匹配的{@code @IfProfileValue}注解的方法完全运行,
	 * 甚至可以跳过{@code TestExecutionListeners}中{@code prepareTestInstance()}方法的执行.
	 * 
	 * @param base 应该应用此规则的基础{@code Statement}
	 * @param frameworkMethod 即将在测试实例上调用的方法
	 * @param testInstance 当前的测试实例
	 * 
	 * @return 一个语句, 它使用Spring TestContext Framework的实例级和方法级功能包装所提供的{@code base}
	 */
	@Override
	public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object testInstance) {
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringMethodRule to test method [" + frameworkMethod.getMethod() + "]");
		}
		Class<?> testClass = testInstance.getClass();
		validateSpringClassRuleConfiguration(testClass);
		TestContextManager testContextManager = SpringClassRule.getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestMethodCallbacks(statement, frameworkMethod, testInstance, testContextManager);
		statement = withAfterTestMethodCallbacks(statement, frameworkMethod, testInstance, testContextManager);
		statement = withTestInstancePreparation(statement, testInstance, testContextManager);
		statement = withPotentialRepeat(statement, frameworkMethod, testInstance);
		statement = withPotentialTimeout(statement, frameworkMethod, testInstance);
		statement = withProfileValueCheck(statement, frameworkMethod, testInstance);
		return statement;
	}

	/**
	 * 使用{@code RunBeforeTestMethodCallbacks}语句包装提供的{@link Statement}.
	 */
	private Statement withBeforeTestMethodCallbacks(Statement statement, FrameworkMethod frameworkMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunBeforeTestMethodCallbacks(
				statement, testInstance, frameworkMethod.getMethod(), testContextManager);
	}

	/**
	 * 使用{@code RunAfterTestMethodCallbacks}语句包装提供的{@link Statement}.
	 */
	private Statement withAfterTestMethodCallbacks(Statement statement, FrameworkMethod frameworkMethod,
			Object testInstance, TestContextManager testContextManager) {

		return new RunAfterTestMethodCallbacks(
				statement, testInstance, frameworkMethod.getMethod(), testContextManager);
	}

	/**
	 * 使用{@code RunPrepareTestInstanceCallbacks}语句包装提供的{@link Statement}.
	 */
	private Statement withTestInstancePreparation(Statement statement, Object testInstance,
			TestContextManager testContextManager) {

		return new RunPrepareTestInstanceCallbacks(statement, testInstance, testContextManager);
	}

	/**
	 * 使用{@code SpringRepeat}语句包装提供的{@link Statement}.
	 * <p>支持Spring的{@link org.springframework.test.annotation.Repeat @Repeat}注解.
	 */
	private Statement withPotentialRepeat(Statement next, FrameworkMethod frameworkMethod, Object testInstance) {
		return new SpringRepeat(next, frameworkMethod.getMethod());
	}

	/**
	 * 使用{@code SpringFailOnTimeout}语句包装提供的{@link Statement}.
	 * <p>支持Spring的{@link org.springframework.test.annotation.Timed @Timed}注解.
	 */
	private Statement withPotentialTimeout(Statement next, FrameworkMethod frameworkMethod, Object testInstance) {
		return new SpringFailOnTimeout(next, frameworkMethod.getMethod());
	}

	/**
	 * 使用{@code ProfileValueChecker}语句包装提供的{@link Statement}.
	 */
	private Statement withProfileValueCheck(Statement statement, FrameworkMethod frameworkMethod, Object testInstance) {
		return new ProfileValueChecker(statement, testInstance.getClass(), frameworkMethod.getMethod());
	}


	/**
	 * 如果提供的{@code testClass}没有声明使用{@code @ClassRule}注解的
	 * {@code public static final SpringClassRule}字段, 则抛出{@link IllegalStateException}.
	 */
	private static SpringClassRule validateSpringClassRuleConfiguration(Class<?> testClass) {
		Field ruleField = null;

		for (Field field : testClass.getFields()) {
			if (ReflectionUtils.isPublicStaticFinal(field) && SpringClassRule.class.isAssignableFrom(field.getType())) {
				ruleField = field;
				break;
			}
		}

		if (ruleField == null) {
			throw new IllegalStateException(String.format(
					"Failed to find 'public static final SpringClassRule' field in test class [%s]. " +
					"Consult the javadoc for SpringClassRule for details.", testClass.getName()));
		}

		if (!ruleField.isAnnotationPresent(ClassRule.class)) {
			throw new IllegalStateException(String.format(
					"SpringClassRule field [%s] must be annotated with JUnit's @ClassRule annotation. " +
					"Consult the javadoc for SpringClassRule for details.", ruleField));
		}

		return (SpringClassRule) ReflectionUtils.getField(ruleField, null);
	}
}
