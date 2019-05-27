package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

/**
 * {@code EmbeddedDatabaseConfigurer}封装了创建, 连接和关闭特定类型的嵌入式数据库所需的配置, 如HSQL, H2, 或 Derby.
 */
public interface EmbeddedDatabaseConfigurer {

	/**
	 * 配置创建和连接到嵌入式数据库所需的属性.
	 * 
	 * @param properties 要配置的连接属性
	 * @param databaseName 嵌入式数据库的名称
	 */
	void configureConnectionProperties(ConnectionProperties properties, String databaseName);

	/**
	 * 关闭支持提供的{@link DataSource}的嵌入式数据库实例.
	 * 
	 * @param dataSource 相应的{@link DataSource}
	 * @param databaseName 正在关闭的数据库的名称
	 */
	void shutdown(DataSource dataSource, String databaseName);

}
