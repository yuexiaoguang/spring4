package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@code SpringClassRule}是一个自定义的JUnit {@link TestRule},
 * 通过{@link TestContextManager}以及相关的支持类和注解,
 * 在标准JUnit测试中支持<em>Spring TestContext Framework</em>的<em>类级</em>功能.
 *
 * <p>contrast{@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner SpringJUnit4ClassRunner}相比,
 * Spring基于规则的JUnit支持的优势在于它独立于任何{@link org.junit.runner.Runner Runner},
 * 因此可以与现有的替代运行器(如JUnit的{@code Parameterized}或第三方运行器{@code MockitoJUnitRunner})结合使用.
 *
 * <p>但是, 为了实现与{@code SpringJUnit4ClassRunner}相同的功能,
 * {@code SpringClassRule}必须与{@link SpringMethodRule}结合使用,
 * 因为{@code SpringClassRule}仅支持{@code SpringClassRule}的类级功能.
 *
 * <h3>示例用法</h3>
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
 * <p>以下列表构成了{@code SpringClassRule}直接或间接支持的所有注解.
 * <em>(请注意, 各种
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}或
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * 实现可能支持其他注解.)</em>
 *
 * <ul>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> 从Spring Framework 4.3开始, 此类需要JUnit 4.12或更高版本.
 */
public class SpringClassRule implements TestRule {

	private static final Log logger = LogFactory.getLog(SpringClassRule.class);

	/**
	 * 由测试类作为键的{@code TestContextManagers}的缓存.
	 */
	private static final Map<Class<?>, TestContextManager> testContextManagerCache =
			new ConcurrentHashMap<Class<?>, TestContextManager>(64);

	static {
		if (!ClassUtils.isPresent("org.junit.internal.Throwables", SpringClassRule.class.getClassLoader())) {
			throw new IllegalStateException("SpringClassRule requires JUnit 4.12 or higher.");
		}
	}


	/**
	 * 将<em>Spring TestContext Framework</em>的<em>类级</em>功能应用于提供的{@code base}语句.
	 * <p>具体来说, 此方法检索此规则及其关联的{@link SpringMethodRule}使用的{@link TestContextManager},
	 * 并在{@code TestContextManager}上调用{@link TestContextManager#beforeTestClass() beforeTestClass()}
	 * 和{@link TestContextManager#afterTestClass() afterTestClass()}方法.
	 * <p>此外, 此方法还会检查当前执行环境中是否启用了测试.
	 * 这可以防止具有不匹配的{@code @IfProfileValue}注解的类完全运行,
	 * 甚至可以跳过{@code TestExecutionListeners}中{@code beforeTestClass()}方法的执行.
	 * 
	 * @param base 应该应用此规则的基础{@code Statement}
	 * @param description 当前测试执行的{@code Description}
	 * 
	 * @return 一个语句, 它将提供的{@code base}包装在Spring TestContext Framework的类级功能中
	 */
	@Override
	public Statement apply(Statement base, Description description) {
		Class<?> testClass = description.getTestClass();
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringClassRule to test class [" + testClass.getName() + "]");
		}
		validateSpringMethodRuleConfiguration(testClass);
		TestContextManager testContextManager = getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestClassCallbacks(statement, testContextManager);
		statement = withAfterTestClassCallbacks(statement, testContextManager);
		statement = withProfileValueCheck(statement, testClass);
		statement = withTestContextManagerCacheEviction(statement, testClass);
		return statement;
	}

	/**
	 * 使用{@code RunBeforeTestClassCallbacks}语句包装提供的{@code statement}.
	 */
	private Statement withBeforeTestClassCallbacks(Statement statement, TestContextManager testContextManager) {
		return new RunBeforeTestClassCallbacks(statement, testContextManager);
	}

	/**
	 * 使用{@code RunAfterTestClassCallbacks}语句包装提供的{@code statement}.
	 */
	private Statement withAfterTestClassCallbacks(Statement statement, TestContextManager testContextManager) {
		return new RunAfterTestClassCallbacks(statement, testContextManager);
	}

	/**
	 * 使用{@code ProfileValueChecker}语句包装提供的{@code statement}.
	 */
	private Statement withProfileValueCheck(Statement statement, Class<?> testClass) {
		return new ProfileValueChecker(statement, testClass, null);
	}

	/**
	 * 使用{@code TestContextManagerCacheEvictor}语句包装提供的{@code statement}.
	 */
	private Statement withTestContextManagerCacheEviction(Statement statement, Class<?> testClass) {
		return new TestContextManagerCacheEvictor(statement, testClass);
	}


	/**
	 * 如果提供的{@code testClass}没有声明使用{@code @Rule}注解的{@code public SpringMethodRule}字段, 则抛出{@link IllegalStateException}.
	 */
	private static void validateSpringMethodRuleConfiguration(Class<?> testClass) {
		Field ruleField = null;

		for (Field field : testClass.getFields()) {
			int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) &&
					SpringMethodRule.class.isAssignableFrom(field.getType())) {
				ruleField = field;
				break;
			}
		}

		if (ruleField == null) {
			throw new IllegalStateException(String.format(
					"Failed to find 'public SpringMethodRule' field in test class [%s]. " +
					"Consult the javadoc for SpringClassRule for details.", testClass.getName()));
		}

		if (!ruleField.isAnnotationPresent(Rule.class)) {
			throw new IllegalStateException(String.format(
					"SpringMethodRule field [%s] must be annotated with JUnit's @Rule annotation. " +
					"Consult the javadoc for SpringClassRule for details.", ruleField));
		}
	}

	/**
	 * 获取与提供的测试类关联的{@link TestContextManager}.
	 * 
	 * @param testClass 要管理的测试类; never {@code null}
	 */
	static TestContextManager getTestContextManager(Class<?> testClass) {
		Assert.notNull(testClass, "testClass must not be null");
		synchronized (testContextManagerCache) {
			TestContextManager testContextManager = testContextManagerCache.get(testClass);
			if (testContextManager == null) {
				testContextManager = new TestContextManager(testClass);
				testContextManagerCache.put(testClass, testContextManager);
			}
			return testContextManager;
		}
	}


	private static class TestContextManagerCacheEvictor extends Statement {

		private final Statement next;

		private final Class<?> testClass;


		TestContextManagerCacheEvictor(Statement next, Class<?> testClass) {
			this.next = next;
			this.testClass = testClass;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				next.evaluate();
			}
			finally {
				testContextManagerCache.remove(testClass);
			}
		}
	}
}
