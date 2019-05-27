package org.springframework.jdbc.datasource.lookup;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

/**
 * 基于Spring {@link BeanFactory}的{@link DataSourceLookup}实现.
 *
 * <p>将查找由bean名称标识的Spring托管bean, 期望它们的类型为{@code javax.sql.DataSource}.
 */
public class BeanFactoryDataSourceLookup implements DataSourceLookup, BeanFactoryAware {

	private BeanFactory beanFactory;


	/**
	 * <p>要访问的BeanFactory必须通过{@code setBeanFactory}设置.
	 */
	public BeanFactoryDataSourceLookup() {
	}

	/**
	 * <p>如果此对象是由Spring IoC容器创建的, 则使用此构造函数是多余的,
	 * 因为提供的{@link BeanFactory}将由创建它的{@link BeanFactory}替换 (c.f. {@link BeanFactoryAware}约定).
	 * 因此, 如果您在Spring IoC容器的上下文之外使用此类, 则仅使用此构造函数.
	 * 
	 * @param beanFactory 用于查找{@link DataSource DataSources}的bean工厂
	 */
	public BeanFactoryDataSourceLookup(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(dataSourceName, DataSource.class);
		}
		catch (BeansException ex) {
			throw new DataSourceLookupFailureException(
					"Failed to look up DataSource bean with name '" + dataSourceName + "'", ex);
		}
	}

}
