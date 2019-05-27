package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 使用等效的自动增量列递增给定HSQL表的最大值.
 * Note: 如果使用此类, 则HSQL键列<i>不应该</i>自动递增, 因为序列表将负责该任务.
 *
 * <p>序列保存在表中. 每个表应该有一个需要自动生成键的序列表.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100));
 * create table tab_sequence (value identity);
 * insert into tab_sequence values(0);</pre>
 *
 * 如果设置了"cacheSize", 则在不查询数据库的情况下提供中间值.
 * 如果服务器或您的应用程序停止, 或崩溃, 或回滚事务, 则永远不会提供未使用的值.
 * 编号中的最大hole大小因此是cacheSize的值.
 *
 * <p><b>NOTE:</b> HSQL现在支持序列, 应该考虑使用它们:
 * {@link HsqlSequenceMaxValueIncrementer}
 */
public class HsqlMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	public HsqlMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 */
	public HsqlMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " values(null)";
	}

	@Override
	protected String getIdentityStatement() {
		return "select max(identity()) from " + getIncrementerName();
	}

}
