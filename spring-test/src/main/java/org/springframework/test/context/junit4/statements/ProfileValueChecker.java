package org.springframework.test.context.junit4.statements;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.util.Assert;

/**
 * {@code ProfileValueChecker}是一个自定义的{@link Statement},
 * 它通过Spring的{@link IfProfileValue @IfProfileValue}注解检查当前环境中是否启用了测试类或测试方法.
 */
public class ProfileValueChecker extends Statement {

	private final Statement next;

	private final Class<?> testClass;

	private final Method testMethod;


	/**
	 * @param next 执行链中的下一个{@code Statement}; never {@code null}
	 * @param testClass 要检查的测试类; never {@code null}
	 * @param testMethod 要检查的测试方法; 可能是{@code null}, 如果在类级别应用此{@code ProfileValueChecker}
	 */
	public ProfileValueChecker(Statement next, Class<?> testClass, Method testMethod) {
		Assert.notNull(next, "The next statement must not be null");
		Assert.notNull(testClass, "The test class must not be null");
		this.next = next;
		this.testClass = testClass;
		this.testMethod = testMethod;
	}


	/**
	 * 通过{@link IfProfileValue @IfProfileValue}注解配置,
	 * 确定当前环境中{@linkplain #ProfileValueChecker 构造函数}的参数指定的测试是否<em>已启用</em>.
	 * <p>如果测试未使用{@code @IfProfileValue}进行注解, 则会将其视为已启用.
	 * <p>如果未启用测试, 则此方法将以失败的假设中止执行链的进一步评估;
	 * 否则, 此方法将简单地评估执行链中的下一个{@link Statement}.
	 * 
	 * @throws AssumptionViolatedException 如果测试被禁用
	 * @throws Throwable 如果对下一个语句的评估失败
	 */
	@Override
	public void evaluate() throws Throwable {
		if (this.testMethod == null) {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testClass)) {
				Annotation ann = AnnotatedElementUtils.findMergedAnnotation(this.testClass, IfProfileValue.class);
				throw new AssumptionViolatedException(String.format(
						"Profile configured via [%s] is not enabled in this environment for test class [%s].",
						ann, this.testClass.getName()));
			}
		}
		else {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testMethod, this.testClass)) {
				throw new AssumptionViolatedException(String.format(
						"Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
						this.testMethod));
			}
		}

		this.next.evaluate();
	}

}
