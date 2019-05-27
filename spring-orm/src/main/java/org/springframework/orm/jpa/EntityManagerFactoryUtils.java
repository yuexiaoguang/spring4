package org.springframework.orm.jpa;

import java.lang.reflect.Method;
import java.util.Map;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.Ordered;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helper类, 具有JPA EntityManager处理方法, 允许在事务中重用EntityManager实例.
 * 还提供异常转换支持.
 *
 * <p>主要用于框架内部使用.
 */
@SuppressWarnings("unchecked")
public abstract class EntityManagerFactoryUtils {

	/**
	 * 清理JPA EntityManagers的TransactionSynchronization对象的顺序值.
	 * 返回DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100 在JDBC连接清理之前执行EntityManager清理.
	 */
	public static final int ENTITY_MANAGER_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	private static final Log logger = LogFactory.getLog(EntityManagerFactoryUtils.class);


	private static Method createEntityManagerWithSynchronizationTypeMethod;

	private static Object synchronizationTypeUnsynchronized;

	static {
		try {
			@SuppressWarnings( "rawtypes" )
			Class<Enum> synchronizationTypeClass = (Class<Enum>) ClassUtils.forName(
					"javax.persistence.SynchronizationType", EntityManagerFactoryUtils.class.getClassLoader());
			createEntityManagerWithSynchronizationTypeMethod = EntityManagerFactory.class.getMethod(
					"createEntityManager", synchronizationTypeClass, Map.class);
			synchronizationTypeUnsynchronized = Enum.valueOf(synchronizationTypeClass, "UNSYNCHRONIZED");
		}
		catch (Exception ex) {
			// No JPA 2.1 API available
			createEntityManagerWithSynchronizationTypeMethod = null;
		}
	}


	/**
	 * 在给定的Spring应用程序上下文中找到具有给定名称的EntityManagerFactory (表示为ListableBeanFactory).
	 * <p>如果发现的EntityManagerFactory实现了{@link EntityManagerFactoryInfo}接口,
	 * 则指定的单元名称将与配置的持久性单元匹配.
	 * 如果没有, 假定EntityManagerFactory bean名称遵循该约定, 则持久性单元名称将与Spring bean名称匹配.
	 * <p>如果没有给出单元名称, 则此方法将通过{@link ListableBeanFactory#getBean(Class)}搜索默认的EntityManagerFactory.
	 * 
	 * @param beanFactory 要搜索的ListableBeanFactory
	 * @param unitName 持久性单元的名称 (可能是{@code null}或为空, 在这种情况下, 将搜索一个类型为EntityManagerFactory的bean)
	 * 
	 * @return the EntityManagerFactory
	 * @throws NoSuchBeanDefinitionException 如果上下文中没有这样的EntityManagerFactory
	 */
	public static EntityManagerFactory findEntityManagerFactory(
			ListableBeanFactory beanFactory, String unitName) throws NoSuchBeanDefinitionException {

		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		if (StringUtils.hasLength(unitName)) {
			// 是否可以找到具有匹配的持久性单元名称的EntityManagerFactory.
			String[] candidateNames =
					BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class);
			for (String candidateName : candidateNames) {
				EntityManagerFactory emf = (EntityManagerFactory) beanFactory.getBean(candidateName);
				if (emf instanceof EntityManagerFactoryInfo) {
					if (unitName.equals(((EntityManagerFactoryInfo) emf).getPersistenceUnitName())) {
						return emf;
					}
				}
			}
			// 找不到匹配的持久性单元 - 获取具有持久性单元名称的EntityManagerFactory作为bean名称 (按照惯例).
			return beanFactory.getBean(unitName, EntityManagerFactory.class);
		}
		else {
			// 在上下文中查找唯一的EntityManagerFactory bean, 回退到父上下文.
			return beanFactory.getBean(EntityManagerFactory.class);
		}
	}

	/**
	 * 从给定工厂获取JPA EntityManager.
	 * 知道绑定到当前线程的相应EntityManager, e.g. 使用JpaTransactionManager时.
	 * <p>Note: 如果没有找到线程绑定的EntityManager, 将返回{@code null}!
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory
	 * 
	 * @return the EntityManager, 或{@code null}
	 * @throws DataAccessResourceFailureException 如果无法获取EntityManager
	 */
	public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf)
			throws DataAccessResourceFailureException {

		return getTransactionalEntityManager(emf, null);
	}

	/**
	 * 从给定工厂获取JPA EntityManager.
	 * 知道绑定到当前线程的相应EntityManager, e.g. 使用JpaTransactionManager时.
	 * <p>Note: 如果没有找到线程绑定的EntityManager, 将返回{@code null}!
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * 
	 * @return the EntityManager, 或{@code null}
	 * @throws DataAccessResourceFailureException 如果无法获取EntityManager
	 */
	public static EntityManager getTransactionalEntityManager(EntityManagerFactory emf, Map<?, ?> properties)
			throws DataAccessResourceFailureException {
		try {
			return doGetTransactionalEntityManager(emf, properties, true);
		}
		catch (PersistenceException ex) {
			throw new DataAccessResourceFailureException("Could not obtain JPA EntityManager", ex);
		}
	}

	/**
	 * 从给定工厂获取JPA EntityManager.
	 * 知道绑定到当前线程的相应EntityManager, e.g. 使用JpaTransactionManager时.
	 * <p>与{@code getEntityManager}相同, 但抛出原始的PersistenceException.
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * 
	 * @return the EntityManager, 或{@code null}
	 * @throws javax.persistence.PersistenceException 如果无法创建EntityManager
	 */
	public static EntityManager doGetTransactionalEntityManager(EntityManagerFactory emf, Map<?, ?> properties)
			throws PersistenceException {

		return doGetTransactionalEntityManager(emf, properties, true);
	}

	/**
	 * 从给定工厂获取JPA EntityManager.
	 * 知道绑定到当前线程的相应EntityManager, e.g. 使用JpaTransactionManager时.
	 * <p>与{@code getEntityManager}相同, 但抛出原始的PersistenceException.
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return the EntityManager, 或{@code null}
	 * @throws javax.persistence.PersistenceException 如果无法创建EntityManager
	 */
	public static EntityManager doGetTransactionalEntityManager(
			EntityManagerFactory emf, Map<?, ?> properties, boolean synchronizedWithTransaction) throws PersistenceException {

		Assert.notNull(emf, "No EntityManagerFactory specified");

		EntityManagerHolder emHolder =
				(EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
		if (emHolder != null) {
			if (synchronizedWithTransaction) {
				if (!emHolder.isSynchronizedWithTransaction()) {
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						// 尝试将EntityManager本身与正在进行的JTA事务显式同步.
						try {
							emHolder.getEntityManager().joinTransaction();
						}
						catch (TransactionRequiredException ex) {
							logger.debug("Could not join transaction because none was actually active", ex);
						}
					}
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						Object transactionData = prepareTransaction(emHolder.getEntityManager(), emf);
						TransactionSynchronizationManager.registerSynchronization(
								new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, false));
						emHolder.setSynchronizedWithTransaction(true);
					}
				}
				// 使用保存器的引用计数来跟踪synchronizedWithTransaction访问.
				// 下面使用 isOpen() 检查来了解它.
				emHolder.requested();
				return emHolder.getEntityManager();
			}
			else {
				// 不同步的EntityManager要求
				if (emHolder.isTransactionActive() && !emHolder.isOpen()) {
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						return null;
					}
					// 具有来自JpaTransactionManager的活动事务的EntityManagerHolder, 之前没有应用程序代码请求同步的EntityManager.
					// 取消绑定以便注册新的非同步EntityManager.
					TransactionSynchronizationManager.unbindResource(emf);
				}
				else {
					// 无论是以前绑定的未同步的EntityManager, 或者应用程序之前已请求同步的EntityManager,
					// 因此将此事务的EntityManager升级为同步之前.
					return emHolder.getEntityManager();
				}
			}
		}
		else if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			// 表示无法获取事务性EntityManager.
			return null;
		}

		// 创建一个新的EntityManager以在当前事务中使用.
		logger.debug("Opening JPA EntityManager");
		EntityManager em = null;
		if (!synchronizedWithTransaction && createEntityManagerWithSynchronizationTypeMethod != null) {
			try {
				em = (EntityManager) ReflectionUtils.invokeMethod(createEntityManagerWithSynchronizationTypeMethod,
						emf, synchronizationTypeUnsynchronized, properties);
			}
			catch (AbstractMethodError err) {
				// JPA 2.1 API可用, 但方法实际上未在持久性提供者中实现:
				// 回到常规的createEntityManager方法.
			}
		}
		if (em == null) {
			em = (!CollectionUtils.isEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
		}

		// 在事务中使用相同的EntityManager进行进一步的JPA操作.
		// 在事务完成时, 将同步删除线程绑定对象.
		logger.debug("Registering transaction synchronization for JPA EntityManager");
		emHolder = new EntityManagerHolder(em);
		if (synchronizedWithTransaction) {
			Object transactionData = prepareTransaction(em, emf);
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, true));
			emHolder.setSynchronizedWithTransaction(true);
		}
		else {
			// 未同步 - 按照JPA 2.1规范的要求, 只在事务范围...
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionScopedEntityManagerSynchronization(emHolder, emf));
		}
		TransactionSynchronizationManager.bindResource(emf, emHolder);

		return em;
	}

	/**
	 * 在给定的EntityManager上准备事务.
	 * 
	 * @param em 要准备的EntityManager
	 * @param emf 已创建EntityManager的EntityManagerFactory
	 * 
	 * @return 保存事务数据的任意对象 (传递给cleanupTransaction)
	 */
	private static Object prepareTransaction(EntityManager em, EntityManagerFactory emf) {
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				return jpaDialect.prepareTransaction(em,
						TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
						TransactionSynchronizationManager.getCurrentTransactionName());
			}
		}
		return null;
	}

	/**
	 * 在给定的EntityManager上准备事务.
	 * 
	 * @param transactionData 保存事务数据的任意对象 (由prepareTransaction返回)
	 * @param emf 创建EntityManager的EntityManagerFactory
	 */
	private static void cleanupTransaction(Object transactionData, EntityManagerFactory emf) {
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			JpaDialect jpaDialect = emfInfo.getJpaDialect();
			if (jpaDialect != null) {
				jpaDialect.cleanupTransaction(transactionData);
			}
		}
	}

	/**
	 * 将当前事务超时应用于给定的JPA Query对象.
	 * <p>此方法相应地设置JPA 2.0查询提示"javax.persistence.query.timeout".
	 * 
	 * @param query JPA Query对象
	 * @param emf 为其创建Query的JPA EntityManagerFactory
	 */
	public static void applyTransactionTimeout(Query query, EntityManagerFactory emf) {
		EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
		if (emHolder != null && emHolder.hasTimeout()) {
			int timeoutValue = (int) emHolder.getTimeToLiveInMillis();
			try {
				query.setHint("javax.persistence.query.timeout", timeoutValue);
			}
			catch (IllegalArgumentException ex) {
				// oh well, at least we tried...
			}
		}
	}

	/**
	 * 将给定的运行时异常转换为{@code org.springframework.dao}层次结构中的适当异常.
	 * 如果没有适当的转换, 返回null: 任何其他异常可能是由用户代码产生的, 不应转换.
	 * <p>这里介绍了最重要的案例, 如未找到对象或乐观锁定失败.
	 * 对于更精细的粒度转换, JpaTransactionManager等通过JpaDialect支持复杂的异常转换.
	 * 
	 * @param ex 发生的运行时异常
	 * 
	 * @return 相应的DataAccessException实例, 或{@code null} 如果不应该转换异常
	 */
	public static DataAccessException convertJpaAccessExceptionIfPossible(RuntimeException ex) {
		// 遵循JPA规范, 持久性提供者除了PersistenceException之外还可以抛出这两个异常.
		if (ex instanceof IllegalStateException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof IllegalArgumentException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		// 检查已知的PersistenceException子类.
		if (ex instanceof EntityNotFoundException) {
			return new JpaObjectRetrievalFailureException((EntityNotFoundException) ex);
		}
		if (ex instanceof NoResultException) {
			return new EmptyResultDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof QueryTimeoutException) {
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage(), ex);
		}
		if (ex instanceof LockTimeoutException) {
			return new CannotAcquireLockException(ex.getMessage(), ex);
		}
		if (ex instanceof PessimisticLockException) {
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof OptimisticLockException) {
			return new JpaOptimisticLockingFailureException((OptimisticLockException) ex);
		}
		if (ex instanceof EntityExistsException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof TransactionRequiredException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}

		// 如果有另一种PersistenceException, 抛出它.
		if (ex instanceof PersistenceException) {
			return new JpaSystemException(ex);
		}

		// 如果到达这里, 有一个由用户代码而不是持久性提供者产生的异常, 所以返回null表示不应该发生转换.
		return null;
	}

	/**
	 * 关闭给定的JPA EntityManager, 捕获并记录抛出的任何清理异常.
	 * 
	 * @param em 要关闭的JPA EntityManager (may be {@code null})
	 */
	public static void closeEntityManager(EntityManager em) {
		if (em != null) {
			logger.debug("Closing JPA EntityManager");
			try {
				if (em.isOpen()) {
					em.close();
				}
			}
			catch (PersistenceException ex) {
				logger.debug("Could not close JPA EntityManager", ex);
			}
			catch (Throwable ex) {
				logger.debug("Unexpected exception on closing JPA EntityManager", ex);
			}
		}
	}


	/**
	 * 在非JPA事务结束时 (e.g. 在参与JtaTransactionManager事务时)资源清理的回调, 与正在进行的事务完全同步.
	 */
	private static class TransactionalEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
			implements Ordered {

		private final Object transactionData;

		private final JpaDialect jpaDialect;

		private final boolean newEntityManager;

		public TransactionalEntityManagerSynchronization(
				EntityManagerHolder emHolder, EntityManagerFactory emf, Object txData, boolean newEm) {

			super(emHolder, emf);
			this.transactionData = txData;
			this.jpaDialect = (emf instanceof EntityManagerFactoryInfo ?
					((EntityManagerFactoryInfo) emf).getJpaDialect() : null);
			this.newEntityManager = newEm;
		}

		@Override
		public int getOrder() {
			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER;
		}

		@Override
		protected void flushResource(EntityManagerHolder resourceHolder) {
			EntityManager em = resourceHolder.getEntityManager();
			if (em instanceof EntityManagerProxy) {
				EntityManager target = ((EntityManagerProxy) em).getTargetEntityManager();
				if (TransactionSynchronizationManager.hasResource(target)) {
					// 在joinTransaction()调用之后, ExtendedEntityManagerSynchronization处于活动状态:
					// 刷新已经注册的同步.
					return;
				}
			}
			try {
				em.flush();
			}
			catch (RuntimeException ex) {
				DataAccessException dae;
				if (this.jpaDialect != null) {
					dae = this.jpaDialect.translateExceptionIfPossible(ex);
				}
				else {
					dae = convertJpaAccessExceptionIfPossible(ex);
				}
				throw (dae != null ? dae : ex);
			}
		}

		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newEntityManager;
		}

		@Override
		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
			closeEntityManager(resourceHolder.getEntityManager());
		}

		@Override
		protected void cleanupResource(
				EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey, boolean committed) {

			if (!committed) {
				// 清除EntityManager中所有挂起的 inserts/updates/deletes.
				// 必要的预绑定EntityManager, 以避免不一致的状态.
				resourceHolder.getEntityManager().clear();
			}
			cleanupTransaction(this.transactionData, resourceKey);
		}
	}


	/**
	 * 在事务结束时关闭EntityManager的最小回调.
	 */
	private static class TransactionScopedEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
			implements Ordered {

		public TransactionScopedEntityManagerSynchronization(EntityManagerHolder emHolder, EntityManagerFactory emf) {
			super(emHolder, emf);
		}

		@Override
		public int getOrder() {
			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER + 1;
		}

		@Override
		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
			closeEntityManager(resourceHolder.getEntityManager());
		}
	}

}
