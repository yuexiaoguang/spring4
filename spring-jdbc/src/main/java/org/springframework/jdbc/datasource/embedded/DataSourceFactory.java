package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code DataSourceFactory}封装了特定{@link DataSource}实现的创建,
 * 例如{@link org.springframework.jdbc.datasource.SimpleDriverDataSource SimpleDriverDataSource}或连接池, 例如Apache DBCP或C3P0.
 *
 * <p>在调用{@link #getDataSource()}实际获取已配置的{@code DataSource}实例之前,
 * 调用{@link #getConnectionProperties()}来配置规范化的{@code DataSource}属性.
 */
public interface DataSourceFactory {

	/**
	 * 获取要配置的{@link #getDataSource DataSource}的{@linkplain ConnectionProperties 连接属性}.
	 */
	ConnectionProperties getConnectionProperties();

	/**
	 * 获取{@link DataSource}, 并应用{@linkplain #getConnectionProperties 连接属性}.
	 */
	DataSource getDataSource();

}
