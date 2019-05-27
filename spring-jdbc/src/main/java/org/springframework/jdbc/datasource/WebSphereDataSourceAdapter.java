package org.springframework.jdbc.datasource;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DataSource}实现, 它将所有调用委托给WebSphere目标{@link DataSource},
 * 通常从JNDI获取, 将当前隔离级别和/或当前用户凭证应用于从中获取的每个Connection.
 *
 * <p>使用特定于IBM的API从WebSphere DataSource获取具有特定隔离级别 (和只读标志)的JDBC连接
 * (<a href="http://publib.boulder.ibm.com/infocenter/wasinfo/v5r1//topic/com.ibm.websphere.base.doc/info/aes/ae/rdat_extiapi.html">IBM code example</a>).
 * 支持
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()}
 * 公开的特定于事务的隔离级别.
 * 当前Spring管理的事务没有定义特定的隔离级别时, 也可以指定默认隔离级别.
 *
 * <p>用法示例, 将目标DataSource定义为内部Bean JNDI查找
 * (当然, 可以通过bean引用链接到任何WebSphere DataSource):
 *
 * <pre class="code">
 * &lt;bean id="myDataSource" class="org.springframework.jdbc.datasource.WebSphereDataSourceAdapter"&gt;
 *   &lt;property name="targetDataSource"&gt;
 *     &lt;bean class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *       &lt;property name="jndiName" value="jdbc/myds"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Thanks to Ricardo Olivieri for submitting the original implementation of this approach!
 */
public class WebSphereDataSourceAdapter extends IsolationLevelDataSourceAdapter {

	protected final Log logger = LogFactory.getLog(getClass());

	private Class<?> wsDataSourceClass;

	private Method newJdbcConnSpecMethod;

	private Method wsDataSourceGetConnectionMethod;

	private Method setTransactionIsolationMethod;

	private Method setReadOnlyMethod;

	private Method setUserNameMethod;

	private Method setPasswordMethod;


	/**
	 * 此构造函数检索WebSphere JDBC连接规范API, 因此可以使用反射获取特定的WebSphere Connection.
	 */
	public WebSphereDataSourceAdapter() {
		try {
			this.wsDataSourceClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSDataSource");
			Class<?> jdbcConnSpecClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.JDBCConnectionSpec");
			Class<?> wsrraFactoryClass = getClass().getClassLoader().loadClass("com.ibm.websphere.rsadapter.WSRRAFactory");
			this.newJdbcConnSpecMethod = wsrraFactoryClass.getMethod("createJDBCConnectionSpec");
			this.wsDataSourceGetConnectionMethod =
					this.wsDataSourceClass.getMethod("getConnection", jdbcConnSpecClass);
			this.setTransactionIsolationMethod =
					jdbcConnSpecClass.getMethod("setTransactionIsolation", int.class);
			this.setReadOnlyMethod = jdbcConnSpecClass.getMethod("setReadOnly", Boolean.class);
			this.setUserNameMethod = jdbcConnSpecClass.getMethod("setUserName", String.class);
			this.setPasswordMethod = jdbcConnSpecClass.getMethod("setPassword", String.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebSphereDataSourceAdapter because WebSphere API classes are not available: " + ex);
		}
	}

	/**
	 * 检查指定的'targetDataSource'是否是WebSphere WSDataSource.
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		if (!this.wsDataSourceClass.isInstance(getTargetDataSource())) {
			throw new IllegalStateException(
					"Specified 'targetDataSource' is not a WebSphere WSDataSource: " + getTargetDataSource());
		}
	}


	/**
	 * 为当前设置构建WebSphere JDBCConnectionSpec对象并调用{@code WSDataSource.getConnection(JDBCConnectionSpec)}.
	 */
	@Override
	protected Connection doGetConnection(String username, String password) throws SQLException {
		// 使用当前隔离级别值和只读标志创建JDBCConnectionSpec.
		Object connSpec = createConnectionSpec(
				getCurrentIsolationLevel(), getCurrentReadOnlyFlag(), username, password);
		if (logger.isDebugEnabled()) {
			logger.debug("Obtaining JDBC Connection from WebSphere DataSource [" +
					getTargetDataSource() + "], using ConnectionSpec [" + connSpec + "]");
		}
		// 通过调用WSDataSource.getConnection(JDBCConnectionSpec)创建连接
		return (Connection) ReflectionUtils.invokeJdbcMethod(
				this.wsDataSourceGetConnectionMethod, getTargetDataSource(), connSpec);
	}

	/**
	 * 为给定的特性创建WebSphere {@code JDBCConnectionSpec}对象.
	 * <p>默认实现使用反射来应用给定的设置.
	 * 可以在子类中重写以自定义JDBCConnectionSpec对象
	 * (<a href="http://publib.boulder.ibm.com/infocenter/wasinfo/v6r0/topic/com.ibm.websphere.javadoc.doc/public_html/api/com/ibm/websphere/rsadapter/JDBCConnectionSpec.html">JDBCConnectionSpec javadoc</a>;
	 * <a href="http://www.ibm.com/developerworks/websphere/library/techarticles/0404_tang/0404_tang.html">IBM developerWorks article</a>).
	 * 
	 * @param isolationLevel 要应用的隔离级别 (或{@code null})
	 * @param readOnlyFlag 要应用的只读标志 (或{@code null})
	 * @param username 要应用的用户名 ({@code null}或空指示默认值)
	 * @param password 要应用的密码 (可能是{@code null}或空)
	 * 
	 * @throws SQLException 如果由JDBCConnectionSpec API方法抛出
	 */
	protected Object createConnectionSpec(
			Integer isolationLevel, Boolean readOnlyFlag, String username, String password) throws SQLException {

		Object connSpec = ReflectionUtils.invokeJdbcMethod(this.newJdbcConnSpecMethod, null);
		if (isolationLevel != null) {
			ReflectionUtils.invokeJdbcMethod(this.setTransactionIsolationMethod, connSpec, isolationLevel);
		}
		if (readOnlyFlag != null) {
			ReflectionUtils.invokeJdbcMethod(this.setReadOnlyMethod, connSpec, readOnlyFlag);
		}
		// 如果用户名为空, 只需让目标DataSource使用其默认凭据即可.
		if (StringUtils.hasLength(username)) {
			ReflectionUtils.invokeJdbcMethod(this.setUserNameMethod, connSpec, username);
			ReflectionUtils.invokeJdbcMethod(this.setPasswordMethod, connSpec, password);
		}
		return connSpec;
	}

}
