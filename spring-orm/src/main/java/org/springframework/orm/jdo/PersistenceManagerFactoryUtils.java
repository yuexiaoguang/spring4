package org.springframework.orm.jdo;

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOException;
import javax.jdo.JDOFatalDataStoreException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOOptimisticVerificationException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper类, 具有JDO {@link PersistenceManager}处理方法, 允许在事务中重用PersistenceManager实例.
 * 还提供异常转换支持.
 *
 * <p>由{@link JdoTransactionManager}内部使用.
 * 也可以直接在应用程序代码中使用.
 */
public abstract class PersistenceManagerFactoryUtils {

	/**
	 * 清理JDO PersistenceManagers的TransactionSynchronization对象的顺序值.
	 * 返回DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100,
	 * 在JDBC连接清理之前执行PersistenceManager清理.
	 */
	public static final int PERSISTENCE_MANAGER_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	private static final Log logger = LogFactory.getLog(PersistenceManagerFactoryUtils.class);


	/**
	 * 为给定的PersistenceManagerFactory创建适当的SQLExceptionTranslator.
	 * <p>如果找到DataSource, 则为DataSource创建一个SQLErrorCodeSQLExceptionTranslator;
	 * 否则回退到SQLStateSQLExceptionTranslator.
	 * 
	 * @param connectionFactory PersistenceManagerFactory的连接工厂 (may be {@code null})
	 * 
	 * @return the SQLExceptionTranslator (never {@code null})
	 */
	static SQLExceptionTranslator newJdbcExceptionTranslator(Object connectionFactory) {
		// 检查PersistenceManagerFactory的DataSource.
		if (connectionFactory instanceof DataSource) {
			return new SQLErrorCodeSQLExceptionTranslator((DataSource) connectionFactory);
		}
		else {
			return new SQLStateSQLExceptionTranslator();
		}
	}

	/**
	 * 通过给定的工厂获取JDO PersistenceManager.
	 * 知道绑定到当前线程的相应PersistenceManager, 例如在使用JdoTransactionManager时.
	 * 如果"allowCreate"是 {@code true}, 将创建一个新的PersistenceManager.
	 * 
	 * @param pmf 用于创建PersistenceManager的PersistenceManagerFactory
	 * @param allowCreate 如果在当前线程没有找到事务性PersistenceManager时, 应创建非事务性PersistenceManager
	 * 
	 * @return the PersistenceManager
	 * @throws DataAccessResourceFailureException 如果无法获取PersistenceManager
	 * @throws IllegalStateException 如果找不到线程绑定的PersistenceManager, 并且"allowCreate"是 {@code false}
	 */
	public static PersistenceManager getPersistenceManager(PersistenceManagerFactory pmf, boolean allowCreate)
		throws DataAccessResourceFailureException, IllegalStateException {

		try {
			return doGetPersistenceManager(pmf, allowCreate);
		}
		catch (JDOException ex) {
			throw new DataAccessResourceFailureException("Could not obtain JDO PersistenceManager", ex);
		}
	}

	/**
	 * 通过给定的工厂获取JDO PersistenceManager.
	 * 知道绑定到当前线程的相应PersistenceManager, 例如在使用JdoTransactionManager时.
	 * 将创建一个新的PersistenceManager, 如果"allowCreate" 是{@code true}.
	 * <p>与{@code getPersistenceManager}相同, 但抛出原始的JDOException.
	 * 
	 * @param pmf 用于创建PersistenceManager的PersistenceManagerFactory
	 * @param allowCreate 如果在当前线程没有找到事务性PersistenceManager时, 应创建非事务性PersistenceManager
	 * 
	 * @return the PersistenceManager
	 * @throws JDOException 如果无法创建PersistenceManager
	 * @throws IllegalStateException 如果找不到线程绑定的PersistenceManager, 并且"allowCreate"是 {@code false}
	 */
	public static PersistenceManager doGetPersistenceManager(PersistenceManagerFactory pmf, boolean allowCreate)
		throws JDOException, IllegalStateException {

		Assert.notNull(pmf, "No PersistenceManagerFactory specified");

		PersistenceManagerHolder pmHolder =
				(PersistenceManagerHolder) TransactionSynchronizationManager.getResource(pmf);
		if (pmHolder != null) {
			if (!pmHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				pmHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new PersistenceManagerSynchronization(pmHolder, pmf, false));
			}
			return pmHolder.getPersistenceManager();
		}

		if (!allowCreate && !TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("No JDO PersistenceManager bound to thread, " +
					"and configuration does not allow creation of non-transactional one here");
		}

		logger.debug("Opening JDO PersistenceManager");
		PersistenceManager pm = pmf.getPersistenceManager();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for JDO PersistenceManager");
			// 在事务中使用相同的PersistenceManager进行进一步的JDO操作.
			// 在事务完成时, 将通过同步删除线程对象.
			pmHolder = new PersistenceManagerHolder(pm);
			pmHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new PersistenceManagerSynchronization(pmHolder, pmf, true));
			TransactionSynchronizationManager.bindResource(pmf, pmHolder);
		}

		return pm;
	}

	/**
	 * 返回给定的JDO PersistenceManager是否是事务性的, 即由Spring的事务工具绑定到当前线程.
	 * 
	 * @param pm 要检查的JDO PersistenceManager
	 * @param pmf 使用PersistenceManager创建的JDO PersistenceManagerFactory (can be {@code null})
	 * 
	 * @return PersistenceManager是否是事务性的
	 */
	public static boolean isPersistenceManagerTransactional(
			PersistenceManager pm, PersistenceManagerFactory pmf) {

		if (pmf == null) {
			return false;
		}
		PersistenceManagerHolder pmHolder =
				(PersistenceManagerHolder) TransactionSynchronizationManager.getResource(pmf);
		return (pmHolder != null && pm == pmHolder.getPersistenceManager());
	}

	/**
	 * 将当前事务超时应用于给定的JDO Query对象.
	 * 
	 * @param query JDO Query对象
	 * @param pmf 为其创建Query的JDO PersistenceManagerFactory
	 * 
	 * @throws JDOException 如果由JDO方法抛出
	 */
	public static void applyTransactionTimeout(Query query, PersistenceManagerFactory pmf) throws JDOException {
		Assert.notNull(query, "No Query object specified");
		PersistenceManagerHolder pmHolder =
				(PersistenceManagerHolder) TransactionSynchronizationManager.getResource(pmf);
		if (pmHolder != null && pmHolder.hasTimeout() &&
				pmf.supportedOptions().contains("javax.jdo.option.DatastoreTimeout")) {
			int timeout = (int) pmHolder.getTimeToLiveInMillis();
			query.setDatastoreReadTimeoutMillis(timeout);
			query.setDatastoreWriteTimeoutMillis(timeout);
		}
	}

	/**
	 * 将给定的JDOException转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * <p>这里介绍了最重要的案例, 如未找到对象或乐观锁定失败.
	 * 对于更精细的粒度转换, JdoTransactionManager通过JdoDialect支持复杂的异常转换.
	 * 
	 * @param ex 发生的JDOException
	 * 
	 * @return 相应的DataAccessException实例
	 */
	public static DataAccessException convertJdoAccessException(JDOException ex) {
		if (ex instanceof JDOObjectNotFoundException) {
			throw new JdoObjectRetrievalFailureException((JDOObjectNotFoundException) ex);
		}
		if (ex instanceof JDOOptimisticVerificationException) {
			throw new JdoOptimisticLockingFailureException((JDOOptimisticVerificationException) ex);
		}
		if (ex instanceof JDODataStoreException) {
			return new JdoResourceFailureException((JDODataStoreException) ex);
		}
		if (ex instanceof JDOFatalDataStoreException) {
			return new JdoResourceFailureException((JDOFatalDataStoreException) ex);
		}
		if (ex instanceof JDOUserException) {
			return new JdoUsageException((JDOUserException) ex);
		}
		if (ex instanceof JDOFatalUserException) {
			return new JdoUsageException((JDOFatalUserException) ex);
		}
		// fallback
		return new JdoSystemException(ex);
	}

	/**
	 * 关闭通过给定工厂创建的给定PersistenceManager, 如果它不是外部管理的 (i.e. 不绑定到线程).
	 * 
	 * @param pm 要关闭的PersistenceManager
	 * @param pmf 创建PersistenceManager使用的PersistenceManagerFactory (can be {@code null})
	 */
	public static void releasePersistenceManager(PersistenceManager pm, PersistenceManagerFactory pmf) {
		try {
			doReleasePersistenceManager(pm, pmf);
		}
		catch (JDOException ex) {
			logger.debug("Could not close JDO PersistenceManager", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing JDO PersistenceManager", ex);
		}
	}

	/**
	 * 实际为给定的工厂发布PersistenceManager.
	 * 与{@code releasePersistenceManager}相同, 但抛出原始的JDOException.
	 * 
	 * @param pm 要关闭的PersistenceManager
	 * @param pmf 创建PersistenceManager使用的PersistenceManagerFactory (can be {@code null})
	 * 
	 * @throws JDOException 如果由JDO方法抛出
	 */
	public static void doReleasePersistenceManager(PersistenceManager pm, PersistenceManagerFactory pmf)
			throws JDOException {

		if (pm == null) {
			return;
		}
		// 仅释放非事务性PersistenceManager.
		if (!isPersistenceManagerTransactional(pm, pmf)) {
			logger.debug("Closing JDO PersistenceManager");
			pm.close();
		}
	}


	/**
	 * 在非JDO事务结束时的资源清理回调 (e.g. 在参与JtaTransactionManager事务时).
	 */
	private static class PersistenceManagerSynchronization
			extends ResourceHolderSynchronization<PersistenceManagerHolder, PersistenceManagerFactory>
			implements Ordered {

		private final boolean newPersistenceManager;

		public PersistenceManagerSynchronization(
				PersistenceManagerHolder pmHolder, PersistenceManagerFactory pmf, boolean newPersistenceManager) {
			super(pmHolder, pmf);
			this.newPersistenceManager = newPersistenceManager;
		}

		@Override
		public int getOrder() {
			return PERSISTENCE_MANAGER_SYNCHRONIZATION_ORDER;
		}

		@Override
		public void flushResource(PersistenceManagerHolder resourceHolder) {
			try {
				resourceHolder.getPersistenceManager().flush();
			}
			catch (JDOException ex) {
				throw convertJdoAccessException(ex);
			}
		}

		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newPersistenceManager;
		}

		@Override
		protected boolean shouldReleaseAfterCompletion(PersistenceManagerHolder resourceHolder) {
			return !resourceHolder.getPersistenceManager().isClosed();
		}

		@Override
		protected void releaseResource(PersistenceManagerHolder resourceHolder, PersistenceManagerFactory resourceKey) {
			releasePersistenceManager(resourceHolder.getPersistenceManager(), resourceKey);
		}
	}

}
