package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link TableMetaDataProvider}的HSQL特定实现.
 * 支持在没有JDBC 3.0 {@code getGeneratedKeys}支持的情况下检索生成的键的功能.
 */
public class HsqlTableMetaDataProvider extends GenericTableMetaDataProvider {

	public HsqlTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public boolean isGetGeneratedKeysSimulated() {
		return true;
	}

	@Override
	public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return "select max(identity()) from " + tableName;
	}

}
