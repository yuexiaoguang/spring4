package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;

/**
 * {@link RowCallbackHandler}的实现. 实例只能使用一次.
 *
 * <p>可以单独使用它 (例如, 在测试用例中, 以确保结果集具有有效的维度),
 * 或者将其用作实际执行某些操作的回调处理器的超类, 并且将从维度信息中受益.
 *
 * <p>{@link JdbcTemplate}的用法示例:
 *
 * <pre class="code">
 * JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 *
 * RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
 * jdbcTemplate.query("select * from user", countCallback);
 * int rowCount = countCallback.getRowCount();
 * </pre>
 */
public class RowCountCallbackHandler implements RowCallbackHandler {

	/** 到目前为止见过的行 */
	private int rowCount;

	/** 到目前为止见过的列 */
	private int columnCount;

	/**
	 * 索引从0开始. ResultSetMetaData对象返回的列的类型(在 java.sql.Types中).
	 */
	private int[] columnTypes;

	/**
	 * 索引从0开始. ResultSetMetaData对象返回的列名.
	 */
	private String[] columnNames;


	/**
	 * 如果这是第一行, 则计算列大小, 否则只计算行数.
	 * <p>子类可以通过覆盖{@code processRow(ResultSet, int)}方法来执行自定义提取或处理.
	 */
	@Override
	public final void processRow(ResultSet rs) throws SQLException {
		if (this.rowCount == 0) {
			ResultSetMetaData rsmd = rs.getMetaData();
			this.columnCount = rsmd.getColumnCount();
			this.columnTypes = new int[this.columnCount];
			this.columnNames = new String[this.columnCount];
			for (int i = 0; i < this.columnCount; i++) {
				this.columnTypes[i] = rsmd.getColumnType(i + 1);
				this.columnNames[i] = JdbcUtils.lookupColumnName(rsmd, i + 1);
			}
			// could also get column names
		}
		processRow(rs, this.rowCount++);
	}

	/**
	 * 子类可以覆盖它以执行自定义提取或处理. 这个类的实现什么都不做.
	 * 
	 * @param rs 从中提取数据的ResultSet. 为每一行调用此方法
	 * @param rowNum 当前行的编号 (从 0开始)
	 */
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {
	}


	/**
	 * 返回列的类型, 为java.sql.Types常量. 在第一次调用processRow后有效.
	 * 
	 * @return 列的类型, 为java.sql.Types常量.
	 * <b>索引从 0 到 n-1.</b>
	 */
	public final int[] getColumnTypes() {
		return this.columnTypes;
	}

	/**
	 * 返回列的名称.
	 * 在第一次调用processRow后有效.
	 * 
	 * @return 列的名称.
	 * <b>索引从 0 到 n-1.</b>
	 */
	public final String[] getColumnNames() {
		return this.columnNames;
	}

	/**
	 * 返回此ResultSet的行数.
	 * 仅在处理完成后才有效
	 * 
	 * @return 此ResultSet的行数.
	 */
	public final int getRowCount() {
		return this.rowCount;
	}

	/**
	 * 返回此结果集中的列数.
	 * 一旦看到第一行就有效, 所以子类可以在处理过程中使用它
	 * 
	 * @return 此结果集中的列数
	 */
	public final int getColumnCount() {
		return this.columnCount;
	}
}
