package org.springframework.jdbc.support.lob;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link LobHandler}接口的默认实现.
 * 调用{@code java.sql.ResultSet}和{@code java.sql.PreparedStatement}提供的直接访问器方法.
 *
 * <p>默认情况下, 传入的流将传递到JDBC驱动程序{@link PreparedStatement}上的相应{@code setBinary/Ascii/CharacterStream}方法.
 * 如果指定的内容长度为负, 则此处理程序将使用set-stream方法的JDBC 4.0变体, 而不使用length参数;
 * 否则, 它会将指定的长度传递给驱动程序.
 *
 * <p>根据规范有关简单BLOB和CLOB处理的建议, 此LobHandler应适用于任何符合JDBC的JDBC驱动程序.
 * 这根本不适用于Oracle 9i的驱动程序; 从Oracle 10g开始, 它确实有效, 但可能仍然存在LOB大小限制.
 * 即使在使用较旧的数据库服务器时, 也请考虑使用最新的Oracle驱动.
 * 有关完整的建议, 请参阅{@link LobHandler} javadoc.
 *
 * <p>某些JDBC驱动程序需要通过JDBC {@code setBlob} / {@code setClob} API显式设置具有BLOB/CLOB目标列的值:
 * 例如, PostgreSQL的驱动程序.
 * 对这样的驱动程序进行操作时, 将{@link #setWrapAsLob "wrapAsLob"}属性切换为"true".
 *
 * <p>在JDBC 4.0上, 此LobHandler还支持通过直接获取流参数的{@code setBlob}/{@code setClob}变体, 来流式传输BLOB/CLOB内容.
 * 在对完全兼容的JDBC 4.0驱动程序进行操作时, 请考虑将{@link #setStreamAsLob "streamAsLob"}属性切换为"true".
 *
 * <p>最后, 主要作为{@link OracleLobHandler}的直接等效, 这个LobHandler还支持创建临时BLOB/CLOB对象.
 * 当"streamAsLob"碰巧遇到LOB大小限制时, 请考虑将{@link #setCreateTemporaryLob "createTemporaryLob"}属性切换为"true".
 *
 * <p>有关建议摘要, 请参阅{@link LobHandler}接口javadoc.
 */
public class DefaultLobHandler extends AbstractLobHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean wrapAsLob = false;

	private boolean streamAsLob = false;

	private boolean createTemporaryLob = false;


	/**
	 * 指定是否使用带有Blob/Clob参数的JDBC {@code setBlob} / {@code setClob}方法,
	 * 将字节数组/字符串提交到包含在JDBC Blob / Clob对象中的JDBC驱动程序.
	 * <p>默认为"false", 使用通用JDBC 2.0 {@code setBinaryStream}/{@code setCharacterStream}方法设置内容.
	 * 将此切换为"true", 针对已知需要此类包装的JDBC驱动程序的显式Blob/Clob包装
	 * (e.g. PostgreSQL用于访问OID列, 而BYTEA列需要以标准方式访问).
	 * <p>此设置会影响字节数组/字符串参数以及流参数,
	 * 除非{@link #setStreamAsLob "streamAsLob"}覆盖此处理以使用JDBC 4.0的新显式流支持.
	 */
	public void setWrapAsLob(boolean wrapAsLob) {
		this.wrapAsLob = wrapAsLob;
	}

	/**
	 * 指定是否使用JDBC 4.0 {@code setBlob} / {@code setClob}方法和流参数,
	 * 将二进制流/字符流作为显式LOB内容提交到JDBC驱动程序.
	 * <p>默认为"false", 使用通用JDBC 2.0 {@code setBinaryStream} / {@code setCharacterStream}方法设置内容.
	 * 如果JDBC驱动程序实际上支持那些JDBC 4.0操作(e.g. Derby), 则将其切换为"true"以显式使用JDBC 4.0的流式传输.
	 * <p>此设置会影响流参数, 以及字节数组/字符串参数, 从而需要JDBC 4.0支持.
	 * 要支持针对JDBC 3.0的LOB内容, 请查看{@link #setWrapAsLob "wrapAsLob"}设置.
	 */
	public void setStreamAsLob(boolean streamAsLob) {
		this.streamAsLob = streamAsLob;
	}

	/**
	 * 指定是否将字节数组/ String复制到通过JDBC 4.0 {@code createBlob} / {@code createClob}方法创建的临时JDBC Blob / Clob对象中.
	 * <p>默认为"false", 使用通用JDBC 2.0 {@code setBinaryStream} / {@code setCharacterStream}方法设置内容.
	 * 将此切换为"true", 以便使用JDBC 4.0创建显式Blob / Clob.
	 * <p>此设置会影响流参数, 以及字节数组/字符串参数, 从而需要JDBC 4.0支持.
	 * 要支持针对JDBC 3.0的LOB内容, 请查看{@link #setWrapAsLob "wrapAsLob"}设置.
	 */
	public void setCreateTemporaryLob(boolean createTemporaryLob) {
		this.createTemporaryLob = createTemporaryLob;
	}


	@Override
	public byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as bytes");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBytes(1, (int) blob.length());
		}
		else {
			return rs.getBytes(columnIndex);
		}
	}

	@Override
	public InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as binary stream");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBinaryStream();
		}
		else {
			return rs.getBinaryStream(columnIndex);
		}
	}

	@Override
	public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as string");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getSubString(1, (int) clob.length());
		}
		else {
			return rs.getString(columnIndex);
		}
	}

	@Override
	public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as ASCII stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getAsciiStream();
		}
		else {
			return rs.getAsciiStream(columnIndex);
		}
	}

	@Override
	public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as character stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getCharacterStream();
		}
		else {
			return rs.getCharacterStream(columnIndex);
		}
	}

	@Override
	public LobCreator getLobCreator() {
		return (this.createTemporaryLob ? new TemporaryLobCreator() : new DefaultLobCreator());
	}


	/**
	 * 默认LobCreator实现.
	 * 可以在DefaultLobHandler扩展中进行子类化.
	 */
	protected class DefaultLobCreator implements LobCreator {

		@Override
		public void setBlobAsBytes(PreparedStatement ps, int paramIndex, byte[] content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new ByteArrayInputStream(content), content.length);
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(content));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else {
				ps.setBytes(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set bytes for BLOB with length " + content.length :
						"Set BLOB to null");
			}
		}

		@Override
		public void setBlobAsBinaryStream(
				PreparedStatement ps, int paramIndex, InputStream binaryStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (binaryStream != null) {
					if (contentLength >= 0) {
						ps.setBlob(paramIndex, binaryStream, contentLength);
					}
					else {
						ps.setBlob(paramIndex, binaryStream);
					}
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (binaryStream != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(binaryStream, contentLength));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setBinaryStream(paramIndex, binaryStream, contentLength);
			}
			else {
				ps.setBinaryStream(paramIndex, binaryStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(binaryStream != null ? "Set binary stream for BLOB with length " + contentLength :
						"Set BLOB to null");
			}
		}

		@Override
		public void setClobAsString(PreparedStatement ps, int paramIndex, String content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new StringReader(content), content.length());
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new PassThroughClob(content));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else {
				ps.setString(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set string for CLOB with length " + content.length() :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsAsciiStream(
				PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (asciiStream != null) {
					try {
						Reader reader = new InputStreamReader(asciiStream, "US-ASCII");
						if (contentLength >= 0) {
							ps.setClob(paramIndex, reader, contentLength);
						}
						else {
							ps.setClob(paramIndex, reader);
						}
					}
					catch (UnsupportedEncodingException ex) {
						throw new SQLException("US-ASCII encoding not supported: " + ex);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (asciiStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(asciiStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setAsciiStream(paramIndex, asciiStream, contentLength);
			}
			else {
				ps.setAsciiStream(paramIndex, asciiStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(asciiStream != null ? "Set ASCII stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsCharacterStream(
				PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (characterStream != null) {
					if (contentLength >= 0) {
						ps.setClob(paramIndex, characterStream, contentLength);
					}
					else {
						ps.setClob(paramIndex, characterStream);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (characterStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(characterStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setCharacterStream(paramIndex, characterStream, contentLength);
			}
			else {
				ps.setCharacterStream(paramIndex, characterStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(characterStream != null ? "Set character stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void close() {
			// 不创建临时LOB时, 什么都不需要做
		}
	}
}
