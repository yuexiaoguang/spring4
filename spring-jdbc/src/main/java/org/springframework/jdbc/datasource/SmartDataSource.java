package org.springframework.jdbc.datasource;

import java.sql.Connection;
import javax.sql.DataSource;

/**
 * {@code javax.sql.DataSource}接口的扩展, 由以未解包的方式返回JDBC Connection的特殊DataSource实现.
 *
 * <p>使用此接口的类可以查询操作后是否应关闭Connection.
 * Spring的DataSourceUtils和JdbcTemplate类自动执行这样的检查.
 */
public interface SmartDataSource extends DataSource {

	/**
	 * 是否应该关闭从此DataSource获取的Connection?
	 * <p>在调用{@code close()}之前, 使用来自SmartDataSource的Connection的代码应始终通过此方法执行检查.
	 * <p>请注意, 'jdbc.core'包中的JdbcTemplate类负责释放JDBC Connection, 释放它负责的的应用程序代码.
	 * 
	 * @param con 要检查的连接
	 * 
	 * @return 是否应关闭给定的连接
	 */
	boolean shouldClose(Connection con);

}
