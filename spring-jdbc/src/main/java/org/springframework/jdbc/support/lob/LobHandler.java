package org.springframework.jdbc.support.lob;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用于处理特定数据库中的大型二进制字段和大型文本字段的抽象, 无论是表示为简单类型还是大型对象.
 * 其主要目的是在{@link OracleLobHandler}中隔离Oracle 9i对LOB的特殊处理;
 * 大多数其他数据库应该能够使用提供的{@link DefaultLobHandler}.
 *
 * <p>为BLOB和CLOB提供访问器方法, 并充当LobCreator实例的工厂, 用作创建BLOB或CLOB的会话.
 * 通常为每个语句执行或每个事务实例化LobCreator;
 * 它们不是线程安全的, 因为它们可能会跟踪已分配的数据库资源, 以便在执行后释放它们.
 *
 * <p>大多数数据库/驱动程序应该能够使用{@link DefaultLobHandler},
 * 它默认委托给JDBC的直接访问器方法, 完全避免使用{@code java.sql.Blob}和{@code java.sql.Clob} API.
 * {@link DefaultLobHandler}也可以配置为使用{@code PreparedStatement.setBlob/setClob} (e.g. 对于 PostgreSQL),
 * 通过设置{@link DefaultLobHandler#setWrapAsLob "wrapAsLob"}属性来访问LOB.
 *
 * <p>不幸的是, Oracle 9i 只接受通过其自己专有的BLOB/CLOB API创建的Blob/Clob实例,
 * 另外不接受PreparedStatement相应的setter方法的流.
 * 因此, 需要在那里使用{@link OracleLobHandler}, 它使用Oracle的BLOB/CLOB API进行两种类型的访问.
 * Oracle 10g+ JDBC驱动程序也可以与{@link DefaultLobHandler}一起使用, 但在LOB大小方面存在一些限制, 具体取决于DBMS设置;
 * 从Oracle 11g开始 (实际上, 使用11g驱动程序甚至使用旧数据库), 根本不需要再使用{@link OracleLobHandler}了.
 *
 * <p>当然, 需要为每个数据库声明不同的字段类型.
 * 在Oracle中, 任何二进制内容都需要进入BLOB, 超过4000字节的所有字符内容都需要进入CLOB.
 * 在MySQL中, 没有CLOB类型的概念, 而是一个行为类似于VARCHAR的LONGTEXT类型.
 * 为了完全可移植性, 请使用LobHandler来处理由于字段大小而在某些数据库上通常需要LOB的字段 (以Oracle的数量作为指导).
 *
 * <p><b>总结推荐的选项 (对于实际的LOB字段):</b>
 * <ul>
 * <li><b>JDBC 4.0驱动程序 (包括Oracle 11g驱动程序):</b>
 * 如果数据库驱动程序在填充LOB字段时需要提示, 请使用{@link DefaultLobHandler}, 可能{@code streamAsLob=true}.
 * 如果碰巧遇到(Oracle) 数据库设置的LOB大小限制, 回退到{@code createTemporaryLob=true}.
 * <li><b>Oracle 10g驱动程序:</b> 在标准设置中使用{@link DefaultLobHandler}.
 * 在Oracle 10.1上, 设置"SetBigStringTryClob"连接属性; 从Oracle 10.2开始, DefaultLobHandler应该可以使用开箱即用的标准设置.
 * 或者, 考虑使用专有的{@link OracleLobHandler} (见下文).
 * <li><b>Oracle 9i驱动程序:</b> 将{@link OracleLobHandler}与特定于连接池的{@link OracleLobHandler#setNativeJdbcExtractor NativeJdbcExtractor}一起使用.
 * <li><b>PostgreSQL:</b> 使用{@code wrapAsLob=true}配置{@link DefaultLobHandler}, 并使用该LobHandler访问数据库表中的OID列 (但不是BYTEA).
 * <li>对于所有其他数据库驱动程序 (以及可能在某些数据库上可能变为LOB的非LOB字段): 只需使用普通的的{@link DefaultLobHandler}.
 * </ul>
 */
public interface LobHandler {

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getBytes}或使用{@code ResultSet.getBlob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名称
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	byte[] getBlobAsBytes(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getBytes}或使用{@code ResultSet.getBlob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getBinaryStream}或使用{@code ResultSet.getBlob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名称
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getBinaryStream}或使用{@code ResultSet.getBlob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getString}或使用{@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名称
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	String getClobAsString(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getString}或使用{@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	String getClobAsString(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getAsciiStream}或使用{@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名称
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getClobAsAsciiStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getAsciiStream}或使用{@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容, 或{@code null}用于 SQL NULL
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getCharacterStream}或使用{@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnName 要使用的列名称
	 * 
	 * @return 内容
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Reader getClobAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的ResultSet中检索给定列.
	 * 可能只是调用{@code ResultSet.getCharacterStream}或使用 {@code ResultSet.getClob}, 具体取决于数据库和驱动程序.
	 * 
	 * @param rs 从中检索内容的ResultSet
	 * @param columnIndex 要使用的列索引
	 * 
	 * @return 内容
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 创建一个新的{@link LobCreator}实例, i.e. 用于创建BLOB和CLOB的会话.
	 * 需要在不再需要创建的LOB之后关闭 - 通常在语句执行或事务完成之后.
	 * 
	 * @return 新的LobCreator实例
	 */
	LobCreator getLobCreator();

}
