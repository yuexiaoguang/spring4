package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索DB2上大型机(z/OS, DB2/390, DB2/400)的给定序列的下一个值.
 *
 * <p>Thanks to Jens Eickmeyer for the suggestion!
 */
public class Db2MainframeMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public Db2MainframeMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public Db2MainframeMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select next value for " + getIncrementerName() + " from sysibm.sysdummy1";
	}

}
