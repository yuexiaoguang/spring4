package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * 由Connection代理实现的{@link java.sql.Connection}的子接口.
 * 允许访问底层目标Connection.
 *
 * <p>当需要强制转换为本机JDBC Connection时, 例如 Oracle's OracleConnection, 可以检查此接口.
 * Spring的{@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractorAdapter}自动检测这些代理,
 * 在委托给特定连接池的实际解包之前.
 * 或者, 所有这些连接也支持JDBC 4.0的 {@link Connection#unwrap}.
 */
public interface ConnectionProxy extends Connection {

	/**
	 * 返回此代理的目标Connection.
	 * <p>这通常是本机驱动程序Connection 或来自连接池的包装器.
	 * 
	 * @return 底层Connection (never {@code null})
	 */
	Connection getTargetConnection();

}
