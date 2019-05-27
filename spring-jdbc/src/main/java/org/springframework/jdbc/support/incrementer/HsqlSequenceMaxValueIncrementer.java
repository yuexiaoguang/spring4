package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索给定HSQL序列的下一个值.
 *
 * <p>Thanks to Guillaume Bilodeau for the suggestion!
 *
 * <p><b>NOTE:</b> 这是使用常规表来支持生成以前版本的HSQL中必需的唯一键的替代方法.
 */
public class HsqlSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public HsqlSequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public HsqlSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "call next value for " + getIncrementerName();
	}

}
