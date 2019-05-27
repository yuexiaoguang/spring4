package org.springframework.jdbc.support.lob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

import org.springframework.util.FileCopyUtils;

/**
 * 简单的JDBC {@link Clob}适配器, 用于公开给定的字符串或字符流.
 * {@link DefaultLobHandler}可选择使用.
 */
class PassThroughClob implements Clob {

	private String content;

	private Reader characterStream;

	private InputStream asciiStream;

	private long contentLength;


	public PassThroughClob(String content) {
		this.content = content;
		this.contentLength = content.length();
	}

	public PassThroughClob(Reader characterStream, long contentLength) {
		this.characterStream = characterStream;
		this.contentLength = contentLength;
	}

	public PassThroughClob(InputStream asciiStream, long contentLength) {
		this.asciiStream = asciiStream;
		this.contentLength = contentLength;
	}


	@Override
	public long length() throws SQLException {
		return this.contentLength;
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		try {
			if (this.content != null) {
				return new StringReader(this.content);
			}
			else if (this.characterStream != null) {
				return this.characterStream;
			}
			else {
				return new InputStreamReader(this.asciiStream, "US-ASCII");
			}
		}
		catch (UnsupportedEncodingException ex) {
			throw new SQLException("US-ASCII encoding not supported: " + ex);
		}
	}

	@Override
	public InputStream getAsciiStream() throws SQLException {
		try {
			if (this.content != null) {
				return new ByteArrayInputStream(this.content.getBytes("US-ASCII"));
			}
			else if (this.characterStream != null) {
				String tempContent = FileCopyUtils.copyToString(this.characterStream);
				return new ByteArrayInputStream(tempContent.getBytes("US-ASCII"));
			}
			else {
				return this.asciiStream;
			}
		}
		catch (UnsupportedEncodingException ex) {
			throw new SQLException("US-ASCII encoding not supported: " + ex);
		}
		catch (IOException ex) {
			throw new SQLException("Failed to read stream content: " + ex);
		}
	}


	@Override
	public Reader getCharacterStream(long pos, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Writer setCharacterStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream setAsciiStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSubString(long pos, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setString(long pos, String str) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(String searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(Clob searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void truncate(long len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void free() throws SQLException {
		// no-op
	}

}
