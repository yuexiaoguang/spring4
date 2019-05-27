package org.springframework.jdbc.datasource.embedded;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.util.Assert;

/**
 * 用于创建{@link EmbeddedDatabase}实例的工厂.
 *
 * <p>确保调用者已完全初始化并填充返回的数据库.
 *
 * <p>工厂可以配置如下:
 * <ul>
 * <li>调用{@link #generateUniqueDatabaseName}为数据库设置唯一的随机名称.
 * <li>调用{@link #setDatabaseName}为数据库设置显式名称.
 * <li>如果要使用其中一种受支持的类型, 调用{@link #setDatabaseType}来设置数据库类型.
 * <li>调用{@link #setDatabaseConfigurer}以配置对自定义嵌入式数据库类型的支持.
 * <li>调用{@link #setDatabasePopulator}来更改用于填充数据库的算法.
 * <li>调用{@link #setDataSourceFactory}来更改用于连接数据库的{@link DataSource}的类型.
 * </ul>
 *
 * <p>配置工厂后, 调用{@link #getDatabase()}以获取对{@link EmbeddedDatabase}实例的引用.
 */
public class EmbeddedDatabaseFactory {

	/**
	 * 嵌入式数据库的默认名称: {@value}
	 */
	public static final String DEFAULT_DATABASE_NAME = "testdb";

	private static final Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private boolean generateUniqueDatabaseName = false;

	private String databaseName = DEFAULT_DATABASE_NAME;

	private DataSourceFactory dataSourceFactory = new SimpleDriverDataSourceFactory();

	private EmbeddedDatabaseConfigurer databaseConfigurer;

	private DatabasePopulator databasePopulator;

	private DataSource dataSource;


	/**
	 * 设置{@code generateUniqueDatabaseName}标志, 以启用或禁用生成伪随机唯一ID以用作数据库名称.
	 * <p>将此标志设置为{@code true}会覆盖通过{@link #setDatabaseName}设置的任何显式名称.
	 */
	public void setGenerateUniqueDatabaseName(boolean generateUniqueDatabaseName) {
		this.generateUniqueDatabaseName = generateUniqueDatabaseName;
	}

	/**
	 * 设置数据库的名称.
	 * <p>默认{@value #DEFAULT_DATABASE_NAME}.
	 * <p>如果{@code generateUniqueDatabaseName}标志已设置为{@code true}, 将被覆盖.
	 * 
	 * @param databaseName 嵌入式数据库的名称
	 */
	public void setDatabaseName(String databaseName) {
		Assert.hasText(databaseName, "Database name is required");
		this.databaseName = databaseName;
	}

	/**
	 * 设置用于创建连接到嵌入式数据库的{@link DataSource}实例的工厂.
	 * <p>默认{@link SimpleDriverDataSourceFactory}.
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		Assert.notNull(dataSourceFactory, "DataSourceFactory is required");
		this.dataSourceFactory = dataSourceFactory;
	}

	/**
	 * 设置要使用的嵌入式数据库的类型.
	 * <p>如果要配置其中一种预先支持的类型, 调用此方法.
	 * <p>默认 HSQL.
	 * 
	 * @param type 数据库类型
	 */
	public void setDatabaseType(EmbeddedDatabaseType type) {
		this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(type);
	}

	/**
	 * 设置将用于配置嵌入式数据库实例的策略.
	 * <p>如果希望使用尚未支持的嵌入式数据库类型, 调用此方法.
	 */
	public void setDatabaseConfigurer(EmbeddedDatabaseConfigurer configurer) {
		this.databaseConfigurer = configurer;
	}

	/**
	 * 设置将用于初始化或填充嵌入式数据库的策略.
	 * <p>默认{@code null}.
	 */
	public void setDatabasePopulator(DatabasePopulator populator) {
		this.databasePopulator = populator;
	}

	/**
	 * 返回{@linkplain EmbeddedDatabase 嵌入式数据库}实例的工厂方法, 该实例也是{@link DataSource}.
	 */
	public EmbeddedDatabase getDatabase() {
		if (this.dataSource == null) {
			initDatabase();
		}
		return new EmbeddedDataSourceProxy(this.dataSource);
	}


	/**
	 * 初始化嵌入式数据库的挂钩.
	 * <p>如果{@code generateUniqueDatabaseName}标志已设置为{@code true},
	 * 则将使用自动生成的名称覆盖{@linkplain #setDatabaseName 数据库名称}的当前值.
	 * <p>子类可以调用此方法来强制初始化; 但是, 此方法只应调用一次.
	 * <p>调用此方法后, {@link #getDataSource()}返回提供与数据库连接的{@link DataSource}.
	 */
	protected void initDatabase() {
		if (this.generateUniqueDatabaseName) {
			setDatabaseName(UUID.randomUUID().toString());
		}

		// 首先创建嵌入式数据库
		if (this.databaseConfigurer == null) {
			this.databaseConfigurer = EmbeddedDatabaseConfigurerFactory.getConfigurer(EmbeddedDatabaseType.HSQL);
		}
		this.databaseConfigurer.configureConnectionProperties(
				this.dataSourceFactory.getConnectionProperties(), this.databaseName);
		this.dataSource = this.dataSourceFactory.getDataSource();

		if (logger.isInfoEnabled()) {
			if (this.dataSource instanceof SimpleDriverDataSource) {
				SimpleDriverDataSource simpleDriverDataSource = (SimpleDriverDataSource) this.dataSource;
				logger.info(String.format("Starting embedded database: url='%s', username='%s'",
						simpleDriverDataSource.getUrl(), simpleDriverDataSource.getUsername()));
			}
			else {
				logger.info(String.format("Starting embedded database '%s'", this.databaseName));
			}
		}

		// 现在填充数据库
		if (this.databasePopulator != null) {
			try {
				DatabasePopulatorUtils.execute(this.databasePopulator, this.dataSource);
			}
			catch (RuntimeException ex) {
				// 无法填充, 因此请将其保留为未初始化
				shutdownDatabase();
				throw ex;
			}
		}
	}

	/**
	 * 关闭嵌入式数据库的挂钩. 子类可以调用此方法来强制关闭.
	 * <p>调用后, {@link #getDataSource()}返回{@code null}.
	 * <p>如果没有初始化嵌入式数据库, 则不执行任何操作.
	 */
	protected void shutdownDatabase() {
		if (this.dataSource != null) {
			if (logger.isInfoEnabled()) {
				if (this.dataSource instanceof SimpleDriverDataSource) {
					logger.info(String.format("Shutting down embedded database: url='%s'",
						((SimpleDriverDataSource) this.dataSource).getUrl()));
				}
				else {
					logger.info(String.format("Shutting down embedded database '%s'", this.databaseName));
				}
			}
			this.databaseConfigurer.shutdown(this.dataSource, this.databaseName);
			this.dataSource = null;
		}
	}

	/**
	 * 获取提供与嵌入式数据库连接的{@link DataSource}的挂钩.
	 * <p>如果{@code DataSource}尚未初始化或数据库已关闭, 则返回{@code null}.
	 * 子类可以调用此方法直接访问{@code DataSource}实例.
	 */
	protected final DataSource getDataSource() {
		return this.dataSource;
	}


	private class EmbeddedDataSourceProxy implements EmbeddedDatabase {

		private final DataSource dataSource;

		public EmbeddedDataSourceProxy(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return this.dataSource.getConnection();
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			return this.dataSource.getConnection(username, password);
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return this.dataSource.getLogWriter();
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {
			this.dataSource.setLogWriter(out);
		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return this.dataSource.getLoginTimeout();
		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {
			this.dataSource.setLoginTimeout(seconds);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return this.dataSource.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return this.dataSource.isWrapperFor(iface);
		}

		// getParentLogger() is required for JDBC 4.1 compatibility
		@Override
		public Logger getParentLogger() {
			return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		}

		@Override
		public void shutdown() {
			shutdownDatabase();
		}
	}
}
