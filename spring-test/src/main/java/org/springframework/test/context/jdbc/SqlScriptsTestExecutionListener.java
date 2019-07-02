package org.springframework.test.context.jdbc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TestContextTransactionUtils;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code TestExecutionListener}, 它为执行通过{@link Sql @Sql}注解配置的
 * SQL {@link Sql#scripts scripts}和内联{@link Sql#statements statements}提供支持.
 *
 * <p>脚本和内联语句将在相应的{@linkplain java.lang.reflect.Method 测试方法}
 * {@linkplain #beforeTestMethod(TestContext) 之前} 或{@linkplain #afterTestMethod(TestContext) 之后}执行,
 * 取决于{@link Sql#executionPhase executionPhase}标志的配置值.
 *
 * <p>脚本和内联语句将在没有事务的情况下, 在现有的Spring管理的事务中或在隔离的事务中执行,
 * 具体取决于{@link SqlConfig#transactionMode}的配置值以及事务管理器是否存在.
 *
 * <h3>脚本资源</h3>
 * <p>有关默认脚本检测以及如何解释脚本资源位置的详细信息, 请参阅{@link Sql#scripts}.
 *
 * <h3>需要的Spring Bean</h3>
 * <p>{@link PlatformTransactionManager} <em>和</em> {@link DataSource}, {@link PlatformTransactionManager},
 * 或者{@link DataSource}必须在Spring {@link ApplicationContext}中定义为bean, 用于相应的测试.
 * 有关允许的配置以及用于定位这些bean的算法的详细信息, 请参阅{@link SqlConfig#transactionMode},
 * {@link SqlConfig#transactionManager}, {@link SqlConfig#dataSource},
 * {@link TestContextTransactionUtils#retrieveDataSource}, 和
 * {@link TestContextTransactionUtils#retrieveTransactionManager}的javadoc.
 */
public class SqlScriptsTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(SqlScriptsTestExecutionListener.class);


	/**
	 * 返回{@code 5000}.
	 */
	@Override
	public final int getOrder() {
		return 5000;
	}

	/**
	 * 在当前测试方法<em>之前</em>, 执行通过{@link Sql @Sql}为所提供的{@link TestContext}配置的SQL脚本.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		executeSqlScripts(testContext, ExecutionPhase.BEFORE_TEST_METHOD);
	}

	/**
	 * 在当前测试方法<em>之后</em>, 执行通过{@link Sql @Sql}为所提供的{@link TestContext}配置的SQL脚本.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		executeSqlScripts(testContext, ExecutionPhase.AFTER_TEST_METHOD);
	}

	/**
	 * 执行通过{@link Sql @Sql}为提供的{@link TestContext}和{@link ExecutionPhase}配置的SQL脚本.
	 */
	private void executeSqlScripts(TestContext testContext, ExecutionPhase executionPhase) throws Exception {
		boolean classLevel = false;

		Set<Sql> sqlAnnotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(
				testContext.getTestMethod(), Sql.class, SqlGroup.class);
		if (sqlAnnotations.isEmpty()) {
			sqlAnnotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(
					testContext.getTestClass(), Sql.class, SqlGroup.class);
			if (!sqlAnnotations.isEmpty()) {
				classLevel = true;
			}
		}

		for (Sql sql : sqlAnnotations) {
			executeSqlScripts(sql, executionPhase, testContext, classLevel);
		}
	}

	/**
	 * 执行通过提供的{@link Sql @Sql}注解为给定的{@link ExecutionPhase}和{@link TestContext}配置的SQL脚本.
	 * <p>必须特别小心才能正确支持配置的{@link SqlConfig#transactionMode}.
	 * 
	 * @param sql 要解析的{@code @Sql}注解
	 * @param executionPhase 当前的执行阶段
	 * @param testContext 当前{@code TestContext}
	 * @param classLevel {@code true} 如果在类级别声明{@link Sql @Sql}
	 */
	private void executeSqlScripts(Sql sql, ExecutionPhase executionPhase, TestContext testContext, boolean classLevel)
			throws Exception {

		if (executionPhase != sql.executionPhase()) {
			return;
		}

		MergedSqlConfig mergedSqlConfig = new MergedSqlConfig(sql.config(), testContext.getTestClass());
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Processing %s for execution phase [%s] and test context %s.",
					mergedSqlConfig, executionPhase, testContext));
		}

		final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setSqlScriptEncoding(mergedSqlConfig.getEncoding());
		populator.setSeparator(mergedSqlConfig.getSeparator());
		populator.setCommentPrefix(mergedSqlConfig.getCommentPrefix());
		populator.setBlockCommentStartDelimiter(mergedSqlConfig.getBlockCommentStartDelimiter());
		populator.setBlockCommentEndDelimiter(mergedSqlConfig.getBlockCommentEndDelimiter());
		populator.setContinueOnError(mergedSqlConfig.getErrorMode() == ErrorMode.CONTINUE_ON_ERROR);
		populator.setIgnoreFailedDrops(mergedSqlConfig.getErrorMode() == ErrorMode.IGNORE_FAILED_DROPS);

		String[] scripts = getScripts(sql, testContext, classLevel);
		scripts = TestContextResourceUtils.convertToClasspathResourcePaths(testContext.getTestClass(), scripts);
		List<Resource> scriptResources = TestContextResourceUtils.convertToResourceList(
				testContext.getApplicationContext(), scripts);
		for (String stmt : sql.statements()) {
			if (StringUtils.hasText(stmt)) {
				stmt = stmt.trim();
				scriptResources.add(new ByteArrayResource(stmt.getBytes(), "from inlined SQL statement: " + stmt));
			}
		}
		populator.setScripts(scriptResources.toArray(new Resource[scriptResources.size()]));
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL scripts: " + ObjectUtils.nullSafeToString(scriptResources));
		}

		String dsName = mergedSqlConfig.getDataSource();
		String tmName = mergedSqlConfig.getTransactionManager();
		DataSource dataSource = TestContextTransactionUtils.retrieveDataSource(testContext, dsName);
		PlatformTransactionManager txMgr = TestContextTransactionUtils.retrieveTransactionManager(testContext, tmName);
		boolean newTxRequired = (mergedSqlConfig.getTransactionMode() == TransactionMode.ISOLATED);

		if (txMgr == null) {
			if (newTxRequired) {
				throw new IllegalStateException(String.format("Failed to execute SQL scripts for test context %s: " +
						"cannot execute SQL scripts using Transaction Mode [%s] without a PlatformTransactionManager.",
						testContext, TransactionMode.ISOLATED));
			}
			if (dataSource == null) {
				throw new IllegalStateException(String.format("Failed to execute SQL scripts for test context %s: " +
						"supply at least a DataSource or PlatformTransactionManager.", testContext));
			}
			// 直接针对DataSource执行脚本
			populator.execute(dataSource);
		}
		else {
			DataSource dataSourceFromTxMgr = getDataSourceFromTransactionManager(txMgr);
			// 确保用户配置了适当的 DataSource/TransactionManager 对.
			if (dataSource != null && dataSourceFromTxMgr != null && !dataSource.equals(dataSourceFromTxMgr)) {
				throw new IllegalStateException(String.format("Failed to execute SQL scripts for test context %s: " +
						"the configured DataSource [%s] (named '%s') is not the one associated with " +
						"transaction manager [%s] (named '%s').", testContext, dataSource.getClass().getName(),
						dsName, txMgr.getClass().getName(), tmName));
			}
			if (dataSource == null) {
				dataSource = dataSourceFromTxMgr;
				if (dataSource == null) {
					throw new IllegalStateException(String.format("Failed to execute SQL scripts for " +
							"test context %s: could not obtain DataSource from transaction manager [%s] (named '%s').",
							testContext, txMgr.getClass().getName(), tmName));
				}
			}
			final DataSource finalDataSource = dataSource;
			int propagation = (newTxRequired ? TransactionDefinition.PROPAGATION_REQUIRES_NEW :
					TransactionDefinition.PROPAGATION_REQUIRED);
			TransactionAttribute txAttr = TestContextTransactionUtils.createDelegatingTransactionAttribute(
					testContext, new DefaultTransactionAttribute(propagation));
			new TransactionTemplate(txMgr, txAttr).execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					populator.execute(finalDataSource);
				}
			});
		}
	}

	private DataSource getDataSourceFromTransactionManager(PlatformTransactionManager transactionManager) {
		try {
			Method getDataSourceMethod = transactionManager.getClass().getMethod("getDataSource");
			Object obj = ReflectionUtils.invokeMethod(getDataSourceMethod, transactionManager);
			if (obj instanceof DataSource) {
				return (DataSource) obj;
			}
		}
		catch (Exception ex) {
			// ignore
		}
		return null;
	}

	private String[] getScripts(Sql sql, TestContext testContext, boolean classLevel) {
		String[] scripts = sql.scripts();
		if (ObjectUtils.isEmpty(scripts) && ObjectUtils.isEmpty(sql.statements())) {
			scripts = new String[] {detectDefaultScript(testContext, classLevel)};
		}
		return scripts;
	}

	/**
	 * 通过实现{@link Sql#scripts}中定义的算法来检测默认SQL脚本.
	 */
	private String detectDefaultScript(TestContext testContext, boolean classLevel) {
		Class<?> clazz = testContext.getTestClass();
		Method method = testContext.getTestMethod();
		String elementType = (classLevel ? "class" : "method");
		String elementName = (classLevel ? clazz.getName() : method.toString());

		String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName());
		if (!classLevel) {
			resourcePath += "." + method.getName();
		}
		resourcePath += ".sql";

		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);

		if (classPathResource.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Detected default SQL script \"%s\" for test %s [%s]",
						prefixedResourcePath, elementType, elementName));
			}
			return prefixedResourcePath;
		}
		else {
			String msg = String.format("Could not detect default SQL script for test %s [%s]: " +
					"%s does not exist. Either declare statements or scripts via @Sql or make the " +
					"default SQL script available.", elementType, elementName, classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
