package org.springframework.orm.hibernate4;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;
import org.hibernate.service.Service;

import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Implementation of Hibernate 4's JtaPlatform SPI (which has a different package
 * location in Hibernate 4.0-4.2 vs 4.3), exposing passed-in {@link TransactionManager},
 * {@link UserTransaction} and {@link TransactionSynchronizationRegistry} references.
 */
@SuppressWarnings({"serial", "unchecked"})
class ConfigurableJtaPlatform implements InvocationHandler {

	static final Class<? extends Service> jtaPlatformClass;

	static {
		Class<?> jpClass;
		try {
			// Try Hibernate 4.0-4.2 JtaPlatform variant
			jpClass = ClassUtils.forName("org.hibernate.service.jta.platform.spi.JtaPlatform",
					ConfigurableJtaPlatform.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			try {
				// Try Hibernate 4.3 JtaPlatform variant
				jpClass = ClassUtils.forName("org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform",
						ConfigurableJtaPlatform.class.getClassLoader());
			}
			catch (ClassNotFoundException ex2) {
				throw new IllegalStateException("Neither Hibernate 4.0-4.2 nor 4.3 variant of JtaPlatform found");
			}
		}
		jtaPlatformClass = (Class<? extends Service>) jpClass;
	}

	static String getJtaPlatformBasePackage() {
		String className = jtaPlatformClass.getName();
		return className.substring(0, className.length() - "spi.JtaPlatform".length());
	}


	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;

	private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * Create a new ConfigurableJtaPlatform instance with the given
	 * JTA TransactionManager and optionally a given UserTransaction.
	 * @param tm the JTA TransactionManager reference (required)
	 * @param ut the JTA UserTransaction reference (optional)
	 * @param tsr the JTA 1.1 TransactionSynchronizationRegistry (optional)
	 */
	public ConfigurableJtaPlatform(TransactionManager tm, UserTransaction ut, TransactionSynchronizationRegistry tsr) {
		Assert.notNull(tm, "TransactionManager reference must not be null");
		this.transactionManager = tm;
		this.userTransaction = (ut != null ? ut : new UserTransactionAdapter(tm));
		this.transactionSynchronizationRegistry = tsr;
	}


	public TransactionManager retrieveTransactionManager() {
		return this.transactionManager;
	}

	public UserTransaction retrieveUserTransaction() {
		return this.userTransaction;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

	public boolean canRegisterSynchronization() {
		try {
			return (this.transactionManager.getStatus() == Status.STATUS_ACTIVE);
		}
		catch (SystemException ex) {
			throw new TransactionException("Could not determine JTA transaction status", ex);
		}
	}

	public void registerSynchronization(Synchronization synchronization) {
		if (this.transactionSynchronizationRegistry != null) {
			this.transactionSynchronizationRegistry.registerInterposedSynchronization(synchronization);
		}
		else {
			try {
				this.transactionManager.getTransaction().registerSynchronization(synchronization);
			}
			catch (Exception ex) {
				throw new TransactionException("Could not access JTA Transaction to register synchronization", ex);
			}
		}
	}

	public int getCurrentStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(this, args);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to delegate to corresponding implementation method", ex);
		}
	}

	/**
	 * Obtain a proxy that implements the current Hibernate version's JtaPlatform interface
	 * in the right package location, delegating all invocations to the same-named methods
	 * on this ConfigurableJtaPlatform class itself.
	 */
	public Object getJtaPlatformProxy() {
		return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {jtaPlatformClass}, this);
	}

}
