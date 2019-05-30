package org.springframework.orm.jpa;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 委托创建各种{@link javax.persistence.EntityManager}代理, 这些代理遵循JPA规范对"扩展" EntityManager的语义.
 *
 * <p>支持"扩展" EntityManager的几种不同变体:
 * 特别是, 由{@link javax.persistence.EntityManagerFactory#createEntityManager()}定义的"应用程序管理的扩展EntityManager",
 * 以及由{@link javax.persistence.PersistenceContextType#EXTENDED}定义的"容器管理的扩展EntityManager".
 *
 * <p>"应用程序管理"和"容器管理"之间的原始区别是需要通过"应用程序"场景中的
 * {@link EntityManager#joinTransaction()}方法显式加入外部管理的事务, 而不是在"容器"情况下自动连接每个用户级EntityManager操作.
 * 从JPA 2.1开始, 两种连接模式都可用于两种EntityManager, 因此"应用程序管理"和"容器管理"之间的区别,
 * 现在主要在连接模式默认值和容器管理的EntityManager的生命周期限制 (i.e. 绑定到它注入的对象).
 */
public abstract class ExtendedEntityManagerCreator {

	/**
	 * 创建应用程序管理的扩展EntityManager代理.
	 * 
	 * @param rawEntityManager 要装饰的原始EntityManager
	 * @param emfInfo 从中获取JpaDialect和PersistenceUnitInfo的 EntityManagerFactoryInfo
	 * 
	 * @return 应用程序管理的EntityManager, 可以加入事务但不会自动参与
	 */
	public static EntityManager createApplicationManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

		return createProxy(rawEntityManager, emfInfo, false, false);
	}

	/**
	 * 创建应用程序管理的扩展EntityManager代理.
	 * 
	 * @param rawEntityManager 要装饰的原始EntityManager
	 * @param emfInfo 从中获取JpaDialect和PersistenceUnitInfo的 EntityManagerFactoryInfo
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return 应用程序管理的EntityManager, 可以加入事务但不会自动参与
	 */
	public static EntityManager createApplicationManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo, boolean synchronizedWithTransaction) {

		return createProxy(rawEntityManager, emfInfo, false, synchronizedWithTransaction);
	}

	/**
	 * 创建容器管理的扩展EntityManager代理.
	 * 
	 * @param rawEntityManager 要装饰的原始EntityManager
	 * @param emfInfo 从中获取JpaDialect和PersistenceUnitInfo的 EntityManagerFactoryInfo
	 * 
	 * @return 容器管理的EntityManager, 它将自动参与任何管理的事务
	 */
	public static EntityManager createContainerManagedEntityManager(
			EntityManager rawEntityManager, EntityManagerFactoryInfo emfInfo) {

		return createProxy(rawEntityManager, emfInfo, true, true);
	}

	/**
	 * 创建容器管理的扩展EntityManager代理.
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory.
	 * 如果这实现了EntityManagerFactoryInfo接口, 则将相应地检测相应的JpaDialect和PersistenceUnitInfo.
	 * 
	 * @return 容器管理的EntityManager, 它将自动参与任何管理的事务
	 */
	public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf) {
		return createContainerManagedEntityManager(emf, null, true);
	}

	/**
	 * 创建容器管理的扩展EntityManager代理.
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory.
	 * 如果这实现了EntityManagerFactoryInfo接口, 则将相应地检测相应的JpaDialect和PersistenceUnitInfo.
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * 
	 * @return 容器管理的EntityManager, 它将自动参与任何管理的事务
	 */
	public static EntityManager createContainerManagedEntityManager(EntityManagerFactory emf, Map<?, ?> properties) {
		return createContainerManagedEntityManager(emf, properties, true);
	}

	/**
	 * 创建容器管理的扩展EntityManager代理.
	 * 
	 * @param emf 用于创建EntityManager的EntityManagerFactory.
	 * 如果这实现了EntityManagerFactoryInfo接口, 则将相应地检测相应的JpaDialect和PersistenceUnitInfo.
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return 容器管理的EntityManager, 它需要容器驱动的生命周期管理, 但可以选择退出自动事务同步
	 */
	public static EntityManager createContainerManagedEntityManager(
			EntityManagerFactory emf, Map<?, ?> properties, boolean synchronizedWithTransaction) {

		Assert.notNull(emf, "EntityManagerFactory must not be null");
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			EntityManagerFactory nativeEmf = emfInfo.getNativeEntityManagerFactory();
			EntityManager rawEntityManager = (!CollectionUtils.isEmpty(properties) ?
					nativeEmf.createEntityManager(properties) : nativeEmf.createEntityManager());
			return createProxy(rawEntityManager, emfInfo, true, synchronizedWithTransaction);
		}
		else {
			EntityManager rawEntityManager = (!CollectionUtils.isEmpty(properties) ?
					emf.createEntityManager(properties) : emf.createEntityManager());
			return createProxy(rawEntityManager, null, null, null, null, true, synchronizedWithTransaction);
		}
	}


	/**
	 * 实际创建EntityManager代理.
	 * 
	 * @param rawEntityManager 原始EntityManager
	 * @param emfInfo 从中获取JpaDialect和PersistenceUnitInfo的EntityManagerFactoryInfo
	 * @param containerManaged 是否遵循容器管理的EntityManager或应用程序管理的EntityManager语义
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return EntityManager代理
	 */
	private static EntityManager createProxy(EntityManager rawEntityManager,
			EntityManagerFactoryInfo emfInfo, boolean containerManaged, boolean synchronizedWithTransaction) {

		Assert.notNull(emfInfo, "EntityManagerFactoryInfo must not be null");
		JpaDialect jpaDialect = emfInfo.getJpaDialect();
		PersistenceUnitInfo pui = emfInfo.getPersistenceUnitInfo();
		Boolean jta = (pui != null ? pui.getTransactionType() == PersistenceUnitTransactionType.JTA : null);
		return createProxy(rawEntityManager, emfInfo.getEntityManagerInterface(),
				emfInfo.getBeanClassLoader(), jpaDialect, jta, containerManaged, synchronizedWithTransaction);
	}

	/**
	 * 实际创建EntityManager代理.
	 * 
	 * @param rawEm 原始EntityManager
	 * @param emIfc 要代理的(可能是特定于供应商的) EntityManager接口, 或{@code null} 用于默认检测所有接口
	 * @param cl 用于代理创建的ClassLoader (maybe {@code null})
	 * @param exceptionTranslator 要使用的PersistenceException转换器
	 * @param jta 是否创建一个支持JTA的EntityManager (或{@code null}, 如果事先不知道)
	 * @param containerManaged 是否遵循容器管理的EntityManager或应用程序管理的EntityManager语义
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return EntityManager代理
	 */
	private static EntityManager createProxy(
			EntityManager rawEm, Class<? extends EntityManager> emIfc, ClassLoader cl,
			PersistenceExceptionTranslator exceptionTranslator, Boolean jta,
			boolean containerManaged, boolean synchronizedWithTransaction) {

		Assert.notNull(rawEm, "EntityManager must not be null");
		Set<Class<?>> ifcs = new LinkedHashSet<Class<?>>();
		if (emIfc != null) {
			ifcs.add(emIfc);
		}
		else {
			ifcs.addAll(ClassUtils.getAllInterfacesForClassAsSet(rawEm.getClass(), cl));
		}
		ifcs.add(EntityManagerProxy.class);
		return (EntityManager) Proxy.newProxyInstance(
				(cl != null ? cl : ExtendedEntityManagerCreator.class.getClassLoader()),
				ClassUtils.toClassArray(ifcs),
				new ExtendedEntityManagerInvocationHandler(
						rawEm, exceptionTranslator, jta, containerManaged, synchronizedWithTransaction));
	}


	/**
	 * 用于JPA规范中定义的扩展EntityManager的InvocationHandler.
	 */
	@SuppressWarnings("serial")
	private static class ExtendedEntityManagerInvocationHandler implements InvocationHandler, Serializable {

		private static final Log logger = LogFactory.getLog(ExtendedEntityManagerInvocationHandler.class);

		private final EntityManager target;

		private final PersistenceExceptionTranslator exceptionTranslator;

		private final boolean jta;

		private final boolean containerManaged;

		private final boolean synchronizedWithTransaction;

		private ExtendedEntityManagerInvocationHandler(EntityManager target,
				PersistenceExceptionTranslator exceptionTranslator, Boolean jta,
				boolean containerManaged, boolean synchronizedWithTransaction) {

			this.target = target;
			this.exceptionTranslator = exceptionTranslator;
			this.jta = (jta != null ? jta : isJtaEntityManager());
			this.containerManaged = containerManaged;
			this.synchronizedWithTransaction = synchronizedWithTransaction;
		}

		private boolean isJtaEntityManager() {
			try {
				this.target.getTransaction();
				return false;
			}
			catch (IllegalStateException ex) {
				logger.debug("Cannot access EntityTransaction handle - assuming we're in a JTA environment");
				return true;
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自EntityManager接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用EntityManager代理的hashCode.
				return hashCode();
			}
			else if (method.getName().equals("getTargetEntityManager")) {
				// 处理EntityManagerProxy接口.
				return this.target;
			}
			else if (method.getName().equals("unwrap")) {
				// 处理JPA 2.0 unwrap方法 - 可以是代理匹配.
				Class<?> targetClass = (Class<?>) args[0];
				if (targetClass == null) {
					return this.target;
				}
				else if (targetClass.isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isOpen")) {
				if (this.containerManaged) {
					return true;
				}
			}
			else if (method.getName().equals("close")) {
				if (this.containerManaged) {
					throw new IllegalStateException("Invalid usage: Cannot close a container-managed EntityManager");
				}
				ExtendedEntityManagerSynchronization synch = (ExtendedEntityManagerSynchronization)
						TransactionSynchronizationManager.getResource(this.target);
				if (synch != null) {
					// 加入本地事务 - 在事务完成之前不实际调用 close()
					synch.closeOnCompletion = true;
					return null;
				}
			}
			else if (method.getName().equals("getTransaction")) {
				if (this.synchronizedWithTransaction) {
					throw new IllegalStateException(
							"Cannot obtain local EntityTransaction from a transaction-synchronized EntityManager");
				}
			}
			else if (method.getName().equals("joinTransaction")) {
				doJoinTransaction(true);
				return null;
			}
			else if (method.getName().equals("isJoinedToTransaction")) {
				// 非JTA时, 处理JPA 2.1 isJoinedToTransaction方法.
				if (!this.jta) {
					return TransactionSynchronizationManager.hasResource(this.target);
				}
			}

			// 如果需要, 自动加入. 排除toString, equals, hashCode调用.
			if (this.synchronizedWithTransaction && method.getDeclaringClass().isInterface()) {
				doJoinTransaction(false);
			}

			// 在当前EntityManager上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * 加入现有事务, 如果尚未加入.
		 * 
		 * @param enforce 是否强制事务 (i.e. 加入失败是否被视为致命的)
		 */
		private void doJoinTransaction(boolean enforce) {
			if (this.jta) {
				// 是否在JTA事务中.
				try {
					this.target.joinTransaction();
					logger.debug("Joined JTA transaction");
				}
				catch (TransactionRequiredException ex) {
					if (!enforce) {
						logger.debug("No JTA transaction to join: " + ex);
					}
					else {
						throw ex;
					}
				}
			}
			else {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					if (!TransactionSynchronizationManager.hasResource(this.target) &&
							!this.target.getTransaction().isActive()) {
						enlistInCurrentTransaction();
					}
					logger.debug("Joined local transaction");
				}
				else {
					if (!enforce) {
						logger.debug("No local transaction to join");
					}
					else {
						throw new TransactionRequiredException("No local transaction to join");
					}
				}
			}
		}

		/**
		 * 在当前事务中登记此应用程序管理的EntityManager.
		 */
		private void enlistInCurrentTransaction() {
			// 资源本地事务, 需要获取EntityTransaction, 立即启动事务, 并在以后登记同步以进行提交或回滚.
			EntityTransaction et = this.target.getTransaction();
			et.begin();
			if (logger.isDebugEnabled()) {
				logger.debug("Starting resource-local transaction on application-managed " +
						"EntityManager [" + this.target + "]");
			}
			ExtendedEntityManagerSynchronization extendedEntityManagerSynchronization =
					new ExtendedEntityManagerSynchronization(this.target, this.exceptionTranslator);
			TransactionSynchronizationManager.bindResource(this.target, extendedEntityManagerSynchronization);
			TransactionSynchronizationManager.registerSynchronization(extendedEntityManagerSynchronization);
		}
	}


	/**
	 * 使用当前的Spring事务登记扩展的EntityManager的TransactionSynchronization.
	 */
	private static class ExtendedEntityManagerSynchronization
			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManager>
			implements Ordered {

		private final EntityManager entityManager;

		private final PersistenceExceptionTranslator exceptionTranslator;

		public volatile boolean closeOnCompletion = false;

		public ExtendedEntityManagerSynchronization(
				EntityManager em, PersistenceExceptionTranslator exceptionTranslator) {

			super(new EntityManagerHolder(em), em);
			this.entityManager = em;
			this.exceptionTranslator = exceptionTranslator;
		}

		@Override
		public int getOrder() {
			return EntityManagerFactoryUtils.ENTITY_MANAGER_SYNCHRONIZATION_ORDER - 1;
		}

		@Override
		protected void flushResource(EntityManagerHolder resourceHolder) {
			try {
				this.entityManager.flush();
			}
			catch (RuntimeException ex) {
				throw convertException(ex);
			}
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return false;
		}

		@Override
		public void afterCommit() {
			super.afterCommit();
			// 触发器在此处提交, 以允许异常传播到调用者.
			try {
				this.entityManager.getTransaction().commit();
			}
			catch (RuntimeException ex) {
				throw convertException(ex);
			}
		}

		@Override
		public void afterCompletion(int status) {
			try {
				super.afterCompletion(status);
				if (status != STATUS_COMMITTED) {
					// 没有进行afterCommit调用: 触发回滚.
					try {
						this.entityManager.getTransaction().rollback();
					}
					catch (RuntimeException ex) {
						throw convertException(ex);
					}
				}
			}
			finally {
				if (this.closeOnCompletion) {
					EntityManagerFactoryUtils.closeEntityManager(this.entityManager);
				}
			}
		}

		private RuntimeException convertException(RuntimeException ex) {
			DataAccessException daex = (this.exceptionTranslator != null) ?
					this.exceptionTranslator.translateExceptionIfPossible(ex) :
					EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex);
			return (daex != null ? daex : ex);
		}
	}
}
