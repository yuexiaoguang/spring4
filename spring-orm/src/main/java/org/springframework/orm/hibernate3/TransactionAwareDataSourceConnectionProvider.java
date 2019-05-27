package org.springframework.orm.hibernate3;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Subclass of LocalDataSourceConnectionProvider that returns a
 * transaction-aware proxy for the exposed DataSource. Used if
 * LocalSessionFactoryBean's "useTransactionAwareDataSource" flag is on.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class TransactionAwareDataSourceConnectionProvider extends LocalDataSourceConnectionProvider {

	/**
	 * Return a TransactionAwareDataSourceProxy for the given DataSource,
	 * provided that it isn't a TransactionAwareDataSourceProxy already.
	 */
	@Override
	protected DataSource getDataSourceToUse(DataSource originalDataSource) {
		if (originalDataSource instanceof TransactionAwareDataSourceProxy) {
			return originalDataSource;
		}
		return new TransactionAwareDataSourceProxy(originalDataSource);
	}

	/**
	 * This implementation returns {@code true}: We can guarantee
	 * to receive the same Connection within a transaction, as we are
	 * exposing a TransactionAwareDataSourceProxy.
	 */
	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

}
