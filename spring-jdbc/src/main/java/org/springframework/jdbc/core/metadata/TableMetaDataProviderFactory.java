package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * 用于根据正在使用的数据库类型创建{@link TableMetaDataProvider}实现的工厂.
 */
public class TableMetaDataProviderFactory {

	private static final Log logger = LogFactory.getLog(TableMetaDataProviderFactory.class);


	/**
	 * @param dataSource 用于检索元数据
	 * @param context 包含配置和元数据的类
	 * 
	 * @return 要使用的TableMetaDataProvider实现的实例
	 */
	public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource, TableMetaDataContext context) {
		return createMetaDataProvider(dataSource, context, null);
	}

	/**
	 * @param dataSource 用于检索元数据
	 * @param context 包含配置和元数据的类
	 * @param nativeJdbcExtractor 要使用的NativeJdbcExtractor
	 * 
	 * @return 要使用的TableMetaDataProvider实现的实例
	 */
	public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource,
				final TableMetaDataContext context, final NativeJdbcExtractor nativeJdbcExtractor) {
		try {
			return (TableMetaDataProvider) JdbcUtils.extractDatabaseMetaData(dataSource,
					new DatabaseMetaDataCallback() {
						@Override
						public Object processMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
							String databaseProductName =
									JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
							boolean accessTableColumnMetaData = context.isAccessTableColumnMetaData();
							TableMetaDataProvider provider;

							if ("Oracle".equals(databaseProductName)) {
								provider = new OracleTableMetaDataProvider(
										databaseMetaData, context.isOverrideIncludeSynonymsDefault());
							}
							else if ("PostgreSQL".equals(databaseProductName)) {
								provider = new PostgresTableMetaDataProvider(databaseMetaData);
							}
							else if ("Apache Derby".equals(databaseProductName)) {
								provider = new DerbyTableMetaDataProvider(databaseMetaData);
							}
							else if ("HSQL Database Engine".equals(databaseProductName)) {
								provider = new HsqlTableMetaDataProvider(databaseMetaData);
							}
							else {
								provider = new GenericTableMetaDataProvider(databaseMetaData);
							}
							if (nativeJdbcExtractor != null) {
								provider.setNativeJdbcExtractor(nativeJdbcExtractor);
							}

							if (logger.isDebugEnabled()) {
								logger.debug("Using " + provider.getClass().getSimpleName());
							}
							provider.initializeWithMetaData(databaseMetaData);
							if (accessTableColumnMetaData) {
								provider.initializeWithTableColumnMetaData(databaseMetaData,
										context.getCatalogName(), context.getSchemaName(), context.getTableName());
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
