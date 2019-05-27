package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * 标准JDBC {@link javax.sql.DataSource}接口的简单实现,
 * 通过bean属性配置普通旧JDBC {@link java.sql.DriverManager},
 * 并从每个{@code getConnection}调用返回一个新的{@link java.sql.Connection}.
 *
 * <p><b>NOTE: 该类不是实际的连接池; 它实际上并没有池连接.</b>
 * 它只是简单替代完整的连接池, 实现相同的标准接口, 但在每次调用时都创建新的连接.
 *
 * <p>在Java EE容器中, 建议使用容器提供的JNDI DataSource.
 * 这样的DataSource可以通过{@link org.springframework.jndi.JndiObjectFactoryBean}
 * 在Spring ApplicationContext中作为DataSource bean公开,
 * 以便无缝切换到本地DataSource bean, 就像这个类一样.
 *
 * <p>如果需要J2EE容器之外的"真实"连接池, 请考虑
 * <a href="http://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * 或<a href="http://sourceforge.net/projects/c3p0">C3P0</a>.
 * Commons DBCP的 BasicDataSource和C3P0的ComboPooledDataSource是完整的连接池bean,
 * 支持与此类相同的基本属性以及特定设置 (例如最小/最大池大小等).
 */
public class SimpleDriverDataSource extends AbstractDriverBasedDataSource {

	private Driver driver;


	/**
	 * bean风格配置的构造函数.
	 */
	public SimpleDriverDataSource() {
	}

	/**
	 * 使用给定的标准Driver参数创建一个新的DriverManagerDataSource.
	 * 
	 * @param driver JDBC Driver对象
	 * @param url 用于访问DriverManager的JDBC URL
	 */
	public SimpleDriverDataSource(Driver driver, String url) {
		setDriver(driver);
		setUrl(url);
	}

	/**
	 * 使用给定的标准Driver参数创建一个新的DriverManagerDataSource.
	 * 
	 * @param driver JDBC Driver对象
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param username 用于访问DriverManager的JDBC用户名
	 * @param password 用于访问DriverManager的JDBC密码
	 */
	public SimpleDriverDataSource(Driver driver, String url, String username, String password) {
		setDriver(driver);
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	/**
	 * 使用给定的标准Driver参数创建一个新的DriverManagerDataSource.
	 * 
	 * @param driver JDBC Driver对象
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param conProps JDBC连接属性
	 */
	public SimpleDriverDataSource(Driver driver, String url, Properties conProps) {
		setDriver(driver);
		setUrl(url);
		setConnectionProperties(conProps);
	}


	/**
	 * 指定要使用的JDBC驱动程序实现类.
	 * <p>将在SimpleDriverDataSource中创建并保存此Driver类的实例.
	 */
	public void setDriverClass(Class<? extends Driver> driverClass) {
		this.driver = BeanUtils.instantiateClass(driverClass);
	}

	/**
	 * 指定要使用的JDBC驱动程序实例.
	 * <p>这允许传入共享的, 可能预先配置的驱动程序实例.
	 */
	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	/**
	 * 返回要使用的JDBC驱动程序实例.
	 */
	public Driver getDriver() {
		return this.driver;
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		Driver driver = getDriver();
		String url = getUrl();
		Assert.notNull(driver, "Driver must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC Driver Connection to [" + url + "]");
		}
		return driver.connect(url, props);
	}

}
