package org.springframework.scheduling.quartz;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.quartz.SchedulerConfigException;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.SimpleSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Quartz的JobStoreCMT类的子类, 它委托给Spring管理的DataSource, 而不是使用Quartz管理的连接池.
 * 如果设置了SchedulerFactoryBean的"dataSource" 属性, 则将使用此JobStore.
 *
 * <p>支持事务和非事务的DataSource访问.
 * 使用非XA DataSource和本地Spring事务, 单个DataSource参数就足够了.
 * 对于XA DataSource和全局JTA事务, 应设置SchedulerFactoryBean的"nonTransactionalDataSource"属性,
 * 传入不参与全局事务的非XA DataSource.
 *
 * <p>此JobStore执行的操作将正确地参与任何类型的Spring管理事务, 因为它使用了知道当前事务的Spring的DataSourceUtils连接处理方法.
 *
 * <p>请注意, 所有影响持久性作业存储的Quartz Scheduler操作通常应在活动事务中执行, 因为它们假定获得正确的锁等.
 */
@SuppressWarnings("unchecked")  // due to a warning in Quartz 2.2's JobStoreCMT
public class LocalDataSourceJobStore extends JobStoreCMT {

	/**
	 * 用于Quartz的事务性ConnectionProvider的名称.
	 * 此提供程序将委托给本地Spring管理的DataSource.
	 */
	public static final String TX_DATA_SOURCE_PREFIX = "springTxDataSource.";

	/**
	 * 用于Quartz的非事务性ConnectionProvider的名称.
	 * 此提供器将委托给本地Spring管理的DataSource.
	 */
	public static final String NON_TX_DATA_SOURCE_PREFIX = "springNonTxDataSource.";


	private DataSource dataSource;


	@Override
	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
			throws SchedulerConfigException {

		// 绝对需要线程绑定的DataSource来初始化.
		this.dataSource = SchedulerFactoryBean.getConfigTimeDataSource();
		if (this.dataSource == null) {
			throw new SchedulerConfigException(
				"No local DataSource found for configuration - " +
				"'dataSource' property must be set on SchedulerFactoryBean");
		}

		// 配置Quartz的事务连接设置.
		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);

		// 为Quartz注册事务性ConnectionProvider.
		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// 返回事务连接.
						return DataSourceUtils.doGetConnection(dataSource);
					}
					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
					/* Quartz 2.2 initialize method */
					public void initialize() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);

		// 非事务性DataSource是可选的: 如果未明确指定, 则回退到默认DataSource.
		DataSource nonTxDataSource = SchedulerFactoryBean.getConfigTimeNonTransactionalDataSource();
		final DataSource nonTxDataSourceToUse = (nonTxDataSource != null ? nonTxDataSource : this.dataSource);

		// 为Quartz配置非事务性连接设置.
		setNonManagedTXDataSource(NON_TX_DATA_SOURCE_PREFIX + getInstanceName());

		// 为Quartz注册非事务性ConnectionProvider.
		DBConnectionManager.getInstance().addConnectionProvider(
				NON_TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						// 始终返回非事务性连接.
						return nonTxDataSourceToUse.getConnection();
					}
					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
					/* Quartz 2.2 initialize method */
					public void initialize() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);

		// 不, 如果HSQL是平台, 真的不想使用锁...
		try {
			String productName = JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName").toString();
			productName = JdbcUtils.commonDatabaseName(productName);
			if (productName != null && productName.toLowerCase().contains("hsql")) {
				setUseDBLocks(false);
				setLockHandler(new SimpleSemaphore());
			}
		}
		catch (MetaDataAccessException ex) {
			logWarnIfNonZero(1, "Could not detect database type. Assuming locks can be taken.");
		}

		super.initialize(loadHelper, signaler);

	}

	@Override
	protected void closeConnection(Connection con) {
		// 适用于事务性和非事务性连接.
		DataSourceUtils.releaseConnection(con, this.dataSource);
	}

}
