package org.springframework.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * JDBC {@link javax.sql.DataSource}实现, 它将所有调用委托给给定目标{@link javax.sql.DataSource}.
 *
 * <p>此类用于子类化, 子类仅覆盖那些不应简单地委托给目标DataSource的方法 (例如{@link #getConnection()}).
 */
public class DelegatingDataSource implements DataSource, InitializingBean {

	private DataSource targetDataSource;

	public DelegatingDataSource() {
	}

	/**
	 * @param targetDataSource 目标DataSource
	 */
	public DelegatingDataSource(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
	}


	/**
	 * 设置此DataSource应委派给的目标DataSource.
	 */
	public void setTargetDataSource(DataSource targetDataSource) {
		Assert.notNull(targetDataSource, "'targetDataSource' must not be null");
		this.targetDataSource = targetDataSource;
	}

	/**
	 * 返回此DataSource应委派给的目标DataSource.
	 */
	public DataSource getTargetDataSource() {
		return this.targetDataSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetDataSource() == null) {
			throw new IllegalArgumentException("Property 'targetDataSource' is required");
		}
	}


	@Override
	public Connection getConnection() throws SQLException {
		return getTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getTargetDataSource().getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return getTargetDataSource().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		getTargetDataSource().setLogWriter(out);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return getTargetDataSource().getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		getTargetDataSource().setLoginTimeout(seconds);
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.0's Wrapper interface
	//---------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return getTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || getTargetDataSource().isWrapperFor(iface));
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.1's getParentLogger method
	//---------------------------------------------------------------------

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
