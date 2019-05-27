package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索给定PostgreSQL序列的下一个值.
 *
 * <p>Thanks to Tomislav Urban for the suggestion!
 */
public class PostgresSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public PostgresSequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public PostgresSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select nextval('" + getIncrementerName() + "')";
	}

}
