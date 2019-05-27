package org.springframework.jdbc.datasource.embedded;

import java.sql.Driver;

/**
 * {@code ConnectionProperties}作为一个简单的数据容器, 允许一致地配置基本的JDBC连接属性,
 * 与实际的{@link javax.sql.DataSource DataSource}实现无关.
 */
public interface ConnectionProperties {

	/**
	 * 设置用于连接到数据库的JDBC驱动程序类.
	 * 
	 * @param driverClass jdbc驱动程序类
	 */
	void setDriverClass(Class<? extends Driver> driverClass);

	/**
	 * 设置数据库的JDBC连接URL.
	 * 
	 * @param url 连接url
	 */
	void setUrl(String url);

	/**
	 * 设置用于连接数据库的用户名.
	 * 
	 * @param username 用户名
	 */
	void setUsername(String username);

	/**
	 * 设置用于连接数据库的密码.
	 * 
	 * @param password 密码
	 */
	void setPassword(String password);

}
