package org.springframework.jdbc.datasource.init;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 用于在初始化期间{@linkplain #setDatabasePopulator 设置}数据库,
 * 并在销毁期间{@link #setDatabaseCleaner 清理}数据库.
 */
public class DataSourceInitializer implements InitializingBean, DisposableBean {

	private DataSource dataSource;

	private DatabasePopulator databasePopulator;

	private DatabasePopulator databaseCleaner;

	private boolean enabled = true;


	/**
	 * 初始化此组件时要填充的以及关闭此组件时要清理的数据库的{@link DataSource}.
	 * <p>此属性是强制性的, 不提供默认值.
	 * 
	 * @param dataSource the DataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置在bean初始化阶段执行的{@link DatabasePopulator}.
	 * 
	 * @param databasePopulator 初始化期间要使用的{@code DatabasePopulator}
	 */
	public void setDatabasePopulator(DatabasePopulator databasePopulator) {
		this.databasePopulator = databasePopulator;
	}

	/**
	 * 设置在bean销毁阶段执行的{@link DatabasePopulator}, 清理数据库并使其处于已知状态以供其他人使用.
	 * 
	 * @param databaseCleaner 在销毁期间使用的{@code DatabasePopulator}
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	/**
	 * 显式启用或禁用{@linkplain #setDatabasePopulator  数据库填充器}和{{@linkplain #setDatabaseCleaner 数据库清理器}.
	 * 
	 * @param enabled {@code true}如果应分别在启动和关闭时调用数据库填充器和数据库清理器
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	/**
	 * 使用{@linkplain #setDatabasePopulator 数据库填充器}来设置数据库.
	 */
	@Override
	public void afterPropertiesSet() {
		execute(this.databasePopulator);
	}

	/**
	 * 使用{@linkplain #setDatabaseCleaner 数据库清理器}清理数据库.
	 */
	@Override
	public void destroy() {
		execute(this.databaseCleaner);
	}

	private void execute(DatabasePopulator populator) {
		Assert.state(this.dataSource != null, "DataSource must be set");
		if (this.enabled && populator != null) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}
}
