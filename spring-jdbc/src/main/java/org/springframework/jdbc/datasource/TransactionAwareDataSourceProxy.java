package org.springframework.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 目标JDBC {@link javax.sql.DataSource}的代理, 增加了对Spring管理的事务的感知.
 * 类似于Java EE服务器提供的事务性JNDI数据源.
 *
 * <p>仍然不知道Spring的数据访问支持的数据访问代码, 可以使用此代理无缝地参与Spring管理的事务.
 * 请注意, 事务管理器, 例如{@link DataSourceTransactionManager}, 仍然需要使用底层DataSource, <i>而不是</i>使用此代理.
 *
 * <p><b>确保TransactionAwareDataSourceProxy是DataSource代理/适配器链的最外层DataSource.</b>
 * TransactionAwareDataSourceProxy可以直接委托给目标连接池, 也可以委托给某些中间代理/适配器,
 * 例如{@link LazyConnectionDataSourceProxy} 或 {@link UserCredentialsDataSourceAdapter}.
 *
 * <p>委托给{@link DataSourceUtils}自动参与线程绑定事务, 例如由{@link DataSourceTransactionManager}管理.
 * 返回的连接上的{@code getConnection}调用和{@code close}调用将在事务中正常运行, i.e. 始终在事务连接上运行.
 * 如果不在事务中, 则应用正常的DataSource行为.
 *
 * <p>此代理允许数据访问代码与纯JDBC API一起使用, 并且仍然参与Spring管理的事务, 类似于Java EE/JTA环境中的JDBC代码.
 * 但是, 如果可能的话, 使用Spring的DataSourceUtils, JdbcTemplate或JDBC操作对象,
 * 即使没有目标DataSource的代理也可以获得事务参与, 从而避免了首先定义这样的代理的需要.
 *
 * <p>作为进一步的效果, 使用事务感知DataSource将剩余的事务超时应用于所有创建的JDBC (Prepared/Callable)Statement.
 * 这意味着通过标准JDBC执行的所有操作都将自动参与Spring管理的事务超时.
 *
 * <p><b>NOTE:</b> 此DataSource代理需要返回包装的Connection (实现{@link ConnectionProxy}接口), 以正确处理close调用.
 * 因此, 返回的Connection无法强制转换为本机JDBC连接类型, 例如OracleConnection, 或连接池实现类型.
 * 使用相应的{@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor}
 * 或JDBC 4的{@link Connection#unwrap}来检索本机JDBC连接.
 */
public class TransactionAwareDataSourceProxy extends DelegatingDataSource {

	private boolean reobtainTransactionalConnections = false;

	public TransactionAwareDataSourceProxy() {
	}

	/**
	 * @param targetDataSource 目标DataSource
	 */
	public TransactionAwareDataSourceProxy(DataSource targetDataSource) {
		super(targetDataSource);
	}

	/**
	 * 指定是否为事务中执行的每个操作重新获取目标Connection.
	 * <p>默认"false".
	 * 指定为"true"以重新获取Connection代理上每个调用的事务连接;
	 * 如果持有跨事务边界的Connection句柄, 那么建议在JBoss上使用.
	 * <p>此设置的效果类似于"hibernate.connection.release_mode"值"after_statement".
	 */
	public void setReobtainTransactionalConnections(boolean reobtainTransactionalConnections) {
		this.reobtainTransactionalConnections = reobtainTransactionalConnections;
	}


	/**
	 * 委托给DataSourceUtils, 用于自动参与Spring管理的事务.
	 * 如果有的话, 抛出原始的SQLException.
	 * <p>返回的Connection句柄实现ConnectionProxy接口, 允许检索底层目标Connection.
	 * 
	 * @return 事务Connection, 或新创建一个
	 */
	@Override
	public Connection getConnection() throws SQLException {
		DataSource ds = getTargetDataSource();
		Assert.state(ds != null, "'targetDataSource' is required");
		return getTransactionAwareConnectionProxy(ds);
	}

	/**
	 * 使用代理包装给定的Connection, 将给定的每个方法调用委托给代理, 除了委托{@code close()}调用给 DataSourceUtils.
	 * 
	 * @param targetDataSource Connection来自的DataSource
	 * 
	 * @return 包装的Connection
	 */
	protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
		return (Connection) Proxy.newProxyInstance(
				ConnectionProxy.class.getClassLoader(),
				new Class<?>[] {ConnectionProxy.class},
				new TransactionAwareInvocationHandler(targetDataSource));
	}

	/**
	 * 确定是为代理获取固定目标连接, 还是为每个操作重新获取目标连接.
	 * <p>对于所有标准情况, 默认实现返回{@code true}.
	 * 这可以通过{@link #setReobtainTransactionalConnections "reobtainTransactionalConnections"}标志覆盖,
	 * 该标志在活动事务中强制执行非固定目标连接.
	 * 请注意, 非事务性访问将始终使用固定连接.
	 * 
	 * @param targetDataSource 目标DataSource
	 */
	protected boolean shouldObtainFixedConnection(DataSource targetDataSource) {
		return (!TransactionSynchronizationManager.isSynchronizationActive() ||
				!this.reobtainTransactionalConnections);
	}


	/**
	 * 调用处理器, 它将对JDBC连接的关闭调用委托给DataSourceUtils, 以便感知线程绑定事务.
	 */
	private class TransactionAwareInvocationHandler implements InvocationHandler {

		private final DataSource targetDataSource;

		private Connection target;

		private boolean closed = false;

		public TransactionAwareInvocationHandler(DataSource targetDataSource) {
			this.targetDataSource = targetDataSource;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自ConnectionProxy接口的调用...

			if (method.getName().equals("equals")) {
				// 只有在代理相同时才被视为相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用Connection代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("toString")) {
				// 允许区分代理和原始连接.
				StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Connection ");
				if (this.target != null) {
					sb.append("[").append(this.target.toString()).append("]");
				}
				else {
					sb.append(" from DataSource [").append(this.targetDataSource).append("]");
				}
				return sb.toString();
			}
			else if (method.getName().equals("unwrap")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isWrapperFor")) {
				if (((Class<?>) args[0]).isInstance(proxy)) {
					return true;
				}
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 仅在事务中不关闭.
				DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
				this.closed = true;
				return null;
			}
			else if (method.getName().equals("isClosed")) {
				return this.closed;
			}

			if (this.target == null) {
				if (this.closed) {
					throw new SQLException("Connection handle already closed");
				}
				if (shouldObtainFixedConnection(this.targetDataSource)) {
					this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
				}
			}
			Connection actualTarget = this.target;
			if (actualTarget == null) {
				actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
			}

			if (method.getName().equals("getTargetConnection")) {
				// 处理getTargetConnection方法: 返回底层Connection.
				return actualTarget;
			}

			// 在目标Connection上调用方法.
			try {
				Object retVal = method.invoke(actualTarget, args);

				// 如果返回值是Statement, 则应用事务超时.
				// 适用于createStatement, prepareStatement, prepareCall.
				if (retVal instanceof Statement) {
					DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (actualTarget != this.target) {
					DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
				}
			}
		}
	}
}
