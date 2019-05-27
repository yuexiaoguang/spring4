package org.springframework.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 将复杂类型设置为语句参数的简单接口.
 *
 * <p>实现执行设置实际值的实际工作.
 * 必须实现回调方法{@code setValue}, 它可以抛出将被调用代码捕获和转换的SQLExceptions.
 * 如果需要创建任何特定于数据库的对象, 则此回调方法可以通过给定的PreparedStatement对象访问底层Connection.
 */
public interface SqlValue {

	/**
	 * 在给定的PreparedStatement上设置值.
	 * 
	 * @param ps 要处理的PreparedStatement
	 * @param paramIndex 需要为其设置值的参数的索引
	 * 
	 * @throws SQLException 如果在设置参数值时遇到SQLException
	 */
	void setValue(PreparedStatement ps, int paramIndex)	throws SQLException;

	/**
	 * 清理此值对象持有的资源.
	 */
	void cleanup();

}
