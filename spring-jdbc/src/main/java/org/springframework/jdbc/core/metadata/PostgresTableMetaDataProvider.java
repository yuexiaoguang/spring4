package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link TableMetaDataProvider}的PostgreSQL特定实现.
 * 支持在没有JDBC 3.0 {@code getGeneratedKeys}支持的情况下检索生成的键的功能.
 */
public class PostgresTableMetaDataProvider extends GenericTableMetaDataProvider {

	public PostgresTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isGetGeneratedKeysSimulated() {
		return true;
	}

	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "RETURNING " + keyColumnName;
	}

}
