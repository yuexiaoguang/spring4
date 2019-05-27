package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

import org.springframework.util.Assert;

/**
 * 使用自定义序列表中的列的{@link DataFieldMaxValueIncrementer}实现的抽象基类.
 * 子类需要在{@link #getNextKey()}实现中提供该表的特定处理.
 */
public abstract class AbstractColumnMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

	/** 此序列的列名称 */
	private String columnName;

	/** 缓存中缓冲的键的数量 */
	private int cacheSize = 1;


	public AbstractColumnMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 */
	public AbstractColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName);
		Assert.notNull(columnName, "Column name must not be null");
		this.columnName = columnName;
	}


	/**
	 * 设置序列表中列的名称.
	 */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	/**
	 * 返回序列表中列的名称.
	 */
	public String getColumnName() {
		return this.columnName;
	}

	/**
	 * 设置缓冲键的数量.
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * 返回缓冲键的数量.
	 */
	public int getCacheSize() {
		return this.cacheSize;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.columnName == null) {
			throw new IllegalArgumentException("Property 'columnName' is required");
		}
	}
}
