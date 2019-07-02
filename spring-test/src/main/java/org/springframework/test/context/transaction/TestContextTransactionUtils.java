package org.springframework.test.context.transaction;

import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.DelegatingTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于处理<em>Spring TestContext Framework</em>中的事务和数据访问相关bean的实用方法.
 *
 * <p>主要供框架内部使用.
 */
public abstract class TestContextTransactionUtils {

	/**
	 * {@link DataSource}的默认bean名称: {@code "dataSource"}.
	 */
	public static final String DEFAULT_DATA_SOURCE_NAME = "dataSource";

	/**
	 * {@link PlatformTransactionManager}的默认bean名称: {@code "transactionManager"}.
	 */
	public static final String DEFAULT_TRANSACTION_MANAGER_NAME = "transactionManager";


	private static final Log logger = LogFactory.getLog(TestContextTransactionUtils.class);


	/**
	 * 检索{@link DataSource}以用于提供的{@linkplain TestContext 测试上下文}.
	 * <p>以下算法用于从提供的测试上下文的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}中检索{@code DataSource}:
	 * <ol>
	 * <li>按类型和名称查找{@code DataSource}, 如果提供的{@code name}非空,
	 * 但命名的{@code DataSource}不存在则抛出{@link BeansException}.
	 * <li>尝试按类型查找单个{@code DataSource}.
	 * <li>尝试按类型查找<em>主</em> {@code DataSource}.
	 * <li>尝试按类型和{@linkplain #DEFAULT_DATA_SOURCE_NAME 默认数据源名称}查找{@code DataSource}.
	 * 
	 * @param testContext 应检索{@code DataSource}的测试上下文; never {@code null}
	 * @param name 要检索的{@code DataSource}的名称 (可能是{@code null}或<em>为空</em>)
	 * 
	 * @return 要使用的{@code DataSource}, 或{@code null}
	 * @throws BeansException 如果在检索显式命名的{@code DataSource}时发生错误
	 */
	public static DataSource retrieveDataSource(TestContext testContext, String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// 按类型和显式名称查找
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, DataSource.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve DataSource named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory) {
				ListableBeanFactory lbf = (ListableBeanFactory) bf;

				// 按类型查找单个bean
				Map<String, DataSource> dataSources =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, DataSource.class);
				if (dataSources.size() == 1) {
					return dataSources.values().iterator().next();
				}

				try {
					// 按类型查找单个bean, 支持'primary' beans
					return bf.getBean(DataSource.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// 按类型和默认名称查找
			return bf.getBean(DEFAULT_DATA_SOURCE_NAME, DataSource.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, DataSource.class);
			return null;
		}
	}

	/**
	 * 检索{@linkplain PlatformTransactionManager 事务管理器}以用于提供的{@linkplain TestContext 测试上下文}.
	 * <p>以下算法用于从提供的测试上下文的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}检索事务管理器:
	 * <ol>
	 * <li>如果提供的{@code name}非空, 则按类型和显式名称查找事务管理器,
	 * 如果指定的事务管理器不存在, 则抛出{@link BeansException}.
	 * <li>尝试按类型查找单个事务管理器.
	 * <li>尝试按类型查找<em>主</em>事务管理器.
	 * <li>尝试通过{@link TransactionManagementConfigurer}查找事务管理器.
	 * <li>尝试按类型和{@linkplain #DEFAULT_TRANSACTION_MANAGER_NAME 默认事务管理器名称}查找事务管理器.
	 * 
	 * @param testContext 应检索事务管理器的测试上下文; never {@code null}
	 * @param name 要检索的事务管理器的名称 (可能是{@code null}或<em>为空</em>)
	 * 
	 * @return 要使用的事务管理器, 或{@code null}
	 * @throws BeansException 如果在检索显式命名的事务管理器时发生错误
	 * @throws IllegalStateException 如果ApplicationContext中存在多个TransactionManagementConfigurer
	 */
	public static PlatformTransactionManager retrieveTransactionManager(TestContext testContext, String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// 按类型和显式名称查找
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, PlatformTransactionManager.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve transaction manager named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory) {
				ListableBeanFactory lbf = (ListableBeanFactory) bf;

				// 按类型查找单个bean
				Map<String, PlatformTransactionManager> txMgrs =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, PlatformTransactionManager.class);
				if (txMgrs.size() == 1) {
					return txMgrs.values().iterator().next();
				}

				try {
					// 按类型查找单个bean, 支持'primary' beans
					return bf.getBean(PlatformTransactionManager.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}

				// 查找 TransactionManagementConfigurer
				Map<String, TransactionManagementConfigurer> configurers =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, TransactionManagementConfigurer.class);
				Assert.state(configurers.size() <= 1,
						"Only one TransactionManagementConfigurer may exist in the ApplicationContext");
				if (configurers.size() == 1) {
					return configurers.values().iterator().next().annotationDrivenTransactionManager();
				}
			}

			// 按类型和默认名称查找
			return bf.getBean(DEFAULT_TRANSACTION_MANAGER_NAME, PlatformTransactionManager.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, PlatformTransactionManager.class);
			return null;
		}
	}

	private static void logBeansException(TestContext testContext, BeansException ex, Class<?> beanType) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Caught exception while retrieving %s for test context %s",
				beanType.getSimpleName(), testContext), ex);
		}
	}

	/**
	 * 为提供的目标{@link TransactionAttribute}和{@link TestContext}创建委托{@link TransactionAttribute},
	 * 使用测试类和测试方法的名称来构建事务的名称.
	 * 
	 * @param testContext 基于名称的{@code TestContext}
	 * @param targetAttribute 要委托的{@code TransactionAttribute}
	 * 
	 * @return 委托{@code TransactionAttribute}
	 */
	public static TransactionAttribute createDelegatingTransactionAttribute(
			TestContext testContext, TransactionAttribute targetAttribute) {

		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(targetAttribute, "Target TransactionAttribute must not be null");
		return new TestContextTransactionAttribute(targetAttribute, testContext);
	}


	@SuppressWarnings("serial")
	private static class TestContextTransactionAttribute extends DelegatingTransactionAttribute {

		private final String name;

		public TestContextTransactionAttribute(TransactionAttribute targetAttribute, TestContext testContext) {
			super(targetAttribute);
			this.name = ClassUtils.getQualifiedMethodName(testContext.getTestMethod(), testContext.getTestClass());
		}

		@Override
		public String getName() {
			return this.name;
		}
	}
}
