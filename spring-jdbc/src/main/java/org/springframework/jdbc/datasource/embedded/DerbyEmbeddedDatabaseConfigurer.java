package org.springframework.jdbc.datasource.embedded;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDriver;

/**
 * 用于Apache Derby数据库的{@link EmbeddedDatabaseConfigurer}.
 *
 * <p>调用{@link #getInstance()}来获取此类的单例实例.
 */
final class DerbyEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final String URL_TEMPLATE = "jdbc:derby:memory:%s;%s";

	private static DerbyEmbeddedDatabaseConfigurer instance;


	/**
	 * 获取单例{@link DerbyEmbeddedDatabaseConfigurer}实例.
	 * 
	 * @return 配置器实例
	 */
	public static synchronized DerbyEmbeddedDatabaseConfigurer getInstance() {
		if (instance == null) {
			// 禁用日志文件
			System.setProperty("derby.stream.error.method",
					OutputStreamFactory.class.getName() + ".getNoopOutputStream");
			instance = new DerbyEmbeddedDatabaseConfigurer();
		}
		return instance;
	}


	private DerbyEmbeddedDatabaseConfigurer() {
	}

	@Override
	public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
		properties.setDriverClass(EmbeddedDriver.class);
		properties.setUrl(String.format(URL_TEMPLATE, databaseName, "create=true"));
		properties.setUsername("sa");
		properties.setPassword("");
	}

	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		try {
			new EmbeddedDriver().connect(
					String.format(URL_TEMPLATE, databaseName, "drop=true"), new Properties());
		}
		catch (SQLException ex) {
			// 表示成功关闭的错误代码
			if (!"08006".equals(ex.getSQLState())) {
				LogFactory.getLog(getClass()).warn("Could not shut down embedded Derby database", ex);
			}
		}
	}

}
