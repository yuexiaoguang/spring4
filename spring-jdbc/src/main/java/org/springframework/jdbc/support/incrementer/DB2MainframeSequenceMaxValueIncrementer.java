package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 的给定序列的下一个值(z/OS, DB2/390, DB2/400)的给定序列的下一个值.
 *
 * <p>Thanks to Jens Eickmeyer for the suggestion!
 *
 * @deprecated in favor of the differently named {@link Db2MainframeMaxValueIncrementer}
 */
@Deprecated
public class DB2MainframeSequenceMaxValueIncrementer extends AbstractSequenceMaxValueIncrementer {

	public DB2MainframeSequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public DB2MainframeSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}


	@Override
	protected String getSequenceQuery() {
		return "select next value for " + getIncrementerName() + " from sysibm.sysdummy1";
	}

}
