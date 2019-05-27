package org.springframework.jdbc.support.lob;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.FileCopyUtils;

/**
 * Oracle数据库的{@link LobHandler}实现.
 * 在使用Oracle的JDBC驱动程序时, 根据需要使用专有API创建{@code oracle.sql.BLOB}和{@code oracle.sql.CLOB}实例.
 * 请注意, 此LobHandler需要Oracle JDBC驱动程序9i或更高版本!
 *
 * <p>虽然大多数数据库都能够使用{@link DefaultLobHandler},
 * 但Oracle 9i (或更具体地说, Oracle 9i JDBC驱动程序)只接受通过其自己的专有BLOB/CLOB API创建的Blob/Clob实例,
 * 而且不接受PreparedStatement相应的setter方法的流.
 * 因此, 需要使用此LobHandler实现的策略, 或升级到Oracle 10g/11g驱动程序 (仍支持访问Oracle 9i数据库).
 *
 * <p><b>NOTE: 从Oracle 10.2开始, {@link DefaultLobHandler}应该可以开箱即用.
 * 在Oracle 11g上, 也可以使用基于JDBC 4.0的选项,
 * 例如{@link DefaultLobHandler#setStreamAsLob}和{@link DefaultLobHandler#setCreateTemporaryLob},
 * 这使得这个专有的OracleLobHandler过时了.</b>
 * 此外, 即使访问旧数据库, 也要考虑升级到新驱动程序.
 * 有关建议摘要, 请参阅{@link LobHandler}接口javadoc.
 *
 * <p>需要在本机JDBC连接上工作, 才能将其强制转换为{@code oracle.jdbc.OracleConnection}.
 * 如果从连接池传递Connection (Java EE环境中的常见情况),
 * 则需要设置适当的{@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor},
 * 以允许自动检索底层本机JDBC Connection.
 * LobHandler和NativeJdbcExtractor是单独的问题, 因此它们由单独的策略接口表示.
 *
 * <p>通过反射编码以避免依赖于Oracle类.
 * 甚至通过反射读取Oracle常量, 因为具有不同常量值的不同Oracle驱动程序(classes12, ojdbc14, ojdbc5, ojdbc6)!
 * 由于此LobHandler在实例化时初始化Oracle类, 如果不想依赖于类路径中的Oracle JAR,
 * 请不要将其定义为急切初始化单例: 使用"lazy-init=true"来避免此问题.
 *
 * @deprecated Oracle 10g驱动程序及更高版本使用{@link DefaultLobHandler}.
 * 考虑使用10g/11g驱动程序甚至对Oracle 9i数据库!
 * {@link DefaultLobHandler#setCreateTemporaryLob}直接等效于OracleLobHandler的实现策略, 只使用标准的JDBC 4.0 API.
 * 也就是说, 在大多数情况下, 常规的DefaultLobHandler设置也可以正常工作.
 */
@Deprecated
public class OracleLobHandler extends AbstractLobHandler {

	private static final String BLOB_CLASS_NAME = "oracle.sql.BLOB";

	private static final String CLOB_CLASS_NAME = "oracle.sql.CLOB";

	private static final String DURATION_SESSION_FIELD_NAME = "DURATION_SESSION";

	private static final String MODE_READWRITE_FIELD_NAME = "MODE_READWRITE";

	private static final String MODE_READONLY_FIELD_NAME = "MODE_READONLY";


	protected final Log logger = LogFactory.getLog(getClass());

	private NativeJdbcExtractor nativeJdbcExtractor;

	private Boolean cache = Boolean.TRUE;

	private Boolean releaseResourcesAfterRead = Boolean.FALSE;

	private Class<?> blobClass;

	private Class<?> clobClass;

	private final Map<Class<?>, Integer> durationSessionConstants = new HashMap<Class<?>, Integer>(2);

	private final Map<Class<?>, Integer> modeReadWriteConstants = new HashMap<Class<?>, Integer>(2);

	private final Map<Class<?>, Integer> modeReadOnlyConstants = new HashMap<Class<?>, Integer>(2);


	/**
	 * 设置适当的NativeJdbcExtractor以便能够检索底层本机{@code oracle.jdbc.OracleConnection}.
	 * 这对于基于DataSource的连接池是必需的, 因为那些需要返回无法转换为本机Connection实现的包装JDBC Connection句柄.
	 * <p>实际上, 这个LobHandler只调用一个NativeJdbcExtractor方法,
	 * 即使用PreparedStatement参数的 {@code getNativeConnectionFromStatement}方法
	 * (如果没有设置提取器, 则返回{@code PreparedStatement.getConnection()}调用).
	 * <p>一个常见的选择是{@code SimpleNativeJdbcExtractor}, 其Connection解包 (这是OracleLobHandler需要的)将与许多连接池一起使用.
	 * See {@code SimpleNativeJdbcExtractor} and
	 * <a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/jdbc/OracleConnection.html">
	 * oracle.jdbc.OracleConnection</a> javadoc for details.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}

	/**
	 * 设置是否将临时LOB缓存在缓冲区缓存中.
	 * 该值将传递给 BLOB/CLOB.createTemporary.
	 * <p>默认{@code true}.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#createTemporary()">oracle.sql.BLOB.createTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#createTemporary()">oracle.sql.CLOB.createTemporary</a></li>
	 * </ul>
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * 设置是否积极释放LOB使用的任何资源.
	 * 如果设置为{@code true}, 那么只能读取一次LOB值. 由于资源已关闭, 任何后续读取都将失败.
	 * <p>当查询生成占用TEMPORARY表空间中的空间的大型临时LOB, 或者释放驱动程序为LOB读取分配的内存时, 将此属性设置为{@code true}会很有用.
	 * <p>Default is {@code false}.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#freeTemporary()">oracle.sql.BLOB.freeTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#freeTemporary()">oracle.sql.CLOB.freeTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#open()">oracle.sql.BLOB.open</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#open()">oracle.sql.CLOB.open</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#open()">oracle.sql.BLOB.close</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#open()">oracle.sql.CLOB.close</a></li>
	 * </ul>
	 */
	public void setReleaseResourcesAfterRead(boolean releaseResources) {
		this.releaseResourcesAfterRead = releaseResources;
	}


	/**
	 * 通过反射检索{@code oracle.sql.BLOB}和{@code oracle.sql.CLOB}类,
	 * 并初始化在那里定义的DURATION_SESSION, MODE_READWRITE 和 MODE_READONLY常量的值.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#DURATION_SESSION">oracle.sql.BLOB.DURATION_SESSION</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#MODE_READWRITE">oracle.sql.BLOB.MODE_READWRITE</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#MODE_READONLY">oracle.sql.BLOB.MODE_READONLY</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#DURATION_SESSION">oracle.sql.CLOB.DURATION_SESSION</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#MODE_READWRITE">oracle.sql.CLOB.MODE_READWRITE</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#MODE_READONLY">oracle.sql.CLOB.MODE_READONLY</a></li>
	 * </ul>
	 * 
	 * @param con Oracle Connection, 用于使用与加载Oracle驱动程序完全相同的类加载器
	 */
	protected synchronized void initOracleDriverClasses(Connection con) {
		if (this.blobClass == null) {
			try {
				// Initialize oracle.sql.BLOB class
				this.blobClass = con.getClass().getClassLoader().loadClass(BLOB_CLASS_NAME);
				this.durationSessionConstants.put(
						this.blobClass, this.blobClass.getField(DURATION_SESSION_FIELD_NAME).getInt(null));
				this.modeReadWriteConstants.put(
						this.blobClass, this.blobClass.getField(MODE_READWRITE_FIELD_NAME).getInt(null));
				this.modeReadOnlyConstants.put(
						this.blobClass, this.blobClass.getField(MODE_READONLY_FIELD_NAME).getInt(null));

				// Initialize oracle.sql.CLOB class
				this.clobClass = con.getClass().getClassLoader().loadClass(CLOB_CLASS_NAME);
				this.durationSessionConstants.put(
						this.clobClass, this.clobClass.getField(DURATION_SESSION_FIELD_NAME).getInt(null));
				this.modeReadWriteConstants.put(
						this.clobClass, this.clobClass.getField(MODE_READWRITE_FIELD_NAME).getInt(null));
				this.modeReadOnlyConstants.put(
						this.clobClass, this.clobClass.getField(MODE_READONLY_FIELD_NAME).getInt(null));
			}
			catch (Exception ex) {
				throw new InvalidDataAccessApiUsageException(
						"Couldn't initialize OracleLobHandler because Oracle driver classes are not available. " +
						"Note that OracleLobHandler requires Oracle JDBC driver 9i or higher!", ex);
			}
		}
	}


	@Override
	public byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle BLOB as bytes");
		Blob blob = rs.getBlob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), blob);
		byte[] retVal = (blob != null ? blob.getBytes(1, (int) blob.length()) : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), blob);
		return retVal;
	}

	@Override
	public InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle BLOB as binary stream");
		Blob blob = rs.getBlob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), blob);
		InputStream retVal = (blob != null ? blob.getBinaryStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), blob);
		return retVal;
	}

	@Override
	public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as string");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		String retVal = (clob != null ? clob.getSubString(1, (int) clob.length()) : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	@Override
	public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as ASCII stream");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		InputStream retVal = (clob != null ? clob.getAsciiStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	@Override
	public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as character stream");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		Reader retVal = (clob != null ? clob.getCharacterStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	@Override
	public LobCreator getLobCreator() {
		return new OracleLobCreator();
	}

	/**
	 * 在读取完成之前初始化任何LOB资源.
	 * <p>如果{@code releaseResourcesAfterRead}属性设置为{@code true},
	 * 则此实现会在任何非临时LOB上调用{@code BLOB.open(BLOB.MODE_READONLY)}或{@code CLOB.open(CLOB.MODE_READONLY)}.
	 * <p>如果需要不同的行为, 则子方法可以覆盖此方法.
	 * 
	 * @param con 用于初始化的连接
	 * @param lob 要初始化的LOB
	 */
	protected void initializeResourcesBeforeRead(Connection con, Object lob) {
		if (this.releaseResourcesAfterRead) {
			initOracleDriverClasses(con);
			try {
				/*
				if (!((BLOB) lob.isTemporary() {
				*/
				Method isTemporary = lob.getClass().getMethod("isTemporary");
				Boolean temporary = (Boolean) isTemporary.invoke(lob);
				if (!temporary) {
					/*
					((BLOB) lob).open(BLOB.MODE_READONLY);
					*/
					Method open = lob.getClass().getMethod("open", int.class);
					open.invoke(lob, this.modeReadOnlyConstants.get(lob.getClass()));
				}
			}
			catch (InvocationTargetException ex) {
				logger.error("Could not open Oracle LOB", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not open Oracle LOB", ex);
			}
		}
	}

	/**
	 * 读取完成后释放任何LOB资源.
	 * <p>如果{@code releaseResourcesAfterRead}属性设置为{@code true},
	 * 则此实现在任何打开的非临时LOB上调用{@code BLOB.close()}或{@code CLOB.close()},
	 * 或在临时LOB上调用 {@code BLOB.freeTemporary()}或{@code CLOB.freeTemporary()}.
	 * <p>如果需要不同的行为, 则子方法可以覆盖此方法.
	 * 
	 * @param con 用于初始化的连接
	 * @param lob 要初始化的LOB
	 */
	protected void releaseResourcesAfterRead(Connection con, Object lob) {
		if (this.releaseResourcesAfterRead) {
			initOracleDriverClasses(con);
			Boolean temporary = Boolean.FALSE;
			try {
				/*
				if (((BLOB) lob.isTemporary() {
				*/
				Method isTemporary = lob.getClass().getMethod("isTemporary");
				temporary = (Boolean) isTemporary.invoke(lob);
				if (temporary) {
					/*
					((BLOB) lob).freeTemporary();
					*/
					Method freeTemporary = lob.getClass().getMethod("freeTemporary");
					freeTemporary.invoke(lob);
				}
				else {
					/*
					if (((BLOB) lob.isOpen() {
					*/
					Method isOpen = lob.getClass().getMethod("isOpen");
					Boolean open = (Boolean) isOpen.invoke(lob);
					if (open) {
						/*
						((BLOB) lob).close();
						*/
						Method close = lob.getClass().getMethod("close");
						close.invoke(lob);
					}
				}
			}
			catch (InvocationTargetException ex) {
				if (temporary) {
					logger.error("Could not free Oracle LOB", ex.getTargetException());
				}
				else {
					logger.error("Could not close Oracle LOB", ex.getTargetException());
				}
			}
			catch (Exception ex) {
				if (temporary) {
					throw new DataAccessResourceFailureException("Could not free Oracle LOB", ex);
				}
				else {
					throw new DataAccessResourceFailureException("Could not close Oracle LOB", ex);
				}
			}
		}
	}


	/**
	 * 适用于Oracle数据库的LobCreator实现.
	 * 创建在关闭时释放的Oracle样式的临时BLOB和CLOB.
	 */
	protected class OracleLobCreator implements LobCreator {

		private final List<Object> temporaryLobs = new LinkedList<Object>();

		@Override
		public void setBlobAsBytes(PreparedStatement ps, int paramIndex, final byte[] content)
				throws SQLException {

			if (content != null) {
				Blob blob = (Blob) createLob(ps, false, new LobCallback() {
					@Override
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getBinaryOutputStream");
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob);
						FileCopyUtils.copy(content, out);
					}
				});
				ps.setBlob(paramIndex, blob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set bytes for Oracle BLOB with length " + blob.length());
				}
			}
			else {
				ps.setBlob(paramIndex, (Blob) null);
				logger.debug("Set Oracle BLOB to null");
			}
		}

		@Override
		public void setBlobAsBinaryStream(
				PreparedStatement ps, int paramIndex, final InputStream binaryStream, int contentLength)
				throws SQLException {

			if (binaryStream != null) {
				Blob blob = (Blob) createLob(ps, false, new LobCallback() {
					@Override
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getBinaryOutputStream", (Class[]) null);
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(binaryStream, out);
					}
				});
				ps.setBlob(paramIndex, blob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set binary stream for Oracle BLOB with length " + blob.length());
				}
			}
			else {
				ps.setBlob(paramIndex, (Blob) null);
				logger.debug("Set Oracle BLOB to null");
			}
		}

		@Override
		public void setClobAsString(PreparedStatement ps, int paramIndex, final String content)
			throws SQLException {

			if (content != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					@Override
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getCharacterOutputStream", (Class[]) null);
						Writer writer = (Writer) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(content, writer);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set string for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		@Override
		public void setClobAsAsciiStream(
				PreparedStatement ps, int paramIndex, final InputStream asciiStream, int contentLength)
			throws SQLException {

			if (asciiStream != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					@Override
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getAsciiOutputStream", (Class[]) null);
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(asciiStream, out);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set ASCII stream for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		@Override
		public void setClobAsCharacterStream(
				PreparedStatement ps, int paramIndex, final Reader characterStream, int contentLength)
			throws SQLException {

			if (characterStream != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					@Override
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getCharacterOutputStream", (Class[]) null);
						Writer writer = (Writer) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(characterStream, writer);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set character stream for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		/**
		 * 为给定的PreparedStatement创建一个LOB实例, 通过给定的回调填充它.
		 */
		protected Object createLob(PreparedStatement ps, boolean clob, LobCallback callback)
				throws SQLException {

			Connection con = null;
			try {
				con = getOracleConnection(ps);
				initOracleDriverClasses(con);
				Object lob = prepareLob(con, clob ? clobClass : blobClass);
				callback.populateLob(lob);
				lob.getClass().getMethod("close", (Class[]) null).invoke(lob, (Object[]) null);
				this.temporaryLobs.add(lob);
				if (logger.isDebugEnabled()) {
					logger.debug("Created new Oracle " + (clob ? "CLOB" : "BLOB"));
				}
				return lob;
			}
			catch (SQLException ex) {
				throw ex;
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof SQLException) {
					throw (SQLException) ex.getTargetException();
				}
				else if (con != null && ex.getTargetException() instanceof ClassCastException) {
					throw new InvalidDataAccessApiUsageException(
							"OracleLobCreator needs to work on [oracle.jdbc.OracleConnection], not on [" +
							con.getClass().getName() + "]: specify a corresponding NativeJdbcExtractor",
							ex.getTargetException());
				}
				else {
					throw new DataAccessResourceFailureException("Could not create Oracle LOB",
							ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not create Oracle LOB", ex);
			}
		}

		/**
		 * 如果设置, 则使用NativeJdbcExtractor检索底层OracleConnection.
		 */
		protected Connection getOracleConnection(PreparedStatement ps)
				throws SQLException, ClassNotFoundException {

			return (nativeJdbcExtractor != null ?
					nativeJdbcExtractor.getNativeConnectionFromStatement(ps) : ps.getConnection());
		}

		/**
		 * 通过反射创建并打开oracle.sql.BLOB/CLOB实例.
		 */
		protected Object prepareLob(Connection con, Class<?> lobClass) throws Exception {
			/*
			BLOB blob = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
			blob.open(BLOB.MODE_READWRITE);
			return blob;
			*/
			Method createTemporary = lobClass.getMethod(
					"createTemporary", Connection.class, boolean.class, int.class);
			Object lob = createTemporary.invoke(null, con, cache, durationSessionConstants.get(lobClass));
			Method open = lobClass.getMethod("open", int.class);
			open.invoke(lob, modeReadWriteConstants.get(lobClass));
			return lob;
		}

		/**
		 * 释放此创建者创建的所有临时BLOB和CLOB.
		 */
		@Override
		public void close() {
			try {
				for (Iterator<?> it = this.temporaryLobs.iterator(); it.hasNext();) {
					/*
					BLOB blob = (BLOB) it.next();
					blob.freeTemporary();
					*/
					Object lob = it.next();
					Method freeTemporary = lob.getClass().getMethod("freeTemporary");
					freeTemporary.invoke(lob);
					it.remove();
				}
			}
			catch (InvocationTargetException ex) {
				logger.error("Could not free Oracle LOB", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not free Oracle LOB", ex);
			}
		}
	}


	/**
	 * 用于createLob的内部回调接口.
	 */
	protected interface LobCallback {

		/**
		 * 使用内容填充给定的BLOB或CLOB实例.
		 * 
		 * @throws Exception 任何异常, 包括InvocationTargetException
		 */
		void populateLob(Object lob) throws Exception;
	}
}
