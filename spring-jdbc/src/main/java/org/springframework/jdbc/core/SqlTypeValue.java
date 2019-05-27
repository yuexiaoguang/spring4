package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;

/**
 * 用于为标准{@code setObject}方法不支持的更复杂的特定于数据库的类型设置值的接口.
 * 这实际上是{@link org.springframework.jdbc.support.SqlValue}的扩展变体.
 *
 * <p>实现执行设置实际值的实际工作.
 * 必须实现回调方法 {@code setTypeValue}, 它可以抛出将被调用代码捕获和转换的SQLExceptions.
 * 如果需要创建任何特定于数据库的对象, 则此回调方法可以通过给定的PreparedStatement对象访问底层Connection.
 */
public interface SqlTypeValue {

	/**
	 * 指示未知 (或未指定)SQL类型的常量.
	 * 如果原始操作方法未指定SQL类型, 则传入{@code setTypeValue}.
	 */
	int TYPE_UNKNOWN = JdbcUtils.TYPE_UNKNOWN;


	/**
	 * 在给定的PreparedStatement上设置类型值.
	 * 
	 * @param ps 要处理的PreparedStatement
	 * @param paramIndex 需要为其设置值的参数的索引
	 * @param sqlType 正在设置的参数的SQL类型
	 * @param typeName 参数的类型名称 (可选)
	 * 
	 * @throws SQLException 如果在设置参数值时遇到SQLException
	 */
	void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException;

}
