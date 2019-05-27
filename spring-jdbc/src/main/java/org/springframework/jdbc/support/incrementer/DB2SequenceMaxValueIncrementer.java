package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索DB2 LUW上给定序列的下一个值(适用于Linux, Unix 和 Windows).
 *
 * <p>Thanks to Mark MacMahon for the suggestion!
 *
 * @deprecated in favor of the specifically named {@link Db2LuwMaxValueIncrementer}
 */
@Deprecated
public class DB2SequenceMaxValueIncrementer extends Db2LuwMaxValueIncrementer {

	public DB2SequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public DB2SequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}

}
