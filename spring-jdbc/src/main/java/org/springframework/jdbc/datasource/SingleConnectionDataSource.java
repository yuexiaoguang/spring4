package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link SmartDataSource}的实现, 它包装了一个在使用后未关闭的JDBC连接.
 * 显然, 这不是多线程的.
 *
 * <p>请注意, 在关闭时, 有人应该通过{@code close()}方法关闭底层连接.
 * 如果它是SmartDataSource感知的, 客户端代码将永远不会在Connection句柄上调用close (e.g. 使用{@code DataSourceUtils.releaseConnection}).
 *
 * <p>如果客户端代码在假设池化连接时调用{@code close()}, 就像使用持久性工具时一样, 将"suppressClose"设置为"true".
 * 这将返回一个关闭抑制代理, 而不是物理连接.
 *
 * <p>这主要用于测试.
 * 例如, 它可以在应用程序服务器外部轻松进行测试, 用于期望在DataSource上运行的代码.
 * 与{@link DriverManagerDataSource}相反, 它始终重用相同的Connection, 避免过多创建物理连接.
 */
public class SingleConnectionDataSource extends DriverManagerDataSource implements SmartDataSource, DisposableBean {

	/** 创建一个关闭抑制代理? */
	private boolean suppressClose;

	/** 覆盖自动提交状态? */
	private Boolean autoCommit;

	/** 包装的Connection */
	private Connection target;

	/** 代理Connection */
	private Connection connection;

	/** 共享连接的同步监视器 */
	private final Object connectionMonitor = new Object();


	/**
	 * bean风格配置的构造函数.
	 */
	public SingleConnectionDataSource() {
	}

	/**
	 * 使用给定的标准DriverManager参数创建新的SingleConnectionDataSource.
	 * 
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param username 用于访问DriverManager的JDBC用户名
	 * @param password 用于访问DriverManager的JDBC密码
	 * @param suppressClose 如果返回的Connection应该是一个关闭抑制代理, 或物理连接
	 */
	public SingleConnectionDataSource(String url, String username, String password, boolean suppressClose) {
		super(url, username, password);
		this.suppressClose = suppressClose;
	}

	/**
	 * 使用给定的标准DriverManager参数创建新的SingleConnectionDataSource.
	 * 
	 * @param url 用于访问DriverManager的JDBC URL
	 * @param suppressClose 如果返回的Connection应该是一个关闭抑制代理, 或物理连接
	 */
	public SingleConnectionDataSource(String url, boolean suppressClose) {
		super(url);
		this.suppressClose = suppressClose;
	}

	/**
	 * 使用给定的Connection创建一个新的SingleConnectionDataSource.
	 * 
	 * @param target 底层目标Connection
	 * @param suppressClose 如果Connection应该用一个禁止{@code close()}调用的Connection包装
	 * (允许在期望池连接, 但不知道SmartDataSource接口的应用程序中使用正常的{@code close()})
	 */
	public SingleConnectionDataSource(Connection target, boolean suppressClose) {
		Assert.notNull(target, "Connection must not be null");
		this.target = target;
		this.suppressClose = suppressClose;
		this.connection = (suppressClose ? getCloseSuppressingConnectionProxy(target) : target);
	}


	/**
	 * 设置返回的Connection是否应该是一个关闭抑制代理或物理连接.
	 */
	public void setSuppressClose(boolean suppressClose) {
		this.suppressClose = suppressClose;
	}

	/**
	 * 返回返回的Connection是否应该是一个关闭抑制代理或物理连接.
	 */
	protected boolean isSuppressClose() {
		return this.suppressClose;
	}

	/**
	 * 设置是否应覆盖返回的Connection的"autoCommit"设置.
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = (autoCommit);
	}

	/**
	 * 返回是否应覆盖返回的Connection的"autoCommit"设置.
	 * 
	 * @return "autoCommit"值, 或{@code null}
	 */
	protected Boolean getAutoCommitValue() {
		return this.autoCommit;
	}


	@Override
	public Connection getConnection() throws SQLException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				// 没有底层Connection -> 通过DriverManager延迟初始化.
				initConnection();
			}
			if (this.connection.isClosed()) {
				throw new SQLException(
						"Connection was closed in SingleConnectionDataSource. Check that user code checks " +
						"shouldClose() before closing Connections, or set 'suppressClose' to 'true'");
			}
			return this.connection;
		}
	}

	/**
	 * 使用单个Connection指定自定义用户名和密码没有意义.
	 * 如果给出相同的用户名和密码, 则返回单个Connection; 否则抛出一个SQLException.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		if (ObjectUtils.nullSafeEquals(username, getUsername()) &&
				ObjectUtils.nullSafeEquals(password, getPassword())) {
			return getConnection();
		}
		else {
			throw new SQLException("SingleConnectionDataSource does not support custom username and password");
		}
	}

	/**
	 * 这是一个连接: 返回到"pool"时不要关闭它.
	 */
	@Override
	public boolean shouldClose(Connection con) {
		synchronized (this.connectionMonitor) {
			return (con != this.connection && con != this.target);
		}
	}

	/**
	 * 关闭底层连接.
	 * 此DataSource的提供器需要关注正确的关闭.
	 * <p>当这个bean实现DisposableBean时, bean工厂会在破坏其缓存的单例时自动调用它.
	 */
	@Override
	public void destroy() {
		synchronized (this.connectionMonitor) {
			closeConnection();
		}
	}


	/**
	 * 通过DriverManager初始化底层连接.
	 */
	public void initConnection() throws SQLException {
		if (getUrl() == null) {
			throw new IllegalStateException("'url' property is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = getConnectionFromDriver(getUsername(), getPassword());
			prepareConnection(this.target);
			if (logger.isInfoEnabled()) {
				logger.info("Established shared JDBC Connection: " + this.target);
			}
			this.connection = (isSuppressClose() ? getCloseSuppressingConnectionProxy(this.target) : this.target);
		}
	}

	/**
	 * 重置底层共享连接, 以便在下次访问时重新初始化.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			closeConnection();
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * 在给定连接公开之前准备它.
	 * <p>如有必要, 默认实现应用自动提交标志.
	 * 可以在子类中重写.
	 * 
	 * @param con 要准备的Connection
	 */
	protected void prepareConnection(Connection con) throws SQLException {
		Boolean autoCommit = getAutoCommitValue();
		if (autoCommit != null && con.getAutoCommit() != autoCommit) {
			con.setAutoCommit(autoCommit);
		}
	}

	/**
	 * 关闭底层共享的Connection.
	 */
	private void closeConnection() {
		if (this.target != null) {
			try {
				this.target.close();
			}
			catch (Throwable ex) {
				logger.warn("Could not close shared JDBC Connection", ex);
			}
		}
	}

	/**
	 * 使用代理来包装给定的Connection, 将每个方法调用委托给它, 但禁止关闭调用.
	 * 
	 * @param target 要包装的原始连接
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * 调用JDBC连接的调用处理器.
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		public CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自ConnectionProxy接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
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
			else if (method.getName().equals("close")) {
				// 处理关闭方法: 不要通过调用.
				return null;
			}
			else if (method.getName().equals("isClosed")) {
				return false;
			}
			else if (method.getName().equals("getTargetConnection")) {
				// 处理getTargetConnection方法: 返回底层Connection.
				return this.target;
			}

			// 在目标Connection上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}
}
