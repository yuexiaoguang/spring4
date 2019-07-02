package org.springframework.test.context.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code TestExecutionListener},
 * 通过遵守Spring的{@link org.springframework.transaction.annotation.Transactional @Transactional}注解,
 * 为<em>测试管理的事务</em>中的测试提供支持.
 *
 * <h3>测试管理的事务</h3>
 * <p><em>测试管理的事务</em>是通过此监听器以声明方式管理的事务, 或通过{@link TestTransaction}以编程方式管理的事务.
 * 此类事务不应与<em>Spring管理的事务</em> (i.e., 在为测试加载的{@code ApplicationContext}中由Spring直接管理的事务)
 * 或<em>应用程序管理的事务</em> (i.e., 通过测试调用的应用程序代码中以编程方式管理的事务)混淆.
 * Spring管理和应用程序管理的事务通常会参与测试管理的事务;
 * 但是, 如果Spring管理的事务或应用程序管理的事务配置了除
 * {@link org.springframework.transaction.annotation.Propagation#REQUIRED REQUIRED}
 * 或{@link org.springframework.transaction.annotation.Propagation#SUPPORTS SUPPORTS}
 * 之外的传播类型, 则应该小心.
 *
 * <h3>启用和禁用事务</h3>
 * <p>使用{@code @Transactional}注解的测试方法会导致测试在一个事务中运行,
 * 默认情况下, 在完成测试后将自动<em>回滚</em>.
 * 如果测试类使用{@code @Transactional}注解, 则该类层次结构中的每个测试方法都将在事务中运行.
 * <em>未</em>使用{@code @Transactional} (在类或方法级别)注解的测试方法将不会在事务中运行.
 * 此外, 使用{@code @Transactional}注解, 但将
 * {@link org.springframework.transaction.annotation.Transactional#propagation propagation}
 * 类型设置为
 * {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
 * 的测试将不会在事务中运行.
 *
 * <h3>声明性回滚和提交行为</h3>
 * <p>默认情况下, 测试完成后, 测试事务将自动<em>回滚</em>;
 * 但是, 可以通过类级别和方法级别的{@link Commit @Commit} 和 {@link Rollback @Rollback}注解,
 * 以声明方式配置事务提交和回滚行为.
 *
 * <h3>程序化事务管理</h3>
 * <p>从Spring Framework 4.1开始, 可以通过{@link TestTransaction}中的静态方法以编程方式与测试管理的事务进行交互.
 * {@code TestTransaction}可以在<em>test</em>方法, <em>before</em>方法, 和<em>after</em>方法之中使用.
 *
 * <h3>在事务之外执行代码</h3>
 * <p>执行事务测试时, 有时可以在事务外执行某些<em>设置</em>或<em>销毁</em>代码.
 * {@code TransactionalTestExecutionListener}为使用{@link BeforeTransaction @BeforeTransaction}
 * 或{@link AfterTransaction @AfterTransaction}注解的方法提供此类支持.
 * 从Spring Framework 4.3开始, {@code @BeforeTransaction}和{@code @AfterTransaction}
 * 也可能在基于Java 8的接口默认方法上声明.
 *
 * <h3>配置事务管理器</h3>
 * <p>{@code TransactionalTestExecutionListener}期望在Spring {@code ApplicationContext}中
 * 定义{@link PlatformTransactionManager} bean以进行测试.
 * 如果测试的{@code ApplicationContext}中有多个{@code PlatformTransactionManager}实例,
 * 则可以通过
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * (e.g., {@code @Transactional("myTxMgr")} 或 {@code @Transactional(transactionManger = "myTxMgr")})
 * 声明<em>限定符</em>, 或者可以通过{@link org.springframework.context.annotation.Configuration @Configuration}类实现
 * {@link org.springframework.transaction.annotation.TransactionManagementConfigurer TransactionManagementConfigurer}.
 * 有关用于在测试{@code ApplicationContext}中查找事务管理器的算法的详细信息,
 * 请参阅{@link TestContextTransactionUtils#retrieveTransactionManager}.
 */
public class TransactionalTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(TransactionalTestExecutionListener.class);

	@SuppressWarnings("deprecation")
	private static final TransactionConfigurationAttributes defaultTxConfigAttributes = new TransactionConfigurationAttributes();

	// 不要求@Transactional 测试方法是 public.
	protected final TransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource(false);

	@SuppressWarnings("deprecation")
	private TransactionConfigurationAttributes configurationAttributes;


	/**
	 * Returns {@code 4000}.
	 */
	@Override
	public final int getOrder() {
		return 4000;
	}

	/**
	 * 如果提供的{@linkplain TestContext 测试上下文} 的测试方法配置为在事务中运行,
	 * 则此方法将运行{@link BeforeTransaction @BeforeTransaction}方法并启动新事务.
	 * <p>请注意, 如果{@code @BeforeTransaction}方法失败, 则不会调用任何剩余的{@code @BeforeTransaction}方法,
	 * 并且不会启动事务.
	 */
	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testMethod, "Test method of supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		Assert.state(txContext == null, "Cannot start new transaction without ending existing transaction");

		PlatformTransactionManager tm = null;
		TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(testMethod, testClass);

		if (transactionAttribute != null) {
			transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext,
				transactionAttribute);

			if (logger.isDebugEnabled()) {
				logger.debug("Explicit transaction definition [" + transactionAttribute +
						"] found for test context " + testContext);
			}

			if (transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
				return;
			}

			tm = getTransactionManager(testContext, transactionAttribute.getQualifier());

			if (tm == null) {
				throw new IllegalStateException(
						"Failed to retrieve PlatformTransactionManager for @Transactional test: " + testContext);
			}
		}

		if (tm != null) {
			txContext = new TransactionContext(testContext, tm, transactionAttribute, isRollback(testContext));
			runBeforeTransactionMethods(testContext);
			txContext.startTransaction();
			TransactionContextHolder.setCurrentTransactionContext(txContext);
		}
	}

	/**
	 * 如果提供的{@linkplain TestContext 测试上下文}的事务当前处于活动状态,
	 * 则此方法将结束事务并运行{{@link AfterTransaction @AfterTransaction}方法.
	 * <p>即使在结束事务时发生错误, 也可以保证调用{@code @AfterTransaction}方法.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		// 如果有 (或可能仍然是)事务...
		if (txContext != null) {
			TransactionStatus transactionStatus = txContext.getTransactionStatus();
			try {
				// 如果事务仍处于活动状态...
				if (transactionStatus != null && !transactionStatus.isCompleted()) {
					txContext.endTransaction();
				}
			}
			finally {
				runAfterTransactionMethods(testContext);
			}
		}
	}

	/**
	 * 为指定的{@linkplain TestContext 测试上下文}运行所有{@link BeforeTransaction @BeforeTransaction}方法.
	 * 但是, 如果其中一个方法失败, 捕获的异常将包装在{@link RuntimeException}中重新抛出,
	 * 其余的方法将<strong>没有</strong>机会执行.
	 * 
	 * @param testContext 当前的测试上下文
	 */
	protected void runBeforeTransactionMethods(TestContext testContext) throws Exception {
		try {
			List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), BeforeTransaction.class);
			Collections.reverse(methods);
			for (Method method : methods) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @BeforeTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
		}
		catch (InvocationTargetException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered while executing @BeforeTransaction methods for test context " +
						testContext + ".", ex.getTargetException());
			}
			ReflectionUtils.rethrowException(ex.getTargetException());
		}
	}

	/**
	 * 为指定的{@linkplain TestContext 测试上下文}运行所有的{@link AfterTransaction @AfterTransaction}方法.
	 * 如果其中一个方法失败, 捕获的异常将被记录为错误, 其余方法仍有机会执行.
	 * 执行完所有方法后, 将重新引发第一个捕获的异常.
	 * 
	 * @param testContext 当前的测试上下文
	 */
	protected void runAfterTransactionMethods(TestContext testContext) throws Exception {
		Throwable afterTransactionException = null;

		List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), AfterTransaction.class);
		for (Method method : methods) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @AfterTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
			catch (InvocationTargetException ex) {
				Throwable targetException = ex.getTargetException();
				if (afterTransactionException == null) {
					afterTransactionException = targetException;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, targetException);
			}
			catch (Exception ex) {
				if (afterTransactionException == null) {
					afterTransactionException = ex;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, ex);
			}
		}

		if (afterTransactionException != null) {
			ReflectionUtils.rethrowException(afterTransactionException);
		}
	}

	/**
	 * 获取{@linkplain PlatformTransactionManager 事务管理器}以用于提供的{@linkplain TestContext 测试上下文}和{@code qualifier}.
	 * <p>如果提供的{@code qualifier}为{@code null}或为空, 则委托给{@link #getTransactionManager(TestContext)}.
	 * 
	 * @param testContext 应检索事务管理器的测试上下文
	 * @param qualifier 在多个bean匹配之间进行选择的限定符; 可能是{@code null}或为空
	 * 
	 * @return 要使用的事务管理器, 或{@code null}
	 * @throws BeansException 如果在检索事务管理器时发生错误
	 */
	protected PlatformTransactionManager getTransactionManager(TestContext testContext, String qualifier) {
		// 从@Transactional按类型和限定符查找
		if (StringUtils.hasText(qualifier)) {
			try {
				// 使用支持autowire的工厂以支持扩展的限定符匹配
				// (仅暴露在内部BeanFactory上, 而不是在ApplicationContext上).
				BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

				return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
			}
			catch (RuntimeException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn(String.format(
							"Caught exception while retrieving transaction manager with qualifier '%s' for test context %s",
							qualifier, testContext), ex);
				}
				throw ex;
			}
		}

		// else
		return getTransactionManager(testContext);
	}

	/**
	 * 获取{@linkplain PlatformTransactionManager 事务管理器}以用于提供的{@linkplain TestContext 测试上下文}.
	 * <p>默认实现委托给{@link TestContextTransactionUtils#retrieveTransactionManager}.
	 * 
	 * @param testContext 应检索事务管理器的测试上下文
	 * 
	 * @return 要使用的事务管理器, 或{@code null}
	 * @throws BeansException 如果在检索显式命名的事务管理器时发生错误
	 * @throws IllegalStateException 如果ApplicationContext中存在多个TransactionManagementConfigurer
	 */
	protected PlatformTransactionManager getTransactionManager(TestContext testContext) {
		@SuppressWarnings("deprecation")
		String tmName = retrieveConfigurationAttributes(testContext).getTransactionManagerName();
		return TestContextTransactionUtils.retrieveTransactionManager(testContext, tmName);
	}

	/**
	 * 确定是否为提供的{@linkplain TestContext 测试上下文}默认回滚事务.
	 * <p>在类级别支持{@link Rollback @Rollback}, {@link Commit @Commit},
	 * {@link TransactionConfiguration @TransactionConfiguration}.
	 * 
	 * @param testContext 应检索默认回滚标志的测试上下文
	 * 
	 * @return 提供的测试上下文的<em>默认回滚</em>标志
	 * @throws Exception 如果在确定默认回滚标志时发生错误
	 */
	@SuppressWarnings("deprecation")
	protected final boolean isDefaultRollback(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Rollback rollback = AnnotatedElementUtils.findMergedAnnotation(testClass, Rollback.class);
		boolean rollbackPresent = (rollback != null);
		TransactionConfigurationAttributes txConfigAttributes = retrieveConfigurationAttributes(testContext);

		if (rollbackPresent && txConfigAttributes != defaultTxConfigAttributes) {
			throw new IllegalStateException(String.format("Test class [%s] is annotated with both @Rollback " +
					"and @TransactionConfiguration, but only one is permitted.", testClass.getName()));
		}

		if (rollbackPresent) {
			boolean defaultRollback = rollback.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Retrieved default @Rollback(%s) for test class [%s].",
						defaultRollback, testClass.getName()));
			}
			return defaultRollback;
		}

		// else
		return txConfigAttributes.isDefaultRollback();
	}

	/**
	 * 确定提供的{@linkplain TestContext 测试上下文}是否回滚事务,
	 * 通过考虑{@linkplain #isDefaultRollback(TestContext) 默认回滚}标志,
	 * 以及可能通过{@link Rollback @Rollback}注解覆盖方法级.
	 * 
	 * @param testContext 应检索回滚标志的测试上下文
	 * 
	 * @return 提供的测试上下文的<em>rollback</em>标志
	 * @throws Exception 如果在确定回滚标志时发生错误
	 */
	protected final boolean isRollback(TestContext testContext) throws Exception {
		boolean rollback = isDefaultRollback(testContext);
		Rollback rollbackAnnotation =
				AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), Rollback.class);
		if (rollbackAnnotation != null) {
			boolean rollbackOverride = rollbackAnnotation.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Method-level @Rollback(%s) overrides default rollback [%s] for test context %s.",
						rollbackOverride, rollback, testContext));
			}
			rollback = rollbackOverride;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"No method-level @Rollback override: using default rollback [%s] for test context %s.",
						rollback, testContext));
			}
		}
		return rollback;
	}

	/**
	 * 获取所提供的{@link Class class}及其超类中的所有方法, 这些方法使用提供的{@code annotationType}注解,
	 * 但不是通过在子类中重写方法以<em>shadowed</em>.
	 * <p>还检测接口上的默认方法.
	 * 
	 * @param clazz 要检索带注解的方法的类
	 * @param annotationType 要搜索的注解类型
	 * 
	 * @return 提供的类及其超类中的所有带注解的方法, 以及带注解的接口默认方法
	 */
	private List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
		List<Method> methods = new ArrayList<Method>(4);
		for (Method method : ReflectionUtils.getUniqueDeclaredMethods(clazz)) {
			if (AnnotationUtils.getAnnotation(method, annotationType) != null) {
				methods.add(method);
			}
		}
		return methods;
	}

	/**
	 * 检索所提供的{@link TestContext}的{@link TransactionConfigurationAttributes},
	 * 其{@linkplain Class 测试类}可以选择声明或继承{@link TransactionConfiguration @TransactionConfiguration}.
	 * <p>如果提供的{@code TestContext}没有{@code @TransactionConfiguration},
	 * 则将使用 {@code TransactionConfigurationAttributes}的默认实例.
	 * 
	 * @param testContext 应检索配置属性的测试上下文
	 * 
	 * @return 此监听器的TransactionConfigurationAttributes实例, 可能已缓存
	 */
	@SuppressWarnings("deprecation")
	TransactionConfigurationAttributes retrieveConfigurationAttributes(TestContext testContext) {
		if (this.configurationAttributes == null) {
			Class<?> clazz = testContext.getTestClass();

			TransactionConfiguration txConfig =
					AnnotatedElementUtils.findMergedAnnotation(clazz, TransactionConfiguration.class);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Retrieved @TransactionConfiguration [%s] for test class [%s].",
						txConfig, clazz.getName()));
			}

			TransactionConfigurationAttributes configAttributes = (txConfig == null ? defaultTxConfigAttributes :
					new TransactionConfigurationAttributes(txConfig.transactionManager(), txConfig.defaultRollback()));
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Using TransactionConfigurationAttributes %s for test class [%s].",
						configAttributes, clazz.getName()));
			}
			this.configurationAttributes = configAttributes;
		}
		return this.configurationAttributes;
	}

}
