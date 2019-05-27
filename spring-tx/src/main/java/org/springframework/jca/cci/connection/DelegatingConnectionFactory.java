package org.springframework.jca.cci.connection;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.springframework.beans.factory.InitializingBean;

/**
 * CCI {@link ConnectionFactory} implementation that delegates all calls
 * to a given target {@link ConnectionFactory}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target {@link ConnectionFactory}.
 */
@SuppressWarnings("serial")
public class DelegatingConnectionFactory implements ConnectionFactory, InitializingBean {

	private ConnectionFactory targetConnectionFactory;


	/**
	 * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}


	@Override
	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection getConnection() throws ResourceException {
		return getTargetConnectionFactory().getConnection();
	}

	@Override
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
		return getTargetConnectionFactory().getConnection(connectionSpec);
	}

	@Override
	public RecordFactory getRecordFactory() throws ResourceException {
		return getTargetConnectionFactory().getRecordFactory();
	}

	@Override
	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		return getTargetConnectionFactory().getMetaData();
	}

	@Override
	public Reference getReference() throws NamingException {
		return getTargetConnectionFactory().getReference();
	}

	@Override
	public void setReference(Reference reference) {
		getTargetConnectionFactory().setReference(reference);
	}

}
