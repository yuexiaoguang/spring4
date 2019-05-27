package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Constants;

/**
 * 目标DataSource的代理, 延迟获取实际的JDBC连接, i.e. 直到第一次创建Statement.
 * 一旦获取实际连接, 将保留连接初始化属性(如自动提交模式, 事务隔离和只读模式), 并将其应用于实际的JDBC连接.
 * 因此, 如果未创建任何语句, 则将忽略提交和回滚调用.
 *
 * <p>除非确实需要, 否则此DataSource代理允许避免从池中获取JDBC连接.
 * JDBC事务控制可以在不从池中获取连接, 或不与数据库通信的情况下进行; 这将在第一次创建JDBC语句时延迟地完成.
 *
 * <p><b>如果同时配置LazyConnectionDataSourceProxy和TransactionAwareDataSourceProxy, 请确保后者是最外层的DataSource.</b>
 * 在这种情况下, 数据访问代码将与事务感知DataSource进行通信, 而DataSource将与LazyConnectionDataSourceProxy一起使用.
 *
 * <p>延迟获取物理JDBC连接在通用事务划分环境中特别有用.
 * 它允许在可能执行数据访问的所有方法上划分事务, 而不会在没有实际数据访问的情况下造成性能损失.
 *
 * <p>此DataSource代理为您提供类似于JTA和事务性JNDI DataSource (由Java EE服务器提供)的行为,
 * 即使使用本地事务策略, 例如DataSourceTransactionManager 或 HibernateTransactionManager.
 * 它没有将Spring的JtaTransactionManager作为事务策略增加值.
 *
 * <p>对于使用Hibernate的只读操作, 建议使用延迟获取JDBC连接, 特别是如果在二级缓存中解析结果的可能性很高.
 * 这样就完全不需要与数据库通信以进行这种只读操作.
 * 将获得与非事务性读取相同的效果, 但延迟获取JDBC Connection允许仍然执行事务中的读取.
 *
 * <p><b>NOTE:</b> 此DataSource代理需要返回包装的Connection (其实现了{@link ConnectionProxy}接口),
 * 以处理实际JDBC连接的延迟获取.
 * 因此, 返回的Connection无法强制转换为本机JDBC连接类型(如OracleConnection), 或连接池实现类型.
 * 使用相应的{@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor}
 * 或JDBC 4的{@link Connection#unwrap}来检索本机JDBC连接.
 */
public class LazyConnectionDataSourceProxy extends DelegatingDataSource {

	/** TransactionDefinition的常量实例 */
	private static final Constants constants = new Constants(Connection.class);

	private static final Log logger = LogFactory.getLog(LazyConnectionDataSourceProxy.class);

	private Boolean defaultAutoCommit;

	private Integer defaultTransactionIsolation;

	public LazyConnectionDataSourceProxy() {
	}

	/**
	 * @param targetDataSource 目标DataSource
	 */
	public LazyConnectionDataSourceProxy(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
		afterPropertiesSet();
	}


	/**
	 * 设置尚未获取目标连接时, 要公开的默认自动提交模式 (-> 实际JDBC连接默认值未知).
	 * <p>如果未指定, 则通过在启动时检查目标连接来确定默认值.
	 * 如果该检查失败, 则在首次访问Connection时延迟确定默认值.
	 */
	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	/**
	 * 设置尚未获取目标连接时, 要公开的默认自动提交模式 (-> 实际JDBC连接默认值未知).
	 * <p>此属性接受{@link java.sql.Connection}接口中定义的int常量值(e.g. 8);
	 * 它主要用于程序化使用.
	 * 请考虑使用"defaultTransactionIsolationName"属性按名称设置值 (e.g. "TRANSACTION_SERIALIZABLE").
	 * <p>如果未指定, 则通过在启动时检查目标连接来确定默认值.
	 * 如果该检查失败, 则在首次访问Connection时延迟确定默认值.
	 */
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	/**
	 * 在{@link java.sql.Connection}中通过相应常量的名称, 设置默认事务隔离级别, e.g. "TRANSACTION_SERIALIZABLE".
	 * 
	 * @param constantName 常量的名称
	 */
	public void setDefaultTransactionIsolationName(String constantName) {
		setDefaultTransactionIsolation(constants.asNumber(constantName).intValue());
	}


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// 如果可能, 通过目标DataSource的Connection确定默认的自动提交和事务隔离.
		if (this.defaultAutoCommit == null || this.defaultTransactionIsolation == null) {
			try {
				Connection con = getTargetDataSource().getConnection();
				try {
					checkDefaultConnectionProperties(con);
				}
				finally {
					con.close();
				}
			}
			catch (SQLException ex) {
				logger.warn("Could not retrieve default auto-commit and transaction isolation settings", ex);
			}
		}
	}

	/**
	 * 检查默认连接属性 (自动提交, 事务隔离), 使它们能够正确公开它们, 而无需从目标DataSource获取实际的JDBC连接.
	 * <p>这将在启动时调用一次, 但也会在每次检索目标Connection时调用.
	 * 如果启动时检查失败 (因为数据库已关闭), 将延迟检索这些设置.
	 * 
	 * @param con 用于检查的连接
	 * 
	 * @throws SQLException 如果由Connection方法抛出
	 */
	protected synchronized void checkDefaultConnectionProperties(Connection con) throws SQLException {
		if (this.defaultAutoCommit == null) {
			this.defaultAutoCommit = con.getAutoCommit();
		}
		if (this.defaultTransactionIsolation == null) {
			this.defaultTransactionIsolation = con.getTransactionIsolation();
		}
	}

	/**
	 * 公开默认的自动提交值.
	 */
	protected Boolean defaultAutoCommit() {
		return this.defaultAutoCommit;
	}

	/**
	 * 公开默认的事务隔离值.
	 */
	protected Integer defaultTransactionIsolation() {
		return this.defaultTransactionIsolation;
	}


	/**
	 * 返回一个Connection句柄, 当被要求使用Statement (或PreparedStatement, 或CallableStatement)时, 它会延迟地获取实际的JDBC Connection.
	 * <p>返回的Connection句柄, 实现ConnectionProxy接口, 允许检索底层目标Connection.
	 * 
	 * @return 延迟的Connection句柄
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler());
	}

	/**
	 * 返回一个Connection句柄, 当被要求使用Statement (或PreparedStatement, 或CallableStatement)时, 它会延迟地获取实际的JDBC Connection.
	 * <p>返回的Connection句柄, 实现ConnectionProxy接口, 允许检索底层目标Connection.
	 * 
	 * @param username 每个连接用户名
	 * @param password 每个连接密码
	 * 
	 * @return 延迟的Connection句柄
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new LazyConnectionInvocationHandler(username, password));
	}


	/**
	 * 调用处理器, 它在第一次创建Statement之前推迟获取实际的JDBC连接.
	 */
	private class LazyConnectionInvocationHandler implements InvocationHandler {

		private String username;

		private String password;

		private Boolean readOnly = Boolean.FALSE;

		private Integer transactionIsolation;

		private Boolean autoCommit;

		private boolean closed = false;

		private Connection target;

		public LazyConnectionInvocationHandler() {
			this.autoCommit = defaultAutoCommit();
			this.transactionIsolation = defaultTransactionIsolation();
		}

		public LazyConnectionInvocationHandler(String username, String password) {
			this();
			this.username = username;
			this.password = password;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自ConnectionProxy接口的调用...

			if (method.getName().equals("equals")) {
				// 必须避免为"equals"获取目标连接.
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 必须避免为"hashCode"获取目标Connection, 并且即使已经获取目标Connection, 也必须返回相同的哈希码:
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("unwrap")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isWrapperFor")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return true;
				}
			}
			else if (method.getName().equals("getTargetConnection")) {
				// 处理getTargetConnection 方法: 返回底层连接.
				return getTargetConnection(method);
			}

			if (!hasTargetConnection()) {
				// 尚未保留物理目标连接 -> 解析事务划分方法, 直到绝对必要时才获取物理JDBC连接.

				if (method.getName().equals("toString")) {
					return "Lazy Connection proxy for target DataSource [" + getTargetDataSource() + "]";
				}
				else if (method.getName().equals("isReadOnly")) {
					return this.readOnly;
				}
				else if (method.getName().equals("setReadOnly")) {
					this.readOnly = (Boolean) args[0];
					return null;
				}
				else if (method.getName().equals("getTransactionIsolation")) {
					if (this.transactionIsolation != null) {
						return this.transactionIsolation;
					}
					// 否则获取实际的Connection并检查那里, 因为没有指定默认值.
				}
				else if (method.getName().equals("setTransactionIsolation")) {
					this.transactionIsolation = (Integer) args[0];
					return null;
				}
				else if (method.getName().equals("getAutoCommit")) {
					if (this.autoCommit != null) {
						return this.autoCommit;
					}
					// 否则获取实际的Connection并检查那里, 因为没有指定默认值.
				}
				else if (method.getName().equals("setAutoCommit")) {
					this.autoCommit = (Boolean) args[0];
					return null;
				}
				else if (method.getName().equals("commit")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("rollback")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("getWarnings")) {
					return null;
				}
				else if (method.getName().equals("clearWarnings")) {
					return null;
				}
				else if (method.getName().equals("close")) {
					// Ignore: no target connection yet.
					this.closed = true;
					return null;
				}
				else if (method.getName().equals("isClosed")) {
					return this.closed;
				}
				else if (this.closed) {
					// 连接代理已关闭, 没有获取物理JDBC连接: 抛出相应的SQLException.
					throw new SQLException("Illegal operation: connection is closed");
				}
			}

			// 已获取目标连接, 或当前操作所需的目标连接 -> 在目标连接上的调用方法.
			try {
				return method.invoke(getTargetConnection(method), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * 返回代理当前是否持有目标Connection.
		 */
		private boolean hasTargetConnection() {
			return (this.target != null);
		}

		/**
		 * 返回目标Connection, 获取它并在必要时初始化它.
		 */
		private Connection getTargetConnection(Method operation) throws SQLException {
			if (this.target == null) {
				// 没有目标连接 -> 获取一个.
				if (logger.isDebugEnabled()) {
					logger.debug("Connecting to database for operation '" + operation.getName() + "'");
				}

				// 从DataSource获取物理连接.
				this.target = (this.username != null) ?
						getTargetDataSource().getConnection(this.username, this.password) :
						getTargetDataSource().getConnection();

				// 如果仍然缺少默认连接属性, 立即检查它们.
				checkDefaultConnectionProperties(this.target);

				// 应用保留的事务设置.
				if (this.readOnly) {
					try {
						this.target.setReadOnly(true);
					}
					catch (Exception ex) {
						// "不支持只读" -> 忽略, 反正只是一个提示
						logger.debug("Could not set JDBC Connection read-only", ex);
					}
				}
				if (this.transactionIsolation != null &&
						!this.transactionIsolation.equals(defaultTransactionIsolation())) {
					this.target.setTransactionIsolation(this.transactionIsolation);
				}
				if (this.autoCommit != null && this.autoCommit != this.target.getAutoCommit()) {
					this.target.setAutoCommit(this.autoCommit);
				}
			}

			else {
				// 已经保存目标连接 -> 返回它.
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing database connection for operation '" + operation.getName() + "'");
				}
			}

			return this.target;
		}
	}
}
