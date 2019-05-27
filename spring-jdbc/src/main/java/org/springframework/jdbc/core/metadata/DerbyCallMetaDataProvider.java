package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider}接口的Derby特定实现.
 * 此类供Simple JDBC类内部使用.
 */
public class DerbyCallMetaDataProvider extends GenericCallMetaDataProvider {

	public DerbyCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public String metaDataSchemaNameToUse(String schemaName) {
		if (schemaName != null) {
			return super.metaDataSchemaNameToUse(schemaName);
		}

		// Use current user schema if no schema specified...
		String userName = getUserName();
		return (userName != null ? userName.toUpperCase() : null);
	}

}
