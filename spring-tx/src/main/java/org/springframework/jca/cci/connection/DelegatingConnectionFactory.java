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
 * CCI {@link ConnectionFactory}实现, 它将所有调用委托给给定目标{@link ConnectionFactory}.
 *
 * <p>这个类是子类化的, 子类只覆盖那些不应该简单地委托给目标{@link ConnectionFactory}的方法(例如{@link #getConnection()}).
 */
@SuppressWarnings("serial")
public class DelegatingConnectionFactory implements ConnectionFactory, InitializingBean {

	private ConnectionFactory targetConnectionFactory;


	/**
	 * 设置此ConnectionFactory应委托给的目标ConnectionFactory.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此ConnectionFactory应委托给的目标ConnectionFactory.
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
