package org.springframework.jca.cci.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

/**
 * CCI ConnectionFactory适配器, 它在所有{@code getConnection}调用上返回相同的Connection,
 * 并忽略对{@code Connection.close()}的调用.
 *
 * <p>对于测试和独立环境很有用, 为多个CciTemplate调用保持使用相同的Connection,
 * 而不需要池化ConnectionFactory, 也可以跨越任意数量的事务.
 *
 * <p>可以直接传入CCI连接, 也可以让工厂通过给定的目标ConnectionFactory延迟创建连接.
 */
@SuppressWarnings("serial")
public class SingleConnectionFactory extends DelegatingConnectionFactory implements DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	/** 包装的Connection */
	private Connection target;

	/** Proxy Connection */
	private Connection connection;

	/** 共享Connection的同步监视器 */
	private final Object connectionMonitor = new Object();


	public SingleConnectionFactory() {
	}

	/**
	 * 总是返回给定的Connection.
	 * 
	 * @param target 单个Connection
	 */
	public SingleConnectionFactory(Connection target) {
		Assert.notNull(target, "Target Connection must not be null");
		this.target = target;
		this.connection = getCloseSuppressingConnectionProxy(target);
	}

	/**
	 * 它始终返回一个Connection, 该连接将通过给定的目标ConnectionFactory延迟地创建.
	 * 
	 * @param targetConnectionFactory 目标ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		setTargetConnectionFactory(targetConnectionFactory);
	}


	/**
	 * 确保已设置Connection或ConnectionFactory.
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Connection or 'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection getConnection() throws ResourceException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	@Override
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
		throw new NotSupportedException(
				"SingleConnectionFactory does not support custom ConnectionSpec");
	}

	/**
	 * 关闭底层Connection.
	 * ConnectionFactory的提供者需要关心正确的关闭.
	 * <p>当这个bean实现DisposableBean时, bean工厂会在销毁其缓存的单例时自动调用它.
	 */
	@Override
	public void destroy() {
		resetConnection();
	}


	/**
	 * 初始化单个底层连接.
	 * <p>如果已存在底层连接, 则关闭并重新初始化Connection.
	 * 
	 * @throws javax.resource.ResourceException 如果由CCI API方法抛出
	 */
	public void initConnection() throws ResourceException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException(
					"'targetConnectionFactory' is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = doCreateConnection();
			prepareConnection(this.target);
			if (logger.isInfoEnabled()) {
				logger.info("Established shared CCI Connection: " + this.target);
			}
			this.connection = getCloseSuppressingConnectionProxy(this.target);
		}
	}

	/**
	 * 重置底层共享连接, 以便在下次访问时重新初始化.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * 通过此模板的ConnectionFactory创建CCI连接.
	 * 
	 * @return 新的CCI Connection
	 * @throws javax.resource.ResourceException 如果由CCI API方法抛出
	 */
	protected Connection doCreateConnection() throws ResourceException {
		return getTargetConnectionFactory().getConnection();
	}

	/**
	 * 在给定连接暴露之前准备它.
	 * <p>默认实现为空. 可以在子类中重写.
	 * 
	 * @param con 要准备的Connection
	 */
	protected void prepareConnection(Connection con) throws ResourceException {
	}

	/**
	 * 关闭给定的Connection.
	 * 
	 * @param con 要关闭的Connection
	 */
	protected void closeConnection(Connection con) {
		try {
			con.close();
		}
		catch (Throwable ex) {
			logger.warn("Could not close shared CCI Connection", ex);
		}
	}

	/**
	 * 使用代理来包装给定的Connection, 该代理将每个方法调用委托给它, 但禁止关闭调用.
	 * 这对于允许应用程序代码处理特殊框架Connection非常有用, 就像来自CCI ConnectionFactory的普通Connection一样.
	 * 
	 * @param target 要包装的原始Connection
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new CloseSuppressingInvocationHandler(target));
	}


	/**
	 * 禁止CCI连接上的close调用的调用处理器.
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		private CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 不要通过调用.
				return null;
			}
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}
}
