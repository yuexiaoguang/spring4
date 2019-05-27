package org.springframework.orm.hibernate3;

/**
 * Subclass of LocalDataSourceConnectionProvider that will be used
 * if LocalSessionFactoryBean's "dataSource" property is set
 * in combination with a Hibernate TransactionManagerLookup.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class LocalJtaDataSourceConnectionProvider extends LocalDataSourceConnectionProvider {

	/**
	 * This implementation returns {@code true},
	 * since we're assuming a JTA DataSource.
	 */
	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

}
