package org.springframework.jca.cci.connection;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jca.cci.CannotGetCciConnectionException;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 提供从{@link javax.resource.cci.ConnectionFactory}获取CCI连接的静态方法.
 * 包括对Spring管理的事务连接的特殊支持, e.g. 由{@link CciLocalTransactionManager}
 * 或{@link org.springframework.transaction.jta.JtaTransactionManager}管理.
 *
 * <p>由{@link org.springframework.jca.cci.core.CciTemplate},
 * Spring的CCI操作对象和{@link CciLocalTransactionManager}内部使用.
 * 也可以直接在应用程序代码中使用.
 */
public abstract class ConnectionFactoryUtils {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryUtils.class);


	/**
	 * 从给定的ConnectionFactory获取连接.
	 * 将ResourceExceptions转换为未受检的通用数据访问异常的Spring层次结构,
	 * 简化调用代码并使任何抛出的异常更有意义.
	 * <p>知道绑定到当前线程的相应Connection, 例如使用{@link CciLocalTransactionManager}时.
	 * 如果事务同步处于活动状态, 则会将Connection绑定到线程 (e.g. 如果在JTA事务中).
	 * 
	 * @param cf 从中获取Connection的ConnectionFactory
	 * 
	 * @return 来自给定ConnectionFactory的CCI连接
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果尝试获取连接失败
	 */
	public static Connection getConnection(ConnectionFactory cf) throws CannotGetCciConnectionException {
		return getConnection(cf, null);
	}

	/**
	 * 从给定的ConnectionFactory获取连接.
	 * 将ResourceExceptions转换为未受检的通用数据访问异常的Spring层次结构,
	 * 简化调用代码并使任何抛出的异常更有意义.
	 * <p>知道绑定到当前线程的相应Connection, 例如使用{@link CciLocalTransactionManager}时.
	 * 如果事务同步处于活动状态, 则会将Connection绑定到线程 (e.g. 如果在JTA事务中).
	 * 
	 * @param cf 从中获取Connection的ConnectionFactory
	 * @param spec 所需连接的ConnectionSpec (may be {@code null}).
	 * Note: 如果指定了此项, 则将为每个调用获取新的Connection, 而不参与共享事务连接.
	 * 
	 * @return 来自给定ConnectionFactory的CCI连接
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果尝试获取连接失败
	 */
	public static Connection getConnection(ConnectionFactory cf, ConnectionSpec spec)
			throws CannotGetCciConnectionException {
		try {
			if (spec != null) {
				Assert.notNull(cf, "No ConnectionFactory specified");
				return cf.getConnection(spec);
			}
			else {
				return doGetConnection(cf);
			}
		}
		catch (ResourceException ex) {
			throw new CannotGetCciConnectionException("Could not get CCI Connection", ex);
		}
	}

	/**
	 * 实际从给定的ConnectionFactory获取CCI连接.
	 * 与{@link #getConnection}相同, 但抛出原始的ResourceException.
	 * <p>知道绑定到当前线程的相应Connection, 例如使用{@link CciLocalTransactionManager}时.
	 * 如果事务同步处于活动状态, 则会将Connection绑定到线程 (e.g. 如果在JTA事务中).
	 * <p>由{@link TransactionAwareConnectionFactoryProxy}直接访问.
	 * 
	 * @param cf 从中获取Connection的ConnectionFactory
	 * 
	 * @return 来自给定ConnectionFactory的CCI连接
	 * @throws ResourceException 如果由CCI API方法抛出
	 */
	public static Connection doGetConnection(ConnectionFactory cf) throws ResourceException {
		Assert.notNull(cf, "No ConnectionFactory specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		if (conHolder != null) {
			return conHolder.getConnection();
		}

		logger.debug("Opening CCI Connection");
		Connection con = cf.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for CCI Connection");
			conHolder = new ConnectionHolder(con);
			conHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(new ConnectionSynchronization(conHolder, cf));
			TransactionSynchronizationManager.bindResource(cf, conHolder);
		}

		return con;
	}

	/**
	 * 确定给定的JCA CCI连接是否是事务性的, 即由Spring的事务工具绑定到当前线程.
	 * 
	 * @param con 要检查的Connection
	 * @param cf 从中获取Connection的ConnectionFactory (may be {@code null})
	 * 
	 * @return Connection是否是事务性的
	 */
	public static boolean isConnectionTransactional(Connection con, ConnectionFactory cf) {
		if (cf == null) {
			return false;
		}
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(cf);
		return (conHolder != null && conHolder.getConnection() == con);
	}

	/**
	 * 关闭从给定的ConnectionFactory获取的给定Connection, 如果它不是外部管理的 (也就是说, 没有绑定到线程).
	 * 
	 * @param con 如有必要, 将关闭的Connection (如果是{@code null}, 则将忽略该调用)
	 * @param cf 从中获取Connection的ConnectionFactory (can be {@code null})
	 */
	public static void releaseConnection(Connection con, ConnectionFactory cf) {
		try {
			doReleaseConnection(con, cf);
		}
		catch (ResourceException ex) {
			logger.debug("Could not close CCI Connection", ex);
		}
		catch (Throwable ex) {
			// 不信任CCI驱动程序: 它可能会抛出RuntimeException或Error.
			logger.debug("Unexpected exception on closing CCI Connection", ex);
		}
	}

	/**
	 * 实际关闭从给定的ConnectionFactory获取的给定Connection.
	 * 与{@link #releaseConnection}相同, 但抛出原始的ResourceException.
	 * <p>由{@link TransactionAwareConnectionFactoryProxy}直接访问.
	 * 
	 * @param con 要关闭的Connection (如果是{@code null}, 则将忽略该调用)
	 * @param cf 从中获取Connection的ConnectionFactory (can be {@code null})
	 * 
	 * @throws ResourceException 如果被JCA CCI方法抛出
	 */
	public static void doReleaseConnection(Connection con, ConnectionFactory cf) throws ResourceException {
		if (con == null || isConnectionTransactional(con, cf)) {
			return;
		}
		con.close();
	}


	/**
	 * 在非本机CCI事务结束时资源清理的回调 (e.g. 在参与JTA事务时).
	 */
	private static class ConnectionSynchronization
			extends ResourceHolderSynchronization<ConnectionHolder, ConnectionFactory> {

		public ConnectionSynchronization(ConnectionHolder connectionHolder, ConnectionFactory connectionFactory) {
			super(connectionHolder, connectionFactory);
		}

		@Override
		protected void releaseResource(ConnectionHolder resourceHolder, ConnectionFactory resourceKey) {
			releaseConnection(resourceHolder.getConnection(), resourceKey);
		}
	}
}
