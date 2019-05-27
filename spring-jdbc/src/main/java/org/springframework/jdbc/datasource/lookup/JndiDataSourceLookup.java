package org.springframework.jdbc.datasource.lookup;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jndi.JndiLocatorSupport;

/**
 * 基于JNDI的{@link DataSourceLookup}实现.
 *
 * <p>对于特定的JNDI配置, 建议配置"jndiEnvironment"/"jndiTemplate" 属性.
 */
public class JndiDataSourceLookup extends JndiLocatorSupport implements DataSourceLookup {

	public JndiDataSourceLookup() {
		setResourceRef(true);
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
		try {
			return lookup(dataSourceName, DataSource.class);
		}
		catch (NamingException ex) {
			throw new DataSourceLookupFailureException(
					"Failed to look up JNDI DataSource with name '" + dataSourceName + "'", ex);
		}
	}
}
