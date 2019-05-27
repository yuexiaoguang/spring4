package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.Assert;

/**
 * 执行{@link DatabasePopulator}的实用方法.
 */
public abstract class DatabasePopulatorUtils {

	/**
	 * 针对给定的{@link DataSource}执行给定的{@link DatabasePopulator}.
	 * 
	 * @param populator 要执行的{@code DatabasePopulator}
	 * @param dataSource 要执行的{@code DataSource}
	 * 
	 * @throws DataAccessException 如果发生错误, 特别是{@link ScriptException}
	 */
	public static void execute(DatabasePopulator populator, DataSource dataSource) throws DataAccessException {
		Assert.notNull(populator, "DatabasePopulator must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		try {
			Connection connection = DataSourceUtils.getConnection(dataSource);
			try {
				populator.populate(connection);
			}
			finally {
				DataSourceUtils.releaseConnection(connection, dataSource);
			}
		}
		catch (Throwable ex) {
			if (ex instanceof ScriptException) {
				throw (ScriptException) ex;
			}
			throw new UncategorizedScriptException("Failed to execute database script", ex);
		}
	}

}
