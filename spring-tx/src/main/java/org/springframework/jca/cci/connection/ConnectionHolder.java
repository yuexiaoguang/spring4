package org.springframework.jca.cci.connection;

import javax.resource.cci.Connection;

import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * 连接保存器, 包装CCI连接.
 *
 * <p>对于给定的ConnectionFactory, CciLocalTransactionManager将此类的实例绑定到线程.
 *
 * <p>Note: 这是一个SPI类, 不适合应用程序使用.
 */
public class ConnectionHolder extends ResourceHolderSupport {

	private final Connection connection;

	public ConnectionHolder(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return this.connection;
	}

}
