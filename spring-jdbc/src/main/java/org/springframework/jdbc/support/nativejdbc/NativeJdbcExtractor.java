package org.springframework.jdbc.support.nativejdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 用于从连接池中包装的对象中提取本机JDBC对象的接口.
 * 这对于在应用程序代码中转换为{@code OracleConnection}或{@code OracleResultSet}等本机实现是必要的,
 * 例如创建Blob或访问特定于供应商的功能.
 *
 * <p>Note: 如果打算转换为特定于数据库的实现, 例如{@code OracleConnection}或{@code OracleResultSet},
 * 则必须设置自定义{@code NativeJdbcExtractor}.
 * 否则, 任何包装好的JDBC对象都没问题, 不需要解包.
 *
 * <p>Note: 为了能够支持任何池的本机ResultSet包装策略, 建议通过此提取器获取本机Statement <i>和</i>本机ResultSet.
 * 有些池只允许解包Statement, 有些只是为了解包ResultSet - 上面的策略将涵盖两者.
 * 通常<i>没有必要</i>解包Connection以检索本机ResultSet.
 *
 * <p>使用包装Connections但不包装Statement的简单连接池时, {@link SimpleNativeJdbcExtractor}通常就足够了.
 * 但是，某些池 (如Apache的Commons DBCP) 包装它们返回的<i>所有</i> JDBC对象:
 * 因此, 需要使用特定的{@code NativeJdbcExtractor} (如{@link CommonsDbcpNativeJdbcExtractor}).
 *
 * <p>{@link org.springframework.jdbc.core.JdbcTemplate}可以正确应用指定的{@code NativeJdbcExtractor}, 解包它创建的所有JDBC对象.
 * 请注意, 如果打算在数据访问代码中强制转换为本机实现, 则这是必要的.
 *
 * <p>{@link org.springframework.jdbc.support.lob.OracleLobHandler},
 * 是Spring的{@link org.springframework.jdbc.support.lob.LobHandler}接口的Oracle特定实现,
 * 需要{@code NativeJdbcExtractor}来获取本机{@code OracleConnection}.
 * 对于可能希望在应用程序中使用的其他Oracle特定功能, 例如Oracle InterMedia, 这也是必需的.
 */
public interface NativeJdbcExtractor {

	/**
	 * 返回是否有必要在本机Connection上工作以接收本机Statement.
	 * <p>如果连接池不允许从其Statement包装器中提取本机JDBC对象, 但支持检索本机JDBC Connection, 则应该为true.
	 * 这样, 应用程序仍然可以通过处理本机JDBC Connection来接收本机Statement和ResultSet.
	 */
	boolean isNativeConnectionNecessaryForNativeStatements();

	/**
	 * 返回是否有必要在本机Connection上工作以接收本机PreparedStatements.
	 * <p>如果连接池不允许从其PreparedStatement包装器中提取本机JDBC对象, 但支持检索本机JDBC Connection, 则应该为true.
	 * 这样, 应用程序仍然可以通过处理本机JDBC Connection来接收本机Statement和ResultSet.
	 */
	boolean isNativeConnectionNecessaryForNativePreparedStatements();

	/**
	 * 返回是否有必要在本机Connection上工作以接收本机CallableStatements.
	 * <p>如果连接池不允许从其CallableStatement包装器中提取本机JDBC对象, 但支持检索本机JDBC Connection, 则应该为true.
	 * 这样, 应用程序仍然可以通过处理本机JDBC Connection来接收本机Statement和ResultSet.
	 */
	boolean isNativeConnectionNecessaryForNativeCallableStatements();

	/**
	 * 检索给定Connection的底层本机JDBC Connection.
	 * 如果不能解包, 则假定返回给定的Connection.
	 * 
	 * @param con Connection句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC连接; 否则, 原来的Connection
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Connection getNativeConnection(Connection con) throws SQLException;

	/**
	 * 检索给定Statement的底层本机JDBC Connection.
	 * 如果不能解包, 则返回{@code Statement.getConnection()}.
	 * <p>如果数据访问代码已经有Statement, 那么使用这种额外的方法可以更有效地解包.
	 * {@code Statement.getConnection()}经常返回本机JDBC Connection, 即使Statement本身被池包装.
	 * 
	 * @param stmt Statement句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC连接; 否则, 原来的Connection
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Connection getNativeConnectionFromStatement(Statement stmt) throws SQLException;

	/**
	 * 检索给定Statement的底层本机JDBC Statement.
	 * 如果不能解包, 则返回给定的Statement.
	 * 
	 * @param stmt Statement句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC Statement; 否则, 原来的Statement
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Statement getNativeStatement(Statement stmt) throws SQLException;

	/**
	 * 检索给定语句的底层本机JDBC PreparedStatement.
	 * 如果不能解包, 则返回给定的PreparedStatement.
	 * 
	 * @param ps PreparedStatement句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC PreparedStatement; 否则, 原来的PreparedStatement
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	PreparedStatement getNativePreparedStatement(PreparedStatement ps) throws SQLException;

	/**
	 * 检索给定语句的底层本机JDBC CallableStatement.
	 * 如果不能解包, 则返回给定的 CallableStatement.
	 * 
	 * @param cs CallableStatement句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC CallableStatement; 否则, 原来的CallableStatement
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	CallableStatement getNativeCallableStatement(CallableStatement cs) throws SQLException;

	/**
	 * 检索给定语句的底层本机JDBC ResultSet.
	 * 如果不能解包, 则返回给定的ResultSet.
	 * 
	 * @param rs ResultSet句柄, 可能由连接池包装
	 * 
	 * @return 如果可能，底层的本机JDBC ResultSet; 否则, 原来的ResultSet
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	ResultSet getNativeResultSet(ResultSet rs) throws SQLException;

}
