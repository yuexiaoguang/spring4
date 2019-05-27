package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * {@link DataFieldMaxValueIncrementer}的基本实现, 委托给一个返回{@code long}的{@link #getNextKey}模板方法.
 * 对String值使用long, 如果需要, 使用零填充.
 */
public abstract class AbstractDataFieldMaxValueIncrementer implements DataFieldMaxValueIncrementer, InitializingBean {

	private DataSource dataSource;

	/** 包含序列的序列/表的名称 */
	private String incrementerName;

	/** 应该用零预先设置的字符串结果的长度 */
	protected int paddingLength = 0;


	public AbstractDataFieldMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public AbstractDataFieldMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(incrementerName, "Incrementer name must not be null");
		this.dataSource = dataSource;
		this.incrementerName = incrementerName;
	}


	/**
	 * 设置从中检索值的数据源.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 返回从中检索值的数据源.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * 设置序列/表的名称.
	 */
	public void setIncrementerName(String incrementerName) {
		this.incrementerName = incrementerName;
	}

	/**
	 * 返回序列/表的名称.
	 */
	public String getIncrementerName() {
		return this.incrementerName;
	}

	/**
	 * 设置填充长度, i.e. 应该用零预先设置的字符串结果的长度.
	 */
	public void setPaddingLength(int paddingLength) {
		this.paddingLength = paddingLength;
	}

	/**
	 * 返回String值的填充长度.
	 */
	public int getPaddingLength() {
		return this.paddingLength;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.dataSource == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (this.incrementerName == null) {
			throw new IllegalArgumentException("Property 'incrementerName' is required");
		}
	}


	@Override
	public int nextIntValue() throws DataAccessException {
		return (int) getNextKey();
	}

	@Override
	public long nextLongValue() throws DataAccessException {
		return getNextKey();
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		String s = Long.toString(getNextKey());
		int len = s.length();
		if (len < this.paddingLength) {
			StringBuilder sb = new StringBuilder(this.paddingLength);
			for (int i = 0; i < this.paddingLength - len; i++) {
				sb.append('0');
			}
			sb.append(s);
			s = sb.toString();
		}
		return s;
	}


	/**
	 * 确定下一个要使用的键.
	 * 
	 * @return 要使用的键.
	 * 它最终将通过此类的公共具体方法以另一种格式转换.
	 */
	protected abstract long getNextKey();

}
