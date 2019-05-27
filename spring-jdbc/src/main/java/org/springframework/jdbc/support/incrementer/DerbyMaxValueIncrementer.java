package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * 使用等效的自动增量列递增给定Derby表的最大值.
 * Note: 如果使用此类, 则Derby键列<i>不应该</i>定义为IDENTITY列, 因为序列表将执行该任务.
 *
 * <p>序列保存在表中. 每个表应该有一个需要自动生成键的序列表.
 *
 * <p>Derby需要一个额外的列用于插入, 因为无法在标识列中插入null并生成值.
 * 这是通过提供必须在序列表中创建的虚拟列的名称来解决的.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100));
 * create table tab_sequence (value int generated always as identity, dummy char(1));
 * insert into tab_sequence (dummy) values(null);</pre>
 *
 * 如果设置了"cacheSize", 则在不查询数据库的情况下提供中间值.
 * 如果服务器或应用程序停止, 或崩溃, 或回滚事务, 则永远不会提供未使用的值.
 * 编号中的最大hole大小因此是cacheSize的值.
 *
 * <b>HINT:</b> 由于Derby支持JDBC 3.0 {@code getGeneratedKeys}方法,
 * 因此建议直接在表中使用IDENTITY列, 然后在使用{@link org.springframework.jdbc.core.JdbcTemplate}的
 * {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}方法调用时,
 * 使用{@link org.springframework.jdbc.support.KeyHolder}.
 *
 * <p>Thanks to Endre Stolsvik for the suggestion!
 */
public class DerbyMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	/** 虚拟名称的默认值 */
	private static final String DEFAULT_DUMMY_NAME = "dummy";

	/** 用于插入的虚拟列的名称 */
	private String dummyName = DEFAULT_DUMMY_NAME;


	public DerbyMaxValueIncrementer() {
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 */
	public DerbyMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
		this.dummyName = DEFAULT_DUMMY_NAME;
	}

	/**
	 * @param dataSource 要使用的DataSource
	 * @param incrementerName 要使用的序列/表的名称
	 * @param columnName 要使用的序列表中的列的名称
	 * @param dummyName 用于插入的虚拟列的名称
	 */
	public DerbyMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName, String dummyName) {
		super(dataSource, incrementerName, columnName);
		this.dummyName = dummyName;
	}


	/**
	 * 设置虚拟列的名称.
	 */
	public void setDummyName(String dummyName) {
		this.dummyName = dummyName;
	}

	/**
	 * 返回虚拟列的名称.
	 */
	public String getDummyName() {
		return this.dummyName;
	}


	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " (" + getDummyName() + ") values(null)";
	}

	@Override
	protected String getIdentityStatement() {
		return "select IDENTITY_VAL_LOCAL() from " + getIncrementerName();
	}

}
