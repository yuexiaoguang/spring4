package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;

/**
 * JDBC {@link javax.sql.DataSource}实现的抽象基类, 它在JDBC {@link java.sql.Driver}上运行.
 */
public abstract class AbstractDriverBasedDataSource extends AbstractDataSource {

	private String url;

	private String username;

	private String password;

	private String catalog;

	private String schema;

	private Properties connectionProperties;


	/**
	 * 设置用于通过Driver进行连接的JDBC URL.
	 */
	public void setUrl(String url) {
		Assert.hasText(url, "Property 'url' must not be empty");
		this.url = url.trim();
	}

	/**
	 * 返回用于通过Driver连接的JDBC URL.
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * 设置用于通过Driver连接的JDBC用户名.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 返回用于通过Driver连接的JDBC用户名.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置用于通过Driver进行连接的JDBC密码.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 返回用于通过Driver连接的JDBC密码.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * 指定要应用于每个Connection的数据库catalog.
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**
	 * 返回要应用于每个Connection的数据库catalog.
	 */
	public String getCatalog() {
		return this.catalog;
	}

	/**
	 * 指定要应用于每个Connection的数据库schema.
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * 返回要应用于每个Connection的数据库schema.
	 */
	public String getSchema() {
		return this.schema;
	}

	/**
	 * 将任意连接属性指定为键/值对, 以传递给 Driver.
	 * <p>还可以包含"user"和"password"属性.
	 * 但是, 在此DataSource上指定的任何"username" 和 "password" bean属性都将覆盖相应的连接属性.
	 */
	public void setConnectionProperties(Properties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	/**
	 * 返回要传递给Driver的连接属性.
	 */
	public Properties getConnectionProperties() {
		return this.connectionProperties;
	}


	/**
	 * 此实现委托给{@code getConnectionFromDriver}, 用此DataSource的默认用户名和密码.
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return getConnectionFromDriver(getUsername(), getPassword());
	}

	/**
	 * 此实现委托给{@code getConnectionFromDriver}, 使用给定的用户名和密码.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnectionFromDriver(username, password);
	}


	/**
	 * 构建Driver的属性, 包括给定的用户名和密码, 并获取相应的Connection.
	 * 
	 * @param username 用户名
	 * @param password 密码
	 * 
	 * @return 获取的Connection
	 * @throws SQLException 失败
	 */
	@UsesJava7
	protected Connection getConnectionFromDriver(String username, String password) throws SQLException {
		Properties mergedProps = new Properties();
		Properties connProps = getConnectionProperties();
		if (connProps != null) {
			mergedProps.putAll(connProps);
		}
		if (username != null) {
			mergedProps.setProperty("user", username);
		}
		if (password != null) {
			mergedProps.setProperty("password", password);
		}

		Connection con = getConnectionFromDriver(mergedProps);
		if (this.catalog != null) {
			con.setCatalog(this.catalog);
		}
		if (this.schema != null) {
			con.setSchema(this.schema);
		}
		return con;
	}

	/**
	 * 使用给定属性获取Connection.
	 * <p>由子类实现的模板方法.
	 * 
	 * @param props 合并的连接属性
	 * 
	 * @return 获取的Connection
	 * @throws SQLException 失败
	 */
	protected abstract Connection getConnectionFromDriver(Properties props) throws SQLException;

}
