package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索给定H2序列的下一个值.
 */
public class H2SequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public H2SequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public H2SequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select " + getIncrementerName() + ".nextval from dual";
	}
}
