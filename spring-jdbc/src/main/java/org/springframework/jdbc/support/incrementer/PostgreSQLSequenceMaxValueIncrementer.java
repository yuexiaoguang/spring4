package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 检索给定PostgreSQL序列的下一个值.
 *
 * <p>Thanks to Tomislav Urban for the suggestion!
 *
 * @deprecated in favor of the differently named {@link PostgresSequenceMaxValueIncrementer}
 */
@Deprecated
public class PostgreSQLSequenceMaxValueIncrementer extends PostgresSequenceMaxValueIncrementer {

	public PostgreSQLSequenceMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 */
	public PostgreSQLSequenceMaxValueIncrementer(DataSource dataSource, String incrementerName) {
		super(dataSource, incrementerName);
	}

}
