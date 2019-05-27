package org.springframework.jdbc.datasource.lookup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

/**
 * 抽象{@link javax.sql.DataSource}实现, 它根据查找键将{@link #getConnection()}调用路由到各种目标DataSource之一.
 * 后者通常 (但不一定) 通过一些线程绑定的事务上下文来确定.
 */
public abstract class AbstractRoutingDataSource extends AbstractDataSource implements InitializingBean {

	private Map<Object, Object> targetDataSources;

	private Object defaultTargetDataSource;

	private boolean lenientFallback = true;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	private Map<Object, DataSource> resolvedDataSources;

	private DataSource resolvedDefaultDataSource;


	/**
	 * 使用查找键作为键, 指定目标DataSource的映射.
	 * 映射的值可以是相应的{@link javax.sql.DataSource}实例, 也可以是数据源名称String
	 * (通过{@link #setDataSourceLookup DataSourceLookup}解析).
	 * <p>键可以是任意类型; 此类仅实现通用查找过程.
	 * 具体的键表示将由{@link #resolveSpecifiedLookupKey(Object)} 和 {@link #determineCurrentLookupKey()}处理.
	 */
	public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		this.targetDataSources = targetDataSources;
	}

	/**
	 * 指定默认目标DataSource.
	 * <p>映射的值可以是相应的{@link javax.sql.DataSource}实例, 也可以是数据源名称String
	 * (通过{@link #setDataSourceLookup DataSourceLookup}解析).
	 * <p>如果没有{@link #setTargetDataSources targetDataSources}与{@link #determineCurrentLookupKey()}当前查找键匹配,
	 * 则此DataSource将用作目标.
	 */
	public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
		this.defaultTargetDataSource = defaultTargetDataSource;
	}

	/**
	 * 如果找不到当前查找键的特定DataSource, 指定是否将回退到默认DataSource.
	 * <p>默认为"true", 接受查找键没有对应的目标DataSource映射中的相应条目 - 在这种情况下简单地回退到默认的DataSource.
	 * <p>如果希望仅在查找键为{@code null}时才应用回退, 请将此标志切换为"false".
	 * 没有DataSource条目的查找键将导致IllegalStateException.
	 */
	public void setLenientFallback(boolean lenientFallback) {
		this.lenientFallback = lenientFallback;
	}

	/**
	 * 设置DataSourceLookup实现以用于解析{@link #setTargetDataSources targetDataSources}映射中的数据源名称字符串.
	 * <p>默认值为{@link JndiDataSourceLookup}, 允许直接指定应用程序服务器DataSource的JNDI名称.
	 */
	public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}


	@Override
	public void afterPropertiesSet() {
		if (this.targetDataSources == null) {
			throw new IllegalArgumentException("Property 'targetDataSources' is required");
		}
		this.resolvedDataSources = new HashMap<Object, DataSource>(this.targetDataSources.size());
		for (Map.Entry<Object, Object> entry : this.targetDataSources.entrySet()) {
			Object lookupKey = resolveSpecifiedLookupKey(entry.getKey());
			DataSource dataSource = resolveSpecifiedDataSource(entry.getValue());
			this.resolvedDataSources.put(lookupKey, dataSource);
		}
		if (this.defaultTargetDataSource != null) {
			this.resolvedDefaultDataSource = resolveSpecifiedDataSource(this.defaultTargetDataSource);
		}
	}

	/**
	 * 将{@link #setTargetDataSources targetDataSources}映射中指定的查找键对象解析为实际查找键,
	 * 以用于与{@link #determineCurrentLookupKey() 当前查找键}匹配.
	 * <p>默认实现只是按原样返回给定的键.
	 * 
	 * @param lookupKey 用户指定的查找键对象
	 * 
	 * @return 匹配所需的查找键
	 */
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		return lookupKey;
	}

	/**
	 * 将指定的数据源对象解析为DataSource实例.
	 * <p>默认实现处理DataSource实例和数据源名称 (通过{@link #setDataSourceLookup DataSourceLookup}解析).
	 * 
	 * @param dataSource {@link #setTargetDataSources targetDataSources}映射中指定的数据源值对象
	 * 
	 * @return 已解析的DataSource (never {@code null})
	 * @throws IllegalArgumentException 如果值类型不受支持
	 */
	protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
		if (dataSource instanceof DataSource) {
			return (DataSource) dataSource;
		}
		else if (dataSource instanceof String) {
			return this.dataSourceLookup.getDataSource((String) dataSource);
		}
		else {
			throw new IllegalArgumentException(
					"Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
		}
	}


	@Override
	public Connection getConnection() throws SQLException {
		return determineTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return determineTargetDataSource().getConnection(username, password);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return determineTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || determineTargetDataSource().isWrapperFor(iface));
	}

	/**
	 * 检索当前目标DataSource.
	 * 确定{@link #determineCurrentLookupKey() 当前查找键}, 在{@link #setTargetDataSources targetDataSources}映射中执行查找,
	 * 如有必要, 可以回退到指定的{@link #setDefaultTargetDataSource 默认目标DataSource}.
	 */
	protected DataSource determineTargetDataSource() {
		Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
		Object lookupKey = determineCurrentLookupKey();
		DataSource dataSource = this.resolvedDataSources.get(lookupKey);
		if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
			dataSource = this.resolvedDefaultDataSource;
		}
		if (dataSource == null) {
			throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		return dataSource;
	}

	/**
	 * 确定当前查找键.
	 * 通常会实现此方法以检查线程绑定的事务上下文.
	 * <p>允许任意键. 返回的键需要匹配存储的查找键类型, 由{@link #resolveSpecifiedLookupKey}方法解析.
	 */
	protected abstract Object determineCurrentLookupKey();

}
