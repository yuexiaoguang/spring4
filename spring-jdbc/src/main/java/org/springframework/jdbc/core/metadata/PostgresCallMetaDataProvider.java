package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

/**
 * {@link CallMetaDataProvider}接口的Postgres特定实现.
 * 此类供Simple JDBC类内部使用.
 */
public class PostgresCallMetaDataProvider extends GenericCallMetaDataProvider {

	private static final String RETURN_VALUE_NAME = "returnValue";


	public PostgresCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isReturnResultSetSupported() {
		return false;
	}

	@Override
	public boolean isRefCursorSupported() {
		return true;
	}

	@Override
	public int getRefCursorSqlType() {
		return Types.OTHER;
	}

	@Override
	public String metaDataSchemaNameToUse(String schemaName) {
		// Use public schema if no schema specified
		return (schemaName == null ? "public" : super.metaDataSchemaNameToUse(schemaName));
	}

	@Override
	public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
		if (meta.getSqlType() == Types.OTHER && "refcursor".equals(meta.getTypeName())) {
			return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
		}
		else {
			return super.createDefaultOutParameter(parameterName, meta);
		}
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return RETURN_VALUE_NAME.equals(parameterName);
	}

}
