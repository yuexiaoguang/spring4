package org.springframework.jdbc.support.lob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.FileCopyUtils;

/**
 * 基于临时LOB的{@link LobCreator}实现,
 * 使用JDBC 4.0的{@link java.sql.Connection#createBlob()} / {@link java.sql.Connection#createClob()}机制.
 *
 * <p>由DefaultLobHandler的{@link DefaultLobHandler#setCreateTemporaryLob}模式使用.
 * 也可以直接用于重用跟踪和释放临时LOB.
 */
public class TemporaryLobCreator implements LobCreator {

	protected static final Log logger = LogFactory.getLog(TemporaryLobCreator.class);

	private final Set<Blob> temporaryBlobs = new LinkedHashSet<Blob>(1);

	private final Set<Clob> temporaryClobs = new LinkedHashSet<Clob>(1);


	@Override
	public void setBlobAsBytes(PreparedStatement ps, int paramIndex, byte[] content)
			throws SQLException {

		if (content != null) {
			Blob blob = ps.getConnection().createBlob();
			blob.setBytes(1, content);
			this.temporaryBlobs.add(blob);
			ps.setBlob(paramIndex, blob);
		}
		else {
			ps.setBlob(paramIndex, (Blob) null);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(content != null ? "Copied bytes into temporary BLOB with length " + content.length :
					"Set BLOB to null");
		}
	}

	@Override
	public void setBlobAsBinaryStream(
			PreparedStatement ps, int paramIndex, InputStream binaryStream, int contentLength)
			throws SQLException {

		if (binaryStream != null) {
			Blob blob = ps.getConnection().createBlob();
			try {
				FileCopyUtils.copy(binaryStream, blob.setBinaryStream(1));
			}
			catch (IOException ex) {
				throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
			}
			this.temporaryBlobs.add(blob);
			ps.setBlob(paramIndex, blob);
		}
		else {
			ps.setBlob(paramIndex, (Blob) null);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(binaryStream != null ?
					"Copied binary stream into temporary BLOB with length " + contentLength :
					"Set BLOB to null");
		}
	}

	@Override
	public void setClobAsString(PreparedStatement ps, int paramIndex, String content)
			throws SQLException {

		if (content != null) {
			Clob clob = ps.getConnection().createClob();
			clob.setString(1, content);
			this.temporaryClobs.add(clob);
			ps.setClob(paramIndex, clob);
		}
		else {
			ps.setClob(paramIndex, (Clob) null);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(content != null ? "Copied string into temporary CLOB with length " + content.length() :
					"Set CLOB to null");
		}
	}

	@Override
	public void setClobAsAsciiStream(
			PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength)
			throws SQLException {

		if (asciiStream != null) {
			Clob clob = ps.getConnection().createClob();
			try {
				FileCopyUtils.copy(asciiStream, clob.setAsciiStream(1));
			}
			catch (IOException ex) {
				throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
			}
			this.temporaryClobs.add(clob);
			ps.setClob(paramIndex, clob);
		}
		else {
			ps.setClob(paramIndex, (Clob) null);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(asciiStream != null ?
					"Copied ASCII stream into temporary CLOB with length " + contentLength :
					"Set CLOB to null");
		}
	}

	@Override
	public void setClobAsCharacterStream(
			PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength)
			throws SQLException {

		if (characterStream != null) {
			Clob clob = ps.getConnection().createClob();
			try {
				FileCopyUtils.copy(characterStream, clob.setCharacterStream(1));
			}
			catch (IOException ex) {
				throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
			}
			this.temporaryClobs.add(clob);
			ps.setClob(paramIndex, clob);
		}
		else {
			ps.setClob(paramIndex, (Clob) null);
		}

		if (logger.isDebugEnabled()) {
			logger.debug(characterStream != null ?
					"Copied character stream into temporary CLOB with length " + contentLength :
					"Set CLOB to null");
		}
	}

	@Override
	public void close() {
		try {
			for (Blob blob : this.temporaryBlobs) {
				blob.free();
			}
			for (Clob clob : this.temporaryClobs) {
				clob.free();
			}
		}
		catch (SQLException ex) {
			logger.error("Could not free LOBs", ex);
		}
	}
}
