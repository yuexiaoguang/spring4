package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索DB2 LUW上给定序列的下一个值 (适用于Linux, Unix 和 Windows).
 *
 * <p>Thanks to Mark MacMahon for the suggestion!
 */
public class Db2LuwMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public Db2LuwMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public Db2LuwMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "values nextval for " + getIncrementerName();
	}

}
