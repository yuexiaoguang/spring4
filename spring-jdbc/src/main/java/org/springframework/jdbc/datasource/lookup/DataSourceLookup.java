package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

/**
 * 用于按名称查找DataSource的策略接口.
 *
 * <p>例如, 用于解析JPA {@code persistence.xml}文件中的数据源名称.
 */
public interface DataSourceLookup {

	/**
	 * 检索由给定名称标识的DataSource.
	 * 
	 * @param dataSourceName DataSource的名称
	 * 
	 * @return the DataSource (never {@code null})
	 * @throws DataSourceLookupFailureException 如果查找失败
	 */
	DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException;

}
