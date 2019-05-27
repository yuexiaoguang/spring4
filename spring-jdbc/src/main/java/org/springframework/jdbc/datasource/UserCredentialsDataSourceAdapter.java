package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 目标JDBC {@link javax.sql.DataSource}的适配器,
 * 将指定的用户凭据应用于每个标准{@code getConnection()}调用,
 * 在目标上隐式调用{@code getConnection(username, password)}.
 * 所有其他方法只是委托给目标DataSource的相应方法.
 *
 * <p>可用于代理未配置用户凭据的目标JNDI DataSource.
 * 客户端代码可以像往常一样使用标准{@code getConnection()}调用来使用此DataSource.
 *
 * <p>在下面的示例中, 客户端代码可以简单地透明地使用预配置的"myDataSource",
 * 使用指定的用户凭据隐式访问"myTargetDataSource".
 *
 * <pre class="code">
 * &lt;bean id="myTargetDataSource" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jdbc/myds"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.UserCredentialsDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource" ref="myTargetDataSource"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean></pre>
 *
 * <p>如果"username"为空, 则此代理将简单地委托给目标DataSource的标准{@code getConnection()}方法.
 * 这可以用于保持UserCredentialsDataSourceAdapter bean定义仅用于隐式传入用户凭据的<i>选项</i>,
 * 如果特定目标DataSource需要它.
 */
public class UserCredentialsDataSourceAdapter extends DelegatingDataSource {

	private String username;

	private String password;

	private String catalog;

	private String schema;

	private final ThreadLocal<JdbcUserCredentials> threadBoundCredentials =
			new NamedThreadLocal<JdbcUserCredentials>("Current JDBC user credentials");


	/**
	 * 设置此适配器用于检索Connection的默认用户名.
	 * <p>默认不指定用户. 请注意, 显式指定的用户名将始终覆盖在DataSource级别指定的任何用户名/密码.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 设置此适配器用于检索Connection的默认密码.
	 * <p>默认不指定密码. 请注意, 显式指定的用户名将始终覆盖在DataSource级别指定的任何用户名/密码.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 指定要应用于每个检索的Connection的数据库catalog.
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**
	 * 指定要应用于每个检索的Connection的数据库schema.
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}


	/**
	 * 为此代理和当前线程设置用户可信度.
	 * 给定的用户名和密码将应用于此DataSource代理上的所有后续{@code getConnection()}调用.
	 * <p>这将覆盖任何静态指定的用户凭据, 即"username" and "password" bean属性的值.
	 * 
	 * @param username 要应用的用户名
	 * @param password 要应用的密码
	 */
	public void setCredentialsForCurrentThread(String username, String password) {
		this.threadBoundCredentials.set(new JdbcUserCredentials(username, password));
	}

	/**
	 * 从当前线程中删除此代理的所有用户凭据.
	 * 之后将再次应用静态指定的用户凭据.
	 */
	public void removeCredentialsFromCurrentThread() {
		this.threadBoundCredentials.remove();
	}


	/**
	 * 确定当前是否存在线程绑定凭据, 如果可用则使用它们,
	 * 否则回退到静态指定的用户名和密码 (i.e. bean属性的值).
	 * <p>使用确定的凭据作为参数, 委托给{@link #doGetConnection(String, String)}.
	 */
	@Override
	@UsesJava7
	public Connection getConnection() throws SQLException {
		JdbcUserCredentials threadCredentials = this.threadBoundCredentials.get();
		Connection con = (threadCredentials != null ?
				doGetConnection(threadCredentials.username, threadCredentials.password) :
				doGetConnection(this.username, this.password));

		if (this.catalog != null) {
			con.setCatalog(this.catalog);
		}
		if (this.schema != null) {
			con.setSchema(this.schema);
		}
		return con;
	}

	/**
	 * 委托给{@link #doGetConnection(String, String)}, 保持给定的用户凭证不变.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return doGetConnection(username, password);
	}

	/**
	 * 此实现委托给目标DataSource的{@code getConnection(username, password)}方法, 传入指定的用户凭据.
	 * 如果指定的用户名为空, 则它将简单地委托给目标DataSource的标准{@code getConnection()}方法.
	 * 
	 * @param username 要使用的用户名
	 * @param password 要使用的密码
	 * 
	 * @return the Connection
	 */
	protected Connection doGetConnection(String username, String password) throws SQLException {
		Assert.state(getTargetDataSource() != null, "'targetDataSource' is required");
		if (StringUtils.hasLength(username)) {
			return getTargetDataSource().getConnection(username, password);
		}
		else {
			return getTargetDataSource().getConnection();
		}
	}


	/**
	 * 用作ThreadLocal值的内部类.
	 */
	private static class JdbcUserCredentials {

		public final String username;

		public final String password;

		private JdbcUserCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public String toString() {
			return "JdbcUserCredentials[username='" + this.username + "',password='" + this.password + "']";
		}
	}

}
