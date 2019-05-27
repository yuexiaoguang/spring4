package org.springframework.jdbc.datasource.embedded;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link EmbeddedDatabaseConfigurer}实现的基类, 通过"SHUTDOWN"语句提供常见的关闭行为.
 */
abstract class AbstractEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.createStatement().execute("SHUTDOWN");
		}
		catch (SQLException ex) {
			logger.warn("Could not shut down embedded database", ex);
		}
		finally {
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex) {
					logger.debug("Could not close JDBC Connection on shutdown", ex);
				}
			}
		}
	}

}
