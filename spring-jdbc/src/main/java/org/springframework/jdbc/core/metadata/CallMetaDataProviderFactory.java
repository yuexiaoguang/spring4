package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * 用于根据正在使用的数据库类型创建{@link CallMetaDataProvider}实现的工厂.
 */
public class CallMetaDataProviderFactory {

	/** 过程调用支持的数据库产品列表 */
	public static final List<String> supportedDatabaseProductsForProcedures = Arrays.asList(
			"Apache Derby",
			"DB2",
			"MySQL",
			"Microsoft SQL Server",
			"Oracle",
			"PostgreSQL",
			"Sybase"
		);

	/** 函数调用支持的数据库产品列表 */
	public static final List<String> supportedDatabaseProductsForFunctions = Arrays.asList(
			"MySQL",
			"Microsoft SQL Server",
			"Oracle",
			"PostgreSQL"
		);

	private static final Log logger = LogFactory.getLog(CallMetaDataProviderFactory.class);


	/**
	 * @param dataSource 用于检索元数据的JDBC DataSource
	 * @param context 包含配置和元数据的类
	 * 
	 * @return 要使用的CallMetaDataProvider实现的实例
	 */
	public static CallMetaDataProvider createMetaDataProvider(DataSource dataSource, final CallMetaDataContext context) {
		try {
			return (CallMetaDataProvider) JdbcUtils.extractDatabaseMetaData(dataSource, new DatabaseMetaDataCallback() {
				@Override
				public Object processMetaData(DatabaseMetaData databaseMetaData) throws SQLException, MetaDataAccessException {
					String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
					boolean accessProcedureColumnMetaData = context.isAccessCallParameterMetaData();
					if (context.isFunction()) {
						if (!supportedDatabaseProductsForFunctions.contains(databaseProductName)) {
							if (logger.isWarnEnabled()) {
								logger.warn(databaseProductName + " is not one of the databases fully supported for function calls " +
										"-- supported are: " + supportedDatabaseProductsForFunctions);
							}
							if (accessProcedureColumnMetaData) {
								logger.warn("Metadata processing disabled - you must specify all parameters explicitly");
								accessProcedureColumnMetaData = false;
							}
						}
					}
					else {
						if (!supportedDatabaseProductsForProcedures.contains(databaseProductName)) {
							if (logger.isWarnEnabled()) {
								logger.warn(databaseProductName + " is not one of the databases fully supported for procedure calls " +
										"-- supported are: " + supportedDatabaseProductsForProcedures);
							}
							if (accessProcedureColumnMetaData) {
								logger.warn("Metadata processing disabled - you must specify all parameters explicitly");
								accessProcedureColumnMetaData = false;
							}
						}
					}

					CallMetaDataProvider provider;
					if ("Oracle".equals(databaseProductName)) {
						provider = new OracleCallMetaDataProvider(databaseMetaData);
					}
					else if ("PostgreSQL".equals(databaseProductName)) {
						provider = new PostgresCallMetaDataProvider((databaseMetaData));
					}
					else if ("Apache Derby".equals(databaseProductName)) {
						provider = new DerbyCallMetaDataProvider((databaseMetaData));
					}
					else if ("DB2".equals(databaseProductName)) {
						provider = new Db2CallMetaDataProvider((databaseMetaData));
					}
					else if ("HDB".equals(databaseProductName)) {
						provider = new HanaCallMetaDataProvider((databaseMetaData));
					}
					else if ("Microsoft SQL Server".equals(databaseProductName)) {
						provider = new SqlServerCallMetaDataProvider((databaseMetaData));
					}
					else if ("Sybase".equals(databaseProductName)) {
						provider = new SybaseCallMetaDataProvider((databaseMetaData));
					}
					else {
						provider = new GenericCallMetaDataProvider(databaseMetaData);
					}

					if (logger.isDebugEnabled()) {
						logger.debug("Using " + provider.getClass().getName());
					}
					provider.initializeWithMetaData(databaseMetaData);
					if (accessProcedureColumnMetaData) {
						provider.initializeWithProcedureColumnMetaData(databaseMetaData,
								context.getCatalogName(), context.getSchemaName(), context.getProcedureName());
					}
					return provider;
				}
			});
		}
		catch (MetaDataAccessException ex) {
			throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
		}
	}

}
