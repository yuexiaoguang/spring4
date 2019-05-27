package org.springframework.jdbc.support;

import java.util.Collections;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.PatternMatchUtils;

/**
 * 用于根据{@link java.sql.DatabaseMetaData}中的"databaseProductName"创建{@link SQLErrorCodes}的工厂.
 *
 * <p>返回使用名为"sql-error-codes.xml"的配置文件中定义的供应商代码填充的{@code SQLErrorCodes}.
 * 如果没有被类路径根目录中的文件覆盖, 则读取此包中的默认文件(例如, 在"/WEB-INF/classes"目录中).
 */
public class SQLErrorCodesFactory {

	/**
	 * 自定义SQL错误代码文件的名称, 从类路径的根目录加载(e.g. 从"/WEB-INF/classes"目录加载).
	 */
	public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

	/**
	 * 从类路径加载的默认SQL错误代码文件的名称.
	 */
	public static final String SQL_ERROR_CODE_DEFAULT_PATH = "org/springframework/jdbc/support/sql-error-codes.xml";


	private static final Log logger = LogFactory.getLog(SQLErrorCodesFactory.class);

	/**
	 * 跟踪单个实例, 以便可以将其返回给请求它的类.
	 */
	private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();


	/**
	 * 返回单例实例.
	 */
	public static SQLErrorCodesFactory getInstance() {
		return instance;
	}


	/**
	 * 保存配置文件中定义的所有数据库的错误代码.
	 * Key是数据库产品名称, value是SQLErrorCodes实例.
	 */
	private final Map<String, SQLErrorCodes> errorCodesMap;

	/**
	 * 缓存每个DataSource的SQLErrorCodes实例.
	 */
	private final Map<DataSource, SQLErrorCodes> dataSourceCache =
			new ConcurrentReferenceHashMap<DataSource, SQLErrorCodes>(16);


	/**
	 * <p>强制Singleton设计模式.
	 * 除了允许通过覆盖{@link #loadResource(String)}方法进行测试之外, 它将是私有的.
	 * <p><b>不要在应用程序代码中进行子类化.</b>
	 */
	protected SQLErrorCodesFactory() {
		Map<String, SQLErrorCodes> errorCodes;

		try {
			DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
			lbf.setBeanClassLoader(getClass().getClassLoader());
			XmlBeanDefinitionReader bdr = new XmlBeanDefinitionReader(lbf);

			// 加载默认的SQL错误代码.
			Resource resource = loadResource(SQL_ERROR_CODE_DEFAULT_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
			}
			else {
				logger.warn("Default sql-error-codes.xml not found (should be included in spring.jar)");
			}

			// 加载自定义SQL错误代码, 覆盖默认值.
			resource = loadResource(SQL_ERROR_CODE_OVERRIDE_PATH);
			if (resource != null && resource.exists()) {
				bdr.loadBeanDefinitions(resource);
				logger.info("Found custom sql-error-codes.xml file at the root of the classpath");
			}

			// 检查SQLErrorCodes类型的所有bean.
			errorCodes = lbf.getBeansOfType(SQLErrorCodes.class, true, false);
			if (logger.isDebugEnabled()) {
				logger.debug("SQLErrorCodes loaded: " + errorCodes.keySet());
			}
		}
		catch (BeansException ex) {
			logger.warn("Error loading SQL error codes from config file", ex);
			errorCodes = Collections.emptyMap();
		}

		this.errorCodesMap = errorCodes;
	}

	/**
	 * 从类路径加载给定资源.
	 * <p><b>不应被应用程序开发人员覆盖, 应该从静态{@link #getInstance()}方法获取此类的实例.</b>
	 * <p>Protected用于测试.
	 * 
	 * @param path 资源路径; 自定义路径或{@link #SQL_ERROR_CODE_DEFAULT_PATH}和 {@link #SQL_ERROR_CODE_OVERRIDE_PATH}其中一个.
	 * 
	 * @return 资源, 或{@code null}
	 */
	protected Resource loadResource(String path) {
		return new ClassPathResource(path, getClass().getClassLoader());
	}


	/**
	 * 返回给定数据库的{@link SQLErrorCodes}实例.
	 * <p>无需数据库元数据查找.
	 * 
	 * @param databaseName 数据库名称 (不能是{@code null})
	 * 
	 * @return 给定数据库的{@code SQLErrorCodes}实例
	 * @throws IllegalArgumentException 如果提供的数据库名称为{@code null}
	 */
	public SQLErrorCodes getErrorCodes(String databaseName) {
		Assert.notNull(databaseName, "Database product name must not be null");

		SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
		if (sec == null) {
			for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
				if (PatternMatchUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
					sec = candidate;
					break;
				}
			}
		}
		if (sec != null) {
			checkCustomTranslatorRegistry(databaseName, sec);
			if (logger.isDebugEnabled()) {
				logger.debug("SQL error codes for '" + databaseName + "' found");
			}
			return sec;
		}

		// 无法在定义的数据库中找到数据库.
		if (logger.isDebugEnabled()) {
			logger.debug("SQL error codes for '" + databaseName + "' not found");
		}
		return new SQLErrorCodes();
	}

	/**
	 * 返回给定{@link DataSource}的{@link SQLErrorCodes},
	 * 评估{@link java.sql.DatabaseMetaData}中的"databaseProductName",
	 * 如果没有找到{@code SQLErrorCodes}则返回空错误代码实例.
	 * 
	 * @param dataSource 标识数据库的{@code DataSource}
	 * 
	 * @return 相应的{@code SQLErrorCodes}对象
	 */
	public SQLErrorCodes getErrorCodes(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
		}

		// 尝试对现有缓存条目进行有效的无锁访问
		SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
		if (sec == null) {
			synchronized (this.dataSourceCache) {
				// 在完整的dataSourceCache锁中仔细检查
				sec = this.dataSourceCache.get(dataSource);
				if (sec == null) {
					// 找不到它 - 要查找它.
					try {
						String name = (String) JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName");
						if (name != null) {
							return registerDatabase(dataSource, name);
						}
					}
					catch (MetaDataAccessException ex) {
						logger.warn("Error while extracting database name - falling back to empty error codes", ex);
					}
					// 返回一个空的SQLErrorCodes实例.
					return new SQLErrorCodes();
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
		}

		return sec;
	}

	/**
	 * 将指定的数据库名称与给定的{@link DataSource}相关联.
	 * 
	 * @param dataSource 标识数据库的{@code DataSource}
	 * @param databaseName 错误代码定义文件中所述的相应数据库名称 (must not be {@code null})
	 * 
	 * @return 相应的{@code SQLErrorCodes}对象 (never {@code null})
	 */
	public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
		SQLErrorCodes sec = getErrorCodes(databaseName);
		if (logger.isDebugEnabled()) {
			logger.debug("Caching SQL error codes for DataSource [" + identify(dataSource) +
					"]: database product name is '" + databaseName + "'");
		}
		this.dataSourceCache.put(dataSource, sec);
		return sec;
	}

	/**
	 * 如果已注册, 则清除指定的{@link DataSource}的缓存.
	 * 
	 * @param dataSource 标识数据库的{@code DataSource}
	 * 
	 * @return 已删除的相应{@code SQLErrorCodes}对象, 或{@code null}如果未注册
	 */
	public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
		return this.dataSourceCache.remove(dataSource);
	}

	/**
	 * 为给定的{@link DataSource}构建标识字符串, 主要用于记录目的.
	 * 
	 * @param dataSource 要内省的{@code DataSource}
	 * 
	 * @return 标识字符串
	 */
	private String identify(DataSource dataSource) {
		return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
	}

	/**
	 * 检查{@link CustomSQLExceptionTranslatorRegistry}.
	 */
	private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
		SQLExceptionTranslator customTranslator =
				CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
		if (customTranslator != null) {
			if (errorCodes.getCustomSqlExceptionTranslator() != null && logger.isWarnEnabled()) {
				logger.warn("Overriding already defined custom translator '" +
						errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName() +
						" with '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Using custom translator '" + customTranslator.getClass().getSimpleName() +
						"' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'");
			}
			errorCodes.setCustomSqlExceptionTranslator(customTranslator);
		}
	}
}
