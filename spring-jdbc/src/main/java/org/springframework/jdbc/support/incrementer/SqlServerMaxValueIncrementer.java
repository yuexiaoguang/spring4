package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 使用等效的自动增量列递增给定SQL Server表的最大值.
 * Note: 如果使用此类, 则表键列<i>不应该</i>定义为IDENTITY列, 因为序列表将负责该任务.
 *
 * <p>此类旨在与Microsoft SQL Server一起使用.
 *
 * <p>序列保存在表中. 每个表应该有一个需要自动生成键的序列表.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100))
 * create table tab_sequence (id bigint identity)
 * insert into tab_sequence default values</pre>
 *
 * 如果设置了"cacheSize", 则在不查询数据库的情况下提供中间值.
 * 如果服务器或应用程序停止, 或崩溃, 或回滚事务, 则永远不会提供未使用的值.
 * 编号中的最大hole大小因此是cacheSize的值.
 *
 * <b>HINT:</b>由于Microsoft SQL Server支持JDBC 3.0 {@code getGeneratedKeys}方法,
 * 因此建议直接在表中使用IDENTITY列, 然后使用{@link org.springframework.jdbc.core.simple.SimpleJdbcInsert},
 * 或使用{@link org.springframework.jdbc.core.JdbcTemplate}的
 * {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}方法时,
 * 使用{@link org.springframework.jdbc.support.KeyHolder}.
 *
 * <p>Thanks to Preben Nilsson for the suggestion!
 */
public class SqlServerMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	public SqlServerMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 */
	public SqlServerMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " default values";
	}

	@Override
	protected String getIdentityStatement() {
		return "select @@identity";
	}
}
