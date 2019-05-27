package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * 用于检索标准{@code CallableStatement.getObject}方法不支持的更复杂的特定于数据库的类型的值的接口.
 *
 * <p>执行获取实际值的实际工作的实现.
 * 他们必须实现回调方法{@code getTypeValue}, 它可以抛出将被调用代码捕获和转换的SQLExceptions.
 * 此回调方法可以通过给定的CallableStatement对象访问底层Connection, 如果需要创建任何特定于数据库的对象.
 */
public interface SqlReturnType {

	/**
	 * 指示未知 (或未指定) SQL类型的常量.
	 * 如果原始操作方法未指定SQL类型, 则传入setTypeValue.
	 */
	int TYPE_UNKNOWN = Integer.MIN_VALUE;


	/**
	 * 从特定对象获取类型值.
	 * 
	 * @param cs 要操作的CallableStatement
	 * @param paramIndex 需要为其设置值的参数的索引
	 * @param sqlType 正在设置的参数的SQL类型
	 * @param typeName 参数的类型名称
	 * 
	 * @return 目标值
	 * @throws SQLException 如果设置参数值时遇到SQLException (也就是说, 不需要捕获SQLException)
	 */
	Object getTypeValue(CallableStatement cs, int paramIndex, int sqlType, String typeName)
			throws SQLException;

}
