package org.springframework.jdbc.core.support;

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.jdbc.core.DisposableSqlTypeValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * 用于表示SQL BLOB/CLOB值参数的对象. BLOB可以是InputStream或字节数组.
 * CLOB可以是Reader, InputStream或String的形式. 每个CLOB/BLOB值将与其长度一起存储.
 * 该类型基于使用哪个构造函数. 除LobCreator引用外, 此类的对象是不可变的.
 *
 * <p>此类包含对更新完成后必须关闭的LocCreator的引用. 这是通过调用closeLobCreator方法完成的.
 * LobCreator的所有处理都是由使用它的框架类完成的 - 无需为此类的最终用户设置或关闭LobCreator.
 *
 * <p>用法示例:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.update(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new Object[] {
 *       name,
 *       new SqlLobValue(contentStream, contentLength, lobHandler),
 *       new SqlLobValue(description, lobHandler)
 *     },
 *     new int[] {Types.VARCHAR, Types.BLOB, Types.CLOB});
 * </pre>
 */
public class SqlLobValue implements DisposableSqlTypeValue {

	private final Object content;

	private final int length;

	/**
	 * 这包含对LobCreator的引用 - 因此可以在更新完成后关闭它.
	 */
	private final LobCreator lobCreator;


	/**
	 * 使用给定的字节数组和DefaultLobHandler创建新的BLOB值.
	 * 
	 * @param bytes 包含BLOB值的字节数组
	 */
	public SqlLobValue(byte[] bytes) {
		this(bytes, new DefaultLobHandler());
	}

	/**
	 * 使用给定的字节数组创建新的BLOB值.
	 * 
	 * @param bytes 包含BLOB值的字节数组
	 * @param lobHandler 要使用的LobHandler
	 */
	public SqlLobValue(byte[] bytes, LobHandler lobHandler) {
		this.content = bytes;
		this.length = (bytes != null ? bytes.length : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用DefaultLobHandler和给定的内容字符串创建新的CLOB值.
	 * 
	 * @param content 包含CLOB值的String
	 */
	public SqlLobValue(String content) {
		this(content, new DefaultLobHandler());
	}

	/**
	 * 使用给定的内容字符串创建新的CLOB值.
	 * 
	 * @param content 包含CLOB值的String
	 * @param lobHandler 要使用的LobHandler
	 */
	public SqlLobValue(String content, LobHandler lobHandler) {
		this.content = content;
		this.length = (content != null ? content.length() : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用DefaultLobHandler和给定流创建新的BLOB/CLOB值.
	 * 
	 * @param stream 包含LOB值的流
	 * @param length LOB值的长度
	 */
	public SqlLobValue(InputStream stream, int length) {
		this(stream, length, new DefaultLobHandler());
	}

	/**
	 * 使用给定流创建新的BLOB/CLOB值.
	 * 
	 * @param stream 包含LOB值的流
	 * @param length LOB值的长度
	 * @param lobHandler 要使用的LobHandler
	 */
	public SqlLobValue(InputStream stream, int length, LobHandler lobHandler) {
		this.content = stream;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用DefaultLobHandler和给定的字符流创建新的CLOB值.
	 * 
	 * @param reader 包含CLOB值的字符流
	 * @param length CLOB值的长度
	 */
	public SqlLobValue(Reader reader, int length) {
		this(reader, length, new DefaultLobHandler());
	}

	/**
	 * 使用给定的字符流创建新的CLOB值.
	 * 
	 * @param reader 包含CLOB值的字符流
	 * @param length CLOB值的长度
	 * @param lobHandler 要使用的LobHandler
	 */
	public SqlLobValue(Reader reader, int length, LobHandler lobHandler) {
		this.content = reader;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}


	/**
	 * 通过LobCreator设置指定的内容.
	 */
	@Override
	public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException {
		if (sqlType == Types.BLOB) {
			if (this.content instanceof byte[] || this.content == null) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, (byte[]) this.content);
			}
			else if (this.content instanceof String) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, ((String) this.content).getBytes());
			}
			else if (this.content instanceof InputStream) {
				this.lobCreator.setBlobAsBinaryStream(ps, paramIndex, (InputStream) this.content, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for BLOB columns");
			}
		}
		else if (sqlType == Types.CLOB) {
			if (this.content instanceof String || this.content == null) {
				this.lobCreator.setClobAsString(ps, paramIndex, (String) this.content);
			}
			else if (this.content instanceof InputStream) {
				this.lobCreator.setClobAsAsciiStream(ps, paramIndex, (InputStream) this.content, this.length);
			}
			else if (this.content instanceof Reader) {
				this.lobCreator.setClobAsCharacterStream(ps, paramIndex, (Reader) this.content, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for CLOB columns");
			}
		}
		else {
			throw new IllegalArgumentException("SqlLobValue only supports SQL types BLOB and CLOB");
		}
	}

	/**
	 * 关闭LobCreator.
	 */
	@Override
	public void cleanup() {
		this.lobCreator.close();
	}
}
