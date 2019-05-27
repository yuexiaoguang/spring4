package org.springframework.jdbc.support.rowset;

import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * Spring的{@link SqlRowSet}的元数据接口, 类似于JDBC的{@link java.sql.ResultSetMetaData}.
 *
 * <p>与标准JDBC ResultSetMetaData的主要区别在于, 永远不会抛出{@link java.sql.SQLException}.
 * 这允许使用SqlRowSetMetaData而无需处理受检异常.
 * SqlRowSetMetaData将抛出Spring的{@link InvalidResultSetAccessException} (适当时).
 */
public interface SqlRowSetMetaData {

	/**
	 * 检索用作指定列的源的表的catalog名称.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return catalog名称
	 */
	String getCatalogName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列将映射到的完全限定类.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 类名
	 */
	String getColumnClassName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索RowSet中的列数.
	 * 
	 * @return 列数
	 */
	int getColumnCount() throws InvalidResultSetAccessException;

	/**
	 * 返回结果集表示的表的列名.
	 * 
	 * @return 列名
	 */
	String[] getColumnNames() throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的最大宽度.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 列的最大宽度
	 */
	int getColumnDisplaySize(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的建议的列标题.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 列标题
	 */
	String getColumnLabel(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的列名.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 列名
	 */
	String getColumnName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的SQL类型代码.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return SQL类型代码
	 */
	int getColumnType(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的DBMS特定类型名称.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 类型名称
	 */
	String getColumnTypeName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的精度.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 精度
	 */
	int getPrecision(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索指定列的小数.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 小数
	 */
	int getScale(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索用作指定列的源的表的schema名称.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return schema名称
	 */
	String getSchemaName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索用作指定列的源的表的名称.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 表的名称
	 */
	String getTableName(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指明指定列的大小写是否重要.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 如果区分大小写, 则为true, 否则为false
	 */
	boolean isCaseSensitive(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指示指定列是否包含货币值.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 如果值是货币值, 则返回true, 否则返回false
	 */
	boolean isCurrency(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 指示指定列是否包含带符号的数字.
	 * 
	 * @param columnIndex 列的索引
	 * 
	 * @return 如果列包含带符号的数字, 则返回true, 否则返回false
	 */
	boolean isSigned(int columnIndex) throws InvalidResultSetAccessException;

}
