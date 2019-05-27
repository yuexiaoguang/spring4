package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * DataSourceLookup的一个实现, 它简单地包装一个给定的DataSource, 为任何数据源名称返回.
 */
public class SingleDataSourceLookup implements DataSourceLookup {

	private final DataSource dataSource;


	/**
	 * @param dataSource 要包装的单个{@link DataSource}
	 */
	public SingleDataSourceLookup(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSource = dataSource;
	}


	@Override
	public DataSource getDataSource(String dataSourceName) {
		return this.dataSource;
	}

}
