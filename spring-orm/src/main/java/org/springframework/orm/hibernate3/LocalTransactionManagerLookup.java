package org.springframework.orm.hibernate3;

import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Implementation of Hibernate's {@link TransactionManagerLookup} interface
 * that returns a Spring-managed JTA {@link TransactionManager}, determined
 * by LocalSessionFactoryBean's "jtaTransactionManager" property.
 *
 * <p>The main advantage of this TransactionManagerLookup is that it avoids
 * double configuration of JTA specifics. A single TransactionManager bean can
 * be used for both JtaTransactionManager and LocalSessionFactoryBean, with no
 * JTA setup in Hibernate configuration.
 *
 * <p>Alternatively, use Hibernate's own TransactionManagerLookup implementations:
 * Spring's JtaTransactionManager only requires a TransactionManager for suspending
 * and resuming transactions, so you might not need to apply such special Spring
 * configuration at all.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class LocalTransactionManagerLookup implements TransactionManagerLookup {

	private final TransactionManager transactionManager;


	public LocalTransactionManagerLookup() {
		TransactionManager tm = LocalSessionFactoryBean.getConfigTimeTransactionManager();
		// absolutely needs thread-bound TransactionManager to initialize
		if (tm == null) {
			throw new IllegalStateException("No JTA TransactionManager found - " +
				"'jtaTransactionManager' property must be set on LocalSessionFactoryBean");
		}
		this.transactionManager = tm;
	}

	@Override
	public TransactionManager getTransactionManager(Properties props) {
		return this.transactionManager;
	}

	@Override
	public String getUserTransactionName() {
		return null;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

}
