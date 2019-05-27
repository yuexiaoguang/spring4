package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * {@link CallMetaDataProvider}接口的Sybase特定实现.
 * 此类供Simple JDBC类内部使用.
 */
public class SybaseCallMetaDataProvider extends GenericCallMetaDataProvider {

	private static final String REMOVABLE_COLUMN_PREFIX = "@";

	private static final String RETURN_VALUE_NAME = "RETURN_VALUE";


	public SybaseCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);
	}


	@Override
	public String parameterNameToUse(String parameterName) {
		if (parameterName == null) {
			return null;
		}
		else if (parameterName.length() > 1 && parameterName.startsWith(REMOVABLE_COLUMN_PREFIX)) {
			return super.parameterNameToUse(parameterName.substring(1));
		}
		else {
			return super.parameterNameToUse(parameterName);
		}
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return (RETURN_VALUE_NAME.equals(parameterName) ||
				RETURN_VALUE_NAME.equals(parameterNameToUse(parameterName)) ||
				super.byPassReturnParameter(parameterName));
	}

}
