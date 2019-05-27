package org.springframework.jdbc.datasource.lookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * 简单的{@link DataSourceLookup}实现, 依赖于Map进行查找.
 *
 * <p>用于需要将任意{@link String}名称与目标{@link DataSource}对象匹配的测试环境或应用程序.
 */
public class MapDataSourceLookup implements DataSourceLookup {

	private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>(4);


	public MapDataSourceLookup() {
	}

	/**
	 * @param dataSources {@link DataSource DataSources}的{@link Map};
	 * 键是{@link String Strings}, 值是实际的{@link DataSource}实例.
	 */
	public MapDataSourceLookup(Map<String, DataSource> dataSources) {
		setDataSources(dataSources);
	}

	/**
	 * @param dataSourceName 要添加提供的{@link DataSource}的名称
	 * @param dataSource 要添加的{@link DataSource}
	 */
	public MapDataSourceLookup(String dataSourceName, DataSource dataSource) {
		addDataSource(dataSourceName, dataSource);
	}


	/**
	 * 设置{@link DataSource DataSources}的{@link Map}; 键是{@link String Strings}, 值是实际的{@link DataSource}实例.
	 * <p>如果提供的{@link Map}是{@code null}, 则此方法调用无效.
	 * 
	 * @param dataSources {@link DataSource DataSources}的{@link Map}
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		if (dataSources != null) {
			this.dataSources.putAll(dataSources);
		}
	}

	/**
	 * 获取此对象维护的{@link DataSource DataSources}的{@link Map}.
	 * <p>返回的{@link Map}是{@link Collections#unmodifiableMap(java.util.Map) unmodifiable}.
	 * 
	 * @return {@link DataSource DataSources}的{@link Map} (never {@code null})
	 */
	public Map<String, DataSource> getDataSources() {
		return Collections.unmodifiableMap(this.dataSources);
	}

	/**
	 * 将提供的{@link DataSource}添加到此对象维护的{@link DataSource DataSources}的映射中.
	 * 
	 * @param dataSourceName 要添加提供的{@link DataSource}的名称
	 * @param dataSource 要添加的{@link DataSource}
	 */
	public void addDataSource(String dataSourceName, DataSource dataSource) {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSources.put(dataSourceName, dataSource);
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		Assert.notNull(dataSourceName, "DataSource name must not be null");
		DataSource dataSource = this.dataSources.get(dataSourceName);
		if (dataSource == null) {
			throw new DataSourceLookupFailureException(
					"No DataSource with name '" + dataSourceName + "' registered");
		}
		return dataSource;
	}
}
