package org.springframework.jdbc.support.lob;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 用于抽象可能特定于数据库的大型二进制字段和大型文本字段创建的接口.
 * 不适用于API中的{@code java.sql.Blob} 和 {@code java.sql.Clob}实例, 因为某些JDBC驱动程序不支持这些类型.
 *
 * <p>LOB创建部分是{@link LobHandler}实现通常不同的地方.
 * 可能的策略包括{@code PreparedStatement.setBinaryStream/setCharacterStream}
 * 以及{@code PreparedStatement.setBlob/setClob},
 * 使用stream参数(需要 JDBC 4.0)或{{@code java.sql.Blob/Clob}包装器对象.
 *
 * <p>LobCreator表示用于创建BLOB的会话:
 * 它<i>不是</i>线程安全的, 需要为每个语句执行或每个事务实例化.
 * 每个LobCreator都需要在完成后关闭.
 *
 * <p>为方便使用PreparedStatement和LobCreator,
 * 考虑将{@link org.springframework.jdbc.core.JdbcTemplate}与
 * {@link org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback}实现一起使用.
 * 有关详细信息, 请参阅后者的javadoc.
 */
public interface LobCreator extends Closeable {

	/**
	 * 使用给定的参数索引, 将给定内容设置为给定语句上的字节.
	 * 可能只是调用{@code PreparedStatement.setBytes}或为它创建一个Blob实例, 具体取决于数据库和驱动程序.
	 * 
	 * @param ps 要设置内容的PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param content 内容, 或{@code null}用于SQL NULL
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void setBlobAsBytes(PreparedStatement ps, int paramIndex, byte[] content)
			throws SQLException;

	/**
	 * 使用给定的参数索引, 将给定内容设置为给定语句的二进制流.
	 * 可能只是调用{@code PreparedStatement.setBinaryStream}或为它创建一个Blob实例, 具体取决于数据库和驱动程序.
	 * 
	 * @param ps 要设置内容的PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param contentStream 内容, 或{@code null}用于SQL NULL
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void setBlobAsBinaryStream(
			PreparedStatement ps, int paramIndex, InputStream contentStream, int contentLength)
			throws SQLException;

	/**
	 * 使用给定的参数索引, 将给定内容设置为给定语句的String.
	 * 可能只是调用{@code PreparedStatement.setString}或为它创建一个Clob实例, 具体取决于数据库和驱动程序.
	 * 
	 * @param ps 要设置内容的PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param content 内容, 或{@code null}用于SQL NULL
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void setClobAsString(PreparedStatement ps, int paramIndex, String content)
			throws SQLException;

	/**
	 * 使用给定的参数索引, 将给定内容设置为给定语句的ASCII流.
	 * 可能只是调用{@code PreparedStatement.setAsciiStream}或为它创建一个Clob实例, 具体取决于数据库和驱动程序.
	 * 
	 * @param ps 要设置内容的PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param asciiStream 内容, 或{@code null}用于SQL NULL
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void setClobAsAsciiStream(
			PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength)
			throws SQLException;

	/**
	 * 使用给定的参数索引, 将给定内容设置为给定语句的字符流.
	 * 可能只是调用{@code PreparedStatement.setCharacterStream}或为它创建一个Clob实例, 具体取决于数据库和驱动程序.
	 * 
	 * @param ps 要设置内容的PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param characterStream 内容, 或{@code null}用于SQL NULL
	 * 
	 * @throws SQLException 如果被JDBC方法抛出
	 */
	void setClobAsCharacterStream(
			PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength)
			throws SQLException;

	/**
	 * 关闭此LobCreator会话, 并释放其临时创建的BLOB和CLOB.
	 * 如果使用PreparedStatement的标准方法, 则无需执行任何操作, 但如果使用专有方法, 则可能需要释放数据库资源.
	 * <p><b>NOTE</b>: 在执行所涉及的PreparedStatements或刷新受影响的O/R映射会话之后需要调用.
	 * 否则, 临时BLOB的数据库资源可能会保持分配状态.
	 */
	@Override
	void close();

}
