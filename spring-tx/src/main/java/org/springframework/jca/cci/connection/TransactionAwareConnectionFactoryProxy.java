package org.springframework.jca.cci.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

/**
 * 目标CCI {@link javax.resource.cci.ConnectionFactory}的代理, 增加了对Spring管理的事务的认识.
 * 类似于J2EE服务器提供的事务性JNDI ConnectionFactory.
 *
 * <p>应该仍然不知道Spring的数据访问支持的数据访问代码可以使用此代理无缝地参与Spring管理的事务.
 * 请注意, 事务管理器, 例如{@link CciLocalTransactionManager},
 * 仍然需要使用底层ConnectionFactory, <i>而不是</i>和此代理一起使用.
 *
 * <p><b>确保TransactionAwareConnectionFactoryProxy是ConnectionFactory代理/适配器链的最外层ConnectionFactory.</b>
 * TransactionAwareConnectionFactoryProxy可以直接委托给目标连接池或某些中间代理/适配器,
 * 如{@link ConnectionSpecConnectionFactoryAdapter}.
 *
 * <p>委托给{@link ConnectionFactoryUtils}自动参与线程绑定事务, 例如由{@link CciLocalTransactionManager}管理.
 * 返回的连接上的{@code getConnection}调用和{@code close}调用将在事务中正常运行, i.e. 始终在事务连接上运行.
 * 如果不在事务中, 则应用正常的ConnectionFactory行为.
 *
 * <p>此代理允许数据访问代码与普通JCA CCI API一起使用, 并且仍然参与Spring管理的事务,
 * 类似于J2EE / JTA环境中的CCI代码.
 * 但是, 如果可能的话, 使用Spring的ConnectionFactoryUtils, CciTemplate 或CCI操作对象,
 * 即使没有目标ConnectionFactory的代理也可以获得事务参与, 从而避免首先需要定义这样的代理.
 *
 * <p><b>NOTE:</b> 此ConnectionFactory代理需要返回包装的Connections, 以便正确处理关闭调用.
 * 因此, 返回的Connections无法强制转换为本机CCI连接类型或连接池实现类型.
 */
@SuppressWarnings("serial")
public class TransactionAwareConnectionFactoryProxy extends DelegatingConnectionFactory {

	public TransactionAwareConnectionFactoryProxy() {
	}

	/**
	 * @param targetConnectionFactory 目标ConnectionFactory
	 */
	public TransactionAwareConnectionFactoryProxy(ConnectionFactory targetConnectionFactory) {
		setTargetConnectionFactory(targetConnectionFactory);
		afterPropertiesSet();
	}


	/**
	 * 委托给ConnectionFactoryUtils以自动参与Spring管理的事务.
	 * 抛出原始的ResourceException.
	 * 
	 * @return 事务性连接, 或创建一个新的
	 */
	@Override
	public Connection getConnection() throws ResourceException {
		Connection con = ConnectionFactoryUtils.doGetConnection(getTargetConnectionFactory());
		return getTransactionAwareConnectionProxy(con, getTargetConnectionFactory());
	}

	/**
	 * 使用代理将给定的每个方法调用委托给它, 但是将{@code close}调用委托给ConnectionFactoryUtils.
	 * 
	 * @param target 要包装的原始连接
	 * @param cf 从中获取Connection的ConnectionFactory
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getTransactionAwareConnectionProxy(Connection target, ConnectionFactory cf) {
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new TransactionAwareInvocationHandler(target, cf));
	}


	/**
	 * 调用处理器, 它将对CCI Connections的close调用委托给ConnectionFactoryUtils, 以便了解线程绑定事务.
	 */
	private static class TransactionAwareInvocationHandler implements InvocationHandler {

		private final Connection target;

		private final ConnectionFactory connectionFactory;

		public TransactionAwareInvocationHandler(Connection target, ConnectionFactory cf) {
			this.target = target;
			this.connectionFactory = cf;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自Connection接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("getLocalTransaction")) {
				if (ConnectionFactoryUtils.isConnectionTransactional(this.target, this.connectionFactory)) {
					throw new javax.resource.spi.IllegalStateException(
							"Local transaction handling not allowed within a managed transaction");
				}
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 仅在事务中不关闭.
				ConnectionFactoryUtils.doReleaseConnection(this.target, this.connectionFactory);
				return null;
			}

			// 在目标Connection上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
