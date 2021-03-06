package org.springframework.http.client.support;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean}, 创建{@link Proxy java.net.Proxy}.
 */
public class ProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

	private Proxy.Type type = Proxy.Type.HTTP;

	private String hostname;

	private int port = -1;

	private Proxy proxy;


	/**
	 * 设置代理类型.
	 * <p>默认{@link java.net.Proxy.Type#HTTP}.
	 */
	public void setType(Proxy.Type type) {
		this.type = type;
	}

	/**
	 * 设置代理主机名.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * 设置代理端口.
	 */
	public void setPort(int port) {
		this.port = port;
	}


	@Override
	public void afterPropertiesSet() throws IllegalArgumentException {
		Assert.notNull(this.type, "'type' must not be null");
		Assert.hasLength(this.hostname, "'hostname' must not be empty");
		if (this.port < 0 || this.port > 65535) {
			throw new IllegalArgumentException("'port' value out of range: " + this.port);
		}

		SocketAddress socketAddress = new InetSocketAddress(this.hostname, this.port);
		this.proxy = new Proxy(this.type, socketAddress);
	}


	@Override
	public Proxy getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return Proxy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
