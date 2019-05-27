package org.springframework.jdbc.core.metadata;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.ReflectionUtils;

/**
 * 特定于Oracle的{@link org.springframework.jdbc.core.metadata.TableMetaDataProvider}的实现.
 * 支持在元数据查找中包含同义词的功能. 还支持使用{@code sys_context}查找当前schema.
 *
 * <p>Thanks to Mike Youngstrom and Bruce Campbell for submitting the original suggestion for the Oracle current schema lookup implementation.
 */
public class OracleTableMetaDataProvider extends GenericTableMetaDataProvider {

	private final boolean includeSynonyms;

	private String defaultSchema;


	/**
	 * @param databaseMetaData 要使用的元数据
	 */
	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		this(databaseMetaData, false);
	}

	/**
	 * @param databaseMetaData 要使用的元数据
	 * @param includeSynonyms 是否包括同义词
	 */
	public OracleTableMetaDataProvider(DatabaseMetaData databaseMetaData, boolean includeSynonyms)
			throws SQLException {

		super(databaseMetaData);
		this.includeSynonyms = includeSynonyms;
		this.defaultSchema = lookupDefaultSchema(databaseMetaData);
	}


	/*
	 * 用于检测当前schema的基于Oracle的实现.
	 */
	private static String lookupDefaultSchema(DatabaseMetaData databaseMetaData) {
		try {
			CallableStatement cstmt = null;
			try {
				Connection con = databaseMetaData.getConnection();
				if (con == null) {
					logger.debug("Cannot check default schema - no Connection from DatabaseMetaData");
					return null;
				}
				cstmt = con.prepareCall("{? = call sys_context('USERENV', 'CURRENT_SCHEMA')}");
				cstmt.registerOutParameter(1, Types.VARCHAR);
				cstmt.execute();
				return cstmt.getString(1);
			}
			finally {
				if (cstmt != null) {
					cstmt.close();
				}
			}
		}
		catch (SQLException ex) {
			logger.debug("Exception encountered during default schema lookup", ex);
			return null;
		}
	}

	@Override
	protected String getDefaultSchema() {
		if (this.defaultSchema != null) {
			return defaultSchema;
		}
		return super.getDefaultSchema();
	}


	@Override
	public void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData,
			String catalogName, String schemaName, String tableName) throws SQLException {

		if (!this.includeSynonyms) {
			logger.debug("Defaulting to no synonyms in table meta-data lookup");
			super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
			return;
		}

		Connection con = databaseMetaData.getConnection();
		if (con == null) {
			logger.warn("Unable to include synonyms in table meta-data lookup - no Connection from DatabaseMetaData");
			super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
			return;
		}

		NativeJdbcExtractor nativeJdbcExtractor = getNativeJdbcExtractor();
		if (nativeJdbcExtractor != null) {
			con = nativeJdbcExtractor.getNativeConnection(con);
		}

		boolean isOracleCon = false;
		try {
			Class<?> oracleConClass = con.getClass().getClassLoader().loadClass("oracle.jdbc.OracleConnection");
			isOracleCon = oracleConClass.isInstance(con);
			if (!isOracleCon) {
				con = (Connection) con.unwrap(oracleConClass);
				isOracleCon = oracleConClass.isInstance(con);
			}
		}
		catch (ClassNotFoundException ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not find Oracle JDBC API: " + ex);
			}
		}
		catch (SQLException ex) {
			// No OracleConnection found by unwrap
		}

		if (!isOracleCon) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unable to include synonyms in table meta-data lookup - no Oracle Connection: " + con);
			}
			super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);
			return;
		}

		logger.debug("Including synonyms in table meta-data lookup");
		Method setIncludeSynonyms;
		Boolean originalValueForIncludeSynonyms;

		try {
			Method getIncludeSynonyms = con.getClass().getMethod("getIncludeSynonyms");
			ReflectionUtils.makeAccessible(getIncludeSynonyms);
			originalValueForIncludeSynonyms = (Boolean) getIncludeSynonyms.invoke(con);

			setIncludeSynonyms = con.getClass().getMethod("setIncludeSynonyms", boolean.class);
			ReflectionUtils.makeAccessible(setIncludeSynonyms);
			setIncludeSynonyms.invoke(con, Boolean.TRUE);
		}
		catch (Throwable ex) {
			throw new InvalidDataAccessApiUsageException("Could not prepare Oracle Connection", ex);
		}

		super.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName, tableName);

		try {
			setIncludeSynonyms.invoke(con, originalValueForIncludeSynonyms);
		}
		catch (Throwable ex) {
			throw new InvalidDataAccessApiUsageException("Could not reset Oracle Connection", ex);
		}
	}

}
