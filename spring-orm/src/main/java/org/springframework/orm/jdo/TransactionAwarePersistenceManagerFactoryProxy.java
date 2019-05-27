package org.springframework.orm.jdo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 目标JDO {@link javax.jdo.PersistenceManagerFactory}的代理,
 * 在{@code getPersistenceManager()}上返回当前线程绑定的PersistenceManager
 * (Spring管理的事务PersistenceManager或单个OpenPersistenceManagerInView PersistenceManager).
 *
 * <p>基本上, {@code getPersistenceManager()}调用无缝转发到{@link PersistenceManagerFactoryUtils#getPersistenceManager}.
 * 此外, {@code PersistenceManager.close}调用转发到{@link PersistenceManagerFactoryUtils#releasePersistenceManager}.
 *
 * <p>这个代理的主要优点是它允许DAO使用普通的JDO PersistenceManagerFactory引用, 同时仍然参与Spring的 (或J2EE服务器的)资源和事务管理.
 * 在这种情况下, DAO只依赖于JDO API, 没有任何Spring依赖性.
 *
 * <p>请注意, 当部署在J2EE服务器中时, 此代理的行为符合JDO规范为JCA连接器公开的PersistenceManagerFactory定义的行为.
 * 因此, DAO可以在JNDI PersistenceManagerFactory和本地PersistenceManagerFactory的代理之间无缝切换, 通过依赖注入接收引用.
 * 这将在DAO代码中没有任何Spring API依赖项的情况下工作!
 *
 * <p>当然, 即使DAO通过此代理, 仍然可以访问目标 PersistenceManagerFactory,
 * 通过定义直接指向目标PersistenceManagerFactory bean的bean引用.
 */
public class TransactionAwarePersistenceManagerFactoryProxy implements FactoryBean<PersistenceManagerFactory> {

	private PersistenceManagerFactory target;

	private boolean allowCreate = true;

	private PersistenceManagerFactory proxy;


	/**
	 * 设置此代理应委托给的目标JDO PersistenceManagerFactory.
	 * 这应该是原始的PersistenceManagerFactory, 由JdoTransactionManager访问.
	 */
	public void setTargetPersistenceManagerFactory(PersistenceManagerFactory target) {
		Assert.notNull(target, "Target PersistenceManagerFactory must not be null");
		this.target = target;
		Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(target.getClass(), target.getClass().getClassLoader());
		this.proxy = (PersistenceManagerFactory) Proxy.newProxyInstance(
				target.getClass().getClassLoader(), ifcs, new PersistenceManagerFactoryInvocationHandler());
	}

	/**
	 * 返回此代理委派给的目标JDO PersistenceManagerFactory.
	 */
	public PersistenceManagerFactory getTargetPersistenceManagerFactory() {
		return this.target;
	}

	/**
	 * 设置当没有为当前线程找到事务性PersistenceManager时, 是否允许PersistenceManagerFactory代理创建非事务性PersistenceManager.
	 * <p>默认"true".
	 * 可以关闭以强制访问事务性PersistenceManagers, 它可以安全地允许编写的DAO获取PersistenceManager而无需显式关闭
	 * (i.e. 没有相应的{@code PersistenceManager.close()}调用的{@code PersistenceManagerFactory.getPersistenceManager()}调用).
	 */
	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	/**
	 * 返回当没有为当前线程找到事务性PersistenceManager时, 是否允许PersistenceManagerFactory代理创建非事务性PersistenceManager.
	 */
	protected boolean isAllowCreate() {
		return this.allowCreate;
	}


	@Override
	public PersistenceManagerFactory getObject() {
		return this.proxy;
	}

	@Override
	public Class<? extends PersistenceManagerFactory> getObjectType() {
		return PersistenceManagerFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 调用处理程序, 它将PersistenceManagerFactory代理上的getPersistenceManager调用委托给PersistenceManagerFactoryUtils, 以便了解线程绑定事务.
	 */
	private class PersistenceManagerFactoryInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自PersistenceManagerFactory接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用PersistenceManagerFactory代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("getPersistenceManager")) {
				PersistenceManagerFactory target = getTargetPersistenceManagerFactory();
				PersistenceManager pm =
						PersistenceManagerFactoryUtils.doGetPersistenceManager(target, isAllowCreate());
				Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(pm.getClass(), pm.getClass().getClassLoader());
				return Proxy.newProxyInstance(
						pm.getClass().getClassLoader(), ifcs, new PersistenceManagerInvocationHandler(pm, target));
			}

			// 在目标PersistenceManagerFactory上调用方法.
			try {
				return method.invoke(getTargetPersistenceManagerFactory(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * 调用处理程序, 它将PersistenceManagers上的close调用委托给PersistenceManagerFactoryUtils, 以便了解线程绑定事务.
	 */
	private static class PersistenceManagerInvocationHandler implements InvocationHandler {

		private final PersistenceManager target;

		private final PersistenceManagerFactory persistenceManagerFactory;

		public PersistenceManagerInvocationHandler(PersistenceManager target, PersistenceManagerFactory pmf) {
			this.target = target;
			this.persistenceManagerFactory = pmf;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// 来自PersistenceManager接口的调用...

			if (method.getName().equals("equals")) {
				// 只有当代理相同时才考虑相等.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// 使用PersistenceManager代理的hashCode.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("close")) {
				// 处理关闭方法: 仅在非事务中关闭.
				PersistenceManagerFactoryUtils.doReleasePersistenceManager(
						this.target, this.persistenceManagerFactory);
				return null;
			}

			// 在目标PersistenceManager上调用方法.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
