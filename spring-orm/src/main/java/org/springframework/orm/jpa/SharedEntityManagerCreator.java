package org.springframework.orm.jpa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 委托为给定的{@link javax.persistence.EntityManagerFactory}
 * 创建可共享的JPA {@link javax.persistence.EntityManager}引用.
 *
 * <p>共享的EntityManager的行为就像从应用程序服务器的JNDI环境获取的EntityManager, 如JPA规范所定义.
 * 如果有的话, 它会将所有调用委托给当前的事务性EntityManager; 否则它将回退到每个操作新创建的EntityManager.
 *
 * <p>有关此类共享的事务性EntityManager的行为定义,
 * 请参阅JPA规范文档中的{@link javax.persistence.PersistenceContextType#TRANSACTION}及其讨论.
 * 这也是基于注解的{@link javax.persistence.PersistenceContext#type()}的默认值.
 */
public abstract class SharedEntityManagerCreator {

	private static final Class<?>[] NO_ENTITY_MANAGER_INTERFACES = new Class<?>[0];

	private static final Set<String> transactionRequiringMethods = new HashSet<String>(8);

	private static final Set<String> queryTerminatingMethods = new HashSet<String>(8);

	static {
		transactionRequiringMethods.add("joinTransaction");
		transactionRequiringMethods.add("flush");
		transactionRequiringMethods.add("persist");
		transactionRequiringMethods.add("merge");
		transactionRequiringMethods.add("remove");
		transactionRequiringMethods.add("refresh");

		queryTerminatingMethods.add("execute");  // JPA 2.1 StoredProcedureQuery
		queryTerminatingMethods.add("executeUpdate");
		queryTerminatingMethods.add("getSingleResult");
		queryTerminatingMethods.add("getResultList");
		queryTerminatingMethods.add("getResultStream");
	}


	/**
	 * 为给定的EntityManagerFactory创建事务性EntityManager代理.
	 * 
	 * @param emf 委托给的EntityManagerFactory.
	 * 
	 * @return 可共享的事务EntityManager代理
	 */
	public static EntityManager createSharedEntityManager(EntityManagerFactory emf) {
		return createSharedEntityManager(emf, null, true);
	}

	/**
	 * 为给定的EntityManagerFactory创建事务性EntityManager代理.
	 * 
	 * @param emf 委托给的EntityManagerFactory.
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * 
	 * @return 可共享的事务EntityManager代理
	 */
	public static EntityManager createSharedEntityManager(EntityManagerFactory emf, Map<?, ?> properties) {
		return createSharedEntityManager(emf, properties, true);
	}

	/**
	 * 为给定的EntityManagerFactory创建事务性EntityManager代理.
	 * 
	 * @param emf 委托给的EntityManagerFactory.
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * 
	 * @return 可共享的事务EntityManager代理
	 */
	public static EntityManager createSharedEntityManager(
			EntityManagerFactory emf, Map<?, ?> properties, boolean synchronizedWithTransaction) {

		Class<?> emIfc = (emf instanceof EntityManagerFactoryInfo ?
				((EntityManagerFactoryInfo) emf).getEntityManagerInterface() : EntityManager.class);
		return createSharedEntityManager(emf, properties, synchronizedWithTransaction,
				(emIfc == null ? NO_ENTITY_MANAGER_INTERFACES : new Class<?>[] {emIfc}));
	}

	/**
	 * 为给定的EntityManagerFactory创建事务性EntityManager代理.
	 * 
	 * @param emf 从中根据需要获取EntityManager的EntityManagerFactory
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * @param entityManagerInterfaces EntityManager要实现的接口. 允许添加或指定专有接口.
	 * 
	 * @return 可共享的事务EntityManager代理
	 */
	public static EntityManager createSharedEntityManager(
			EntityManagerFactory emf, Map<?, ?> properties, Class<?>... entityManagerInterfaces) {

		return createSharedEntityManager(emf, properties, true, entityManagerInterfaces);
	}

	/**
	 * 为给定的EntityManagerFactory创建事务性EntityManager代理.
	 * 
	 * @param emf 从中根据需要获取EntityManager的EntityManagerFactory
	 * @param properties 要传递到{@code createEntityManager}调用的属性 (may be {@code null})
	 * @param synchronizedWithTransaction 是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则)
	 * @param entityManagerInterfaces EntityManager要实现的接口. 允许添加或指定专有接口.
	 * 
	 * @return 可共享的事务EntityManager代理
	 */
	public static EntityManager createSharedEntityManager(EntityManagerFactory emf, Map<?, ?> properties,
			boolean synchronizedWithTransaction, Class<?>... entityManagerInterfaces) {

		ClassLoader cl = null;
		if (emf instanceof EntityManagerFactoryInfo) {
			cl = ((EntityManagerFactoryInfo) emf).getBeanClassLoader();
		}
		Class<?>[] ifcs = new Class<?>[entityManagerInterfaces.length + 1];
		System.arraycopy(entityManagerInterfaces, 0, ifcs, 0, entityManagerInterfaces.length);
		ifcs[entityManagerInterfaces.length] = EntityManagerProxy.class;
		return (EntityManager) Proxy.newProxyInstance(
				(cl != null ? cl : SharedEntityManagerCreator.class.getClassLoader()),
				ifcs, new SharedEntityManagerInvocationHandler(emf, properties, synchronizedWithTransaction));
	}


	/**
	 * 调用处理器, 它将所有调用委托给当前事务性EntityManager; 否则, 它将回退到每个操作新创建的EntityManager.
	 */
	@SuppressWarnings("serial")
	private static class SharedEntityManagerInvocationHandler implements InvocationHandler, Serializable {

		private final Log logger = LogFactory.getLog(getClass());

		private final EntityManagerFactory targetFactory;

		private final Map<?, ?> properties;

		private final boolean synchronizedWithTransaction;

		private transient volatile ClassLoader proxyClassLoader;

		public SharedEntityManagerInvocationHandler(
				EntityManagerFactory target, Map<?, ?> properties, boolean synchronizedWithTransaction) {

			this.targetFactory = target;
			this.properties = properties;
			this.synchronizedWithTransaction = synchronizedWithTransaction;
			initProxyClassLoader();
		}

		private void initProxyClassLoader() {
			if (this.targetFactory instanceof EntityManagerFactoryInfo) {
				this.proxyClassLoader = ((EntityManagerFactoryInfo) this.targetFactory).getBeanClassLoader();
			}
			else {
				this.proxyClassLoader = this.targetFactory.getClass().getClassLoader();
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
			else if (method.getName().equals("toString")) {
				// 在不触及目标EntityManager的情况下传递toString.
				return "Shared EntityManager proxy for target factory [" + this.targetFactory + "]";
			}
			else if (method.getName().equals("getEntityManagerFactory")) {
				// JPA 2.0: 返回EntityManagerFactory, 而不创建EntityManager.
				return this.targetFactory;
			}
			else if (method.getName().equals("getCriteriaBuilder") || method.getName().equals("getMetamodel")) {
				// JPA 2.0: 返回EntityManagerFactory的 CriteriaBuilder/Metamodel (避免创建EntityManager)
				try {
					return EntityManagerFactory.class.getMethod(method.getName()).invoke(this.targetFactory);
				}
				catch (InvocationTargetException ex) {
					throw ex.getTargetException();
				}
			}
			else if (method.getName().equals("unwrap")) {
				// JPA 2.0: 处理unwrap方法 - 可以是代理匹配.
				Class<?> targetClass = (Class<?>) args[0];
				if (targetClass != null && targetClass.isInstance(proxy)) {
					return proxy;
				}
			}
			else if (method.getName().equals("isOpen")) {
				// 处理isOpen方法: 总是返回 true.
				return true;
			}
			else if (method.getName().equals("close")) {
				// 处理close方法: 抑制, 无效.
				return null;
			}
			else if (method.getName().equals("getTransaction")) {
				throw new IllegalStateException(
						"Not allowed to create transaction on shared EntityManager - " +
						"use Spring transactions or EJB CMT instead");
			}

			// 确定当前的EntityManager: 由工厂管理的事务管理器, 或给定调用的临时管理器.
			EntityManager target = EntityManagerFactoryUtils.doGetTransactionalEntityManager(
					this.targetFactory, this.properties, this.synchronizedWithTransaction);

			if (method.getName().equals("getTargetEntityManager")) {
				// 处理EntityManagerProxy接口.
				if (target == null) {
					throw new IllegalStateException("No transactional EntityManager available");
				}
				return target;
			}
			else if (method.getName().equals("unwrap")) {
				Class<?> targetClass = (Class<?>) args[0];
				if (targetClass == null) {
					return (target != null ? target : proxy);
				}
				// 现在需要一个事务目标.
				if (target == null) {
					throw new IllegalStateException("No transactional EntityManager available");
				}
				// 仍然在目标EntityManager上执行unwrap调用.
			}
			else if (transactionRequiringMethods.contains(method.getName())) {
				// 根据JPA规范, 现在需要一个事务目标.
				// 否则, 操作将被接受, 但仍保持未刷新状态...
				if (target == null || (!TransactionSynchronizationManager.isActualTransactionActive() &&
						!target.getTransaction().isActive())) {
					throw new TransactionRequiredException("No EntityManager with actual transaction available " +
							"for current thread - cannot reliably process '" + method.getName() + "' call");
				}
			}

			// 常规EntityManager操作.
			boolean isNewEm = false;
			if (target == null) {
				logger.debug("Creating new EntityManager for shared EntityManager invocation");
				target = (!CollectionUtils.isEmpty(this.properties) ?
						this.targetFactory.createEntityManager(this.properties) :
						this.targetFactory.createEntityManager());
				isNewEm = true;
			}

			// 在当前EntityManager上调用方法.
			try {
				Object result = method.invoke(target, args);
				if (result instanceof Query) {
					Query query = (Query) result;
					if (isNewEm) {
						Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(query.getClass(), this.proxyClassLoader);
						result = Proxy.newProxyInstance(this.proxyClassLoader, ifcs,
								new DeferredQueryInvocationHandler(query, target));
						isNewEm = false;
					}
					else {
						EntityManagerFactoryUtils.applyTransactionTimeout(query, this.targetFactory);
					}
				}
				return result;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (isNewEm) {
					EntityManagerFactoryUtils.closeEntityManager(target);
				}
			}
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			// 依赖于默认序列化, 只需在反序列化后初始化状态.
			ois.defaultReadObject();
			// 初始化transient字段.
			initProxyClassLoader();
		}
	}


	/**
	 * 调用处理器, 它处理由共享EntityManager上的非事务性createQuery调用创建的延迟Query对象.
	 */
	private static class DeferredQueryInvocationHandler implements InvocationHandler {

		private final Query target;

		private EntityManager entityManager;

		public DeferredQueryInvocationHandler(Query target, EntityManager entityManager) {
			this.target = target;
			this.entityManager = entityManager;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自Query接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用EntityManager代理的hashCode.
				return hashCode();
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

			// 在实际的Query对象上调用方法.
			try {
				Object retVal = method.invoke(this.target, args);
				return (retVal == this.target ? proxy : retVal);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				if (queryTerminatingMethods.contains(method.getName())) {
					// 实际执行查询: 之后关闭EntityManager, 因为这是保持打开的唯一原因.
					EntityManagerFactoryUtils.closeEntityManager(this.entityManager);
					this.entityManager = null;
				}
			}
		}
	}

}
