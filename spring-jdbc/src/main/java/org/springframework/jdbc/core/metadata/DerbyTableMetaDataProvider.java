package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link TableMetaDataProvider}的Derby特定实现.
 * 覆盖有关检索生成的键的Derby元数据信息.
 */
public class DerbyTableMetaDataProvider extends GenericTableMetaDataProvider {

	private boolean supportsGeneratedKeysOverride = false;


	public DerbyTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
		super.initializeWithMetaData(databaseMetaData);
		if (!databaseMetaData.supportsGetGeneratedKeys()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Overriding supportsGetGeneratedKeys from DatabaseMetaData to 'true'; it was reported as " +
						"'false' by " + databaseMetaData.getDriverName() + " " + databaseMetaData.getDriverVersion());
			}
			this.supportsGeneratedKeysOverride = true;
		}
	}

	@Override
	public boolean isGetGeneratedKeysSupported() {
		return (super.isGetGeneratedKeysSupported() || this.supportsGeneratedKeysOverride);
	}

}
