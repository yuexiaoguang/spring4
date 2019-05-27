package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider}接口的SAP HANA特定实现.
 * 此类供Simple JDBC类内部使用.
 */
public class HanaCallMetaDataProvider extends GenericCallMetaDataProvider {

	public HanaCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		super.initializeWithMetaData(databaseMetaData);
		setStoresUpperCaseIdentifiers(false);
	}

}
