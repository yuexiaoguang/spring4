package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * 由JDBC连接的句柄实现的简单接口.
 * 例如, 由JpaDialect和JdoDialect使用.
 */
public interface ConnectionHandle {

	/**
	 * 获取此句柄引用的JDBC连接.
	 */
	Connection getConnection();

	/**
	 * 释放此句柄引用的JDBC连接.
	 * 
	 * @param con 要释放的JDBC连接
	 */
	void releaseConnection(Connection con);

}
