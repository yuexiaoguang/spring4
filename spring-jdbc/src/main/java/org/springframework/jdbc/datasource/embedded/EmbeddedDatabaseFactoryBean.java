package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;

/**
 * {@link EmbeddedDatabaseFactory}的子类, 它实现{@link FactoryBean}以注册为Spring bean.
 * 返回实际的{@link DataSource}, 它提供与嵌入式数据库到Spring的连接.
 *
 * <p>返回目标{@link DataSource}, 而不是{@link EmbeddedDatabase}代理,
 * 因为{@link FactoryBean}将管理嵌入式数据库实例的初始化和销毁​​生命周期.
 *
 * <p>在管理Spring容器关闭时, 实现{@link DisposableBean}以关闭嵌入式数据库.
 */
public class EmbeddedDatabaseFactoryBean extends EmbeddedDatabaseFactory
		implements FactoryBean<DataSource>, InitializingBean, DisposableBean {

	private DatabasePopulator databaseCleaner;


	/**
	 * 设置要在bean销毁回调中运行的脚本执行, 清理数据库, 并使其处于已知状态以供其他人使用.
	 * 
	 * @param databaseCleaner 在destroy上运行的数据库脚本执行器
	 */
	public void setDatabaseCleaner(DatabasePopulator databaseCleaner) {
		this.databaseCleaner = databaseCleaner;
	}

	@Override
	public void afterPropertiesSet() {
		initDatabase();
	}


	@Override
	public DataSource getObject() {
		return getDataSource();
	}

	@Override
	public Class<? extends DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.databaseCleaner != null) {
			DatabasePopulatorUtils.execute(this.databaseCleaner, getDataSource());
		}
		shutdownDatabase();
	}

}
