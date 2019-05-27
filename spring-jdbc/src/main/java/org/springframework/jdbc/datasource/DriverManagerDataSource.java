package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 标准JDBC {@link javax.sql.DataSource}接口简单实现,
 * 通过bean属性配置普通旧JDBC {@link java.sql.DriverManager},
 * 并从每个{@code getConnection}调用返回一个新的{@link java.sql.Connection}.
 *
 * <p><b>NOTE: 该类不是实际的连接池; 它实际上并没有池连接.</b>
 * 它只是简单替代完整的连接池, 实现相同的标准接口, 但在每次调用时都创建新的连接.
 *
 * <p>对于J2EE容器外部的测试或独立环境非常有用,
 * 可以作为相应ApplicationContext中的DataSource bean, 也可以与简单的JNDI环境结合使用.
 * 池假设{@code Connection.close()}调用只会关闭Connection, 因此任何支持DataSource的持久性代码都应该有效.
 *
 * <p><b>NOTE: 在特殊的类加载环境中, 如OSGi, 
 * 由于通过直接Driver使用解析的JDBC DriverManager的一般类加载问题 (这正是SimpleDriverDataSource所做的),
 * 因此这个类实际上被{@link SimpleDriverDataSource}取代了.</b>
 *
 * <p>在J2EE容器中, 建议使用容器提供的JNDI DataSource.
 * 这样的DataSource可以通过{@link org.springframework.jndi.JndiObjectFactoryBean}
 * 在Spring ApplicationContext中作为DataSource bean公开, 以便无缝切换到本地DataSource bean, 就像这个类一样.
 * 对于测试, 可以通过Spring的{@link org.springframework.mock.jndi.SimpleNamingContextBuilder}设置模拟JNDI环境,
 * 或者将bean定义切换到本地DataSource (这更简单, 因此推荐).
 *
 * <p>如果需要J2EE容器之外的"真实"连接池, 请考虑
 * <a href="http://commons.apache.org/proper/commons-dbcp">Apache Commons DBCP</a>
 * 或<a href="http://sourceforge.net/projects/c3p0">C3P0</a>.
 * Commons DBCP的 BasicDataSource和C3P0的ComboPooledDataSource是完整的连接池bean,
 * 支持与此类相同的基本属性以及特定设置 (例如最小/最大池大小等).
 */
public class DriverManagerDataSource extends AbstractDriverBasedDataSource {

	/**
	 * bean风格配置的构造函数.
	 */
	public DriverManagerDataSource() {
	}

	/**
	 * 使用给定的JDBC URL创建新的DriverManagerDataSource, 而不是为JDBC访问指定用户名或密码.
	 * 
	 * @param url 用于访问DriverManager的JDBC URL
	 */
	public DriverManagerDataSource(String url) {
		setUrl(url);
	}

	/**
	 * 使用给定的标准DriverManager参数创建新的DriverManagerDataSource.
	 * 
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param username 用于访问DriverManager的JDBC用户名
	 * @param password 用于访问DriverManager的JDBC密码
	 */
	public DriverManagerDataSource(String url, String username, String password) {
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	/**
	 * 使用给定的JDBC URL创建新的DriverManagerDataSource, 而不是为JDBC访问指定用户名或密码.
	 * 
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param conProps JDBC连接属性
	 */
	public DriverManagerDataSource(String url, Properties conProps) {
		setUrl(url);
		setConnectionProperties(conProps);
	}


	/**
	 * 设置JDBC驱动程序类名称.
	 * 该驱动程序将在启动时初始化, 并使用JDK的DriverManager注册.
	 * <p><b>NOTE: DriverManagerDataSource主要用于访问<i>预先注册的</i> JDBC驱动程序.</b>
	 * 如果需要注册新的驱动程序, 请考虑使用{@link SimpleDriverDataSource}.
	 * 或者, 在实例化此DataSource之前, 请考虑自己初始化JDBC驱动程序.
	 * "driverClassName"属性主要是为了向后兼容, 以及在Commons DBCP和此DataSource之间迁移而保留的.
	 */
	public void setDriverClassName(String driverClassName) {
		Assert.hasText(driverClassName, "Property 'driverClassName' must not be empty");
		String driverClassNameToUse = driverClassName.trim();
		try {
			Class.forName(driverClassNameToUse, true, ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Could not load JDBC driver class [" + driverClassNameToUse + "]", ex);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Loaded JDBC driver: " + driverClassNameToUse);
		}
	}


	@Override
	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		String url = getUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Creating new JDBC DriverManager Connection to [" + url + "]");
		}
		return getConnectionFromDriverManager(url, props);
	}

	/**
	 * 使用来自DriverManager的令人讨厌的静态获取连接, 被提取到受保护的方法中, 以便于单元测试.
	 */
	protected Connection getConnectionFromDriverManager(String url, Properties props) throws SQLException {
		return DriverManager.getConnection(url, props);
	}
}
