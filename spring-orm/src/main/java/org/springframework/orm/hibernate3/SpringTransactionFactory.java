package org.springframework.orm.hibernate3;

import java.util.Properties;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.JDBCTransaction;
import org.hibernate.transaction.TransactionFactory;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring-aware implementation of the Hibernate TransactionFactory interface, aware of
 * Spring-synchronized transactions (in particular Spring-managed JTA transactions)
 * and asking for default release mode ON_CLOSE. Otherwise identical to Hibernate's
 * default {@link org.hibernate.transaction.JDBCTransactionFactory} implementation.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class SpringTransactionFactory implements TransactionFactory {

	/**
	 * Sets connection release mode "on_close" as default.
	 * <p>This was the case for Hibernate 3.0; Hibernate 3.1 changed
	 * it to "auto" (i.e. "after_statement" or "after_transaction").
	 * However, for Spring's resource management (in particular for
	 * HibernateTransactionManager), "on_close" is the better default.
	 */
	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}

	@Override
	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext) {
		return new JDBCTransaction(jdbcContext, transactionContext);
	}

	@Override
	public void configure(Properties props) {
	}

	@Override
	public boolean isTransactionManagerRequired() {
		return false;
	}

	@Override
	public boolean areCallbacksLocalToHibernateTransactions() {
		return true;
	}

	@Override
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext, Context transactionContext, Transaction transaction) {

		return (transaction != null && transaction.isActive()) ||
				TransactionSynchronizationManager.isActualTransactionActive();
	}

}
