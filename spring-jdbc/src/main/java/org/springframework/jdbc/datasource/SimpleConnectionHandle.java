package org.springframework.jdbc.datasource;

import java.sql.Connection;

import org.springframework.util.Assert;

/**
 * {@link ConnectionHandle}接口的简单实现, 包含给定的JDBC Connection.
 */
public class SimpleConnectionHandle implements ConnectionHandle {

	private final Connection connection;


	/**
	 * @param connection JDBC Connection
	 */
	public SimpleConnectionHandle(Connection connection) {
		Assert.notNull(connection, "Connection must not be null");
		this.connection = connection;
	}

	/**
	 * 按原样返回指定的连接.
	 */
	@Override
	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * 这个实现是空的, 因为使用的是不必释放的标准Connection句柄.
	 */
	@Override
	public void releaseConnection(Connection con) {
	}


	@Override
	public String toString() {
		return "SimpleConnectionHandle: " + this.connection;
	}
}
