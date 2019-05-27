package org.springframework.jdbc.support.rowset;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;

/**
 * {@link javax.sql.RowSet}的镜像接口, 表示{@link java.sql.ResultSet}数据的断开连接变体.
 *
 * <p>与标准JDBC RowSet的主要区别在于, 此处永远不会抛出{@link java.sql.SQLException}.
 * 这允许使用SqlRowSet而无需处理受检异常.
 * SqlRowSet将抛出Spring的{@link InvalidResultSetAccessException} (适当时).
 *
 * <p>Note: 此接口扩展了{@code java.io.Serializable}标记接口.
 * 通常持有断开连接的数据的实现被鼓励实际可序列化 (尽可能).
 */
public interface SqlRowSet extends Serializable {

	/**
	 * 检索元数据, i.e. 此行集的列的数量, 类型和属性.
	 * 
	 * @return 相应的SqlRowSetMetaData实例
	 */
	SqlRowSetMetaData getMetaData();

	/**
	 * 将给定的列标签映射到其列索引.
	 * 
	 * @param columnLabel 列名
	 * 
	 * @return 给定列标签的列索引
	 */
	int findColumn(String columnLabel) throws InvalidResultSetAccessException;


	// RowSet methods for extracting data values

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	BigDecimal getBigDecimal(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	BigDecimal getBigDecimal(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	boolean getBoolean(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	boolean getBoolean(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	byte getByte(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	byte getByte(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	Date getDate(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	Date getDate(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Date getDate(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Date getDate(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	double getDouble(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	double getDouble(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	float getFloat(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	float getFloat(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	int getInt(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	int getInt(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	long getLong(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	long getLong(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值 (用于NCHAR, NVARCHAR, LONGNVARCHAR 列).
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	String getNString(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值 (用于NCHAR, NVARCHAR, LONGNVARCHAR 列).
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	String getNString(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	Object getObject(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	Object getObject(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * @param map 包含从SQL类型到Java类型的映射的Map对象
	 * 
	 * @return 列的值
	 */
	Object getObject(int columnIndex,  Map<String, Class<?>> map) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * @param map 包含从SQL类型到Java类型的映射的Map对象
	 * 
	 * @return 列的值
	 */
	Object getObject(String columnLabel,  Map<String, Class<?>> map) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * @param type 要将指定列转换为的Java类型
	 * 
	 * @return 列的值
	 */
	<T> T getObject(int columnIndex, Class<T> type) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * @param type 要将指定列转换为的Java类型
	 * 
	 * @return 列的值
	 */
	<T> T getObject(String columnLabel, Class<T> type) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	short getShort(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	short getShort(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	String getString(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	String getString(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	Time getTime(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	Time getTime(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Time getTime(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Time getTime(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * 
	 * @return 列的值
	 */
	Timestamp getTimestamp(int columnIndex) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * 
	 * @return 列的值
	 */
	Timestamp getTimestamp(String columnLabel) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnIndex 列索引
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Timestamp getTimestamp(int columnIndex, Calendar cal) throws InvalidResultSetAccessException;

	/**
	 * 检索当前行中指定列的值.
	 * 
	 * @param columnLabel 列标签
	 * @param cal 用于构造Date的Calendar
	 * 
	 * @return 列的值
	 */
	Timestamp getTimestamp(String columnLabel, Calendar cal) throws InvalidResultSetAccessException;


	// RowSet navigation methods

	/**
	 * 将游标移动到行集中的给定行号, 在最后一行之后.
	 * 
	 * @param row 游标应移动的行数
	 * 
	 * @return {@code true}如果游标在行集上, 否则{@code false}
	 */
	boolean absolute(int row) throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的末尾.
	 */
	void afterLast() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的前面, 在第一行之前.
	 */
	void beforeFirst() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的第一行.
	 * 
	 * @return {@code true}如果游标在有效行上, 否则{@code false}
	 */
	boolean first() throws InvalidResultSetAccessException;

	/**
	 * 检索当前行号.
	 * 
	 * @return 当前行号
	 */
	int getRow() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否在此行集的最后一行之后.
	 * 
	 * @return {@code true}是, {@code false}否
	 */
	boolean isAfterLast() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否在此行集的第一行之前.
	 * 
	 * @return {@code true}是, {@code false}否
	 */
	boolean isBeforeFirst() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否在此行集的第一行.
	 * 
	 * @return {@code true}是, {@code false}否
	 */
	boolean isFirst() throws InvalidResultSetAccessException;

	/**
	 * 检索游标是否在此行集的最后一行.
	 * 
	 * @return {@code true}是, {@code false}否
	 */
	boolean isLast() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到此行集的最后一行.
	 * 
	 * @return {@code true}如果光标在有效行上, 否则{@code false}
	 */
	boolean last() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到下一行.
	 * 
	 * @return {@code true}如果新行有效, {@code false}如果没有更多行
	 */
	boolean next() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动到上一行.
	 * 
	 * @return {@code true}如果新行有效, {@code false}如果它不在行集
	 */
	boolean previous() throws InvalidResultSetAccessException;

	/**
	 * 将游标移动相对的行数, 正数或负数.
	 * 
	 * @return {@code true}如果游标在一行上, 否则{@code false} otherwise
	 */
	boolean relative(int rows) throws InvalidResultSetAccessException;

	/**
	 * 最后一列读取的值是否为SQL {@code NULL}.
	 * <p>请注意, 必须首先调用其中一个getter方法, 然后调用{@code wasNull()}方法.
	 * 
	 * @return {@code true}如果检索到的最新列是SQL {@code NULL}, 否则{@code false}
	 */
	boolean wasNull() throws InvalidResultSetAccessException;

}
