package org.springframework.orm.jdo.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jdo.DefaultJdoDialect;
import org.springframework.orm.jdo.JdoDialect;
import org.springframework.orm.jdo.PersistenceManagerFactoryUtils;
import org.springframework.util.Assert;

/**
 * 实现{@link javax.jdo.PersistenceManager}接口的代理, 在每次调用时委托给当前线程绑定的PersistenceManager
 * (Spring管理的事务PersistenceManager或单个OpenPersistenceManagerInView PersistenceManager).
 * 这个类使得这种Spring风格的PersistenceManager代理可用于bean引用.
 *
 * <p>这个代理的主要优点是它允许DAO使用JDO 3.0样式的普通JDO PersistenceManager引用
 * (参见{@link javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()}), 同时仍然参与Spring的资源和事务管理.
 *
 * <p>此代理的行为与JDO 3.0规范为PersistenceManager代理定义的行为相匹配.
 * 因此, DAO可以在{@link StandardPersistenceManagerProxyBean}和这个Spring风格的代理之间无缝切换, 通过依赖注入接收引用.
 * 这将在DAO代码中没有任何Spring API依赖项的情况下工作!
 */
public class SpringPersistenceManagerProxyBean implements FactoryBean<PersistenceManager>, InitializingBean {

	private PersistenceManagerFactory persistenceManagerFactory;

	private JdoDialect jdoDialect;

	private Class<? extends PersistenceManager> persistenceManagerInterface = PersistenceManager.class;

	private boolean allowCreate = true;

	private PersistenceManager proxy;


	/**
	 * 设置此代理的目标PersistenceManagerFactory.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory persistenceManagerFactory) {
		this.persistenceManagerFactory = persistenceManagerFactory;
	}

	/**
	 * 返回此代理的目标PersistenceManagerFactory.
	 */
	protected PersistenceManagerFactory getPersistenceManagerFactory() {
		return this.persistenceManagerFactory;
	}

	/**
	 * 设置用于此代理的JDO方言.
	 * <p>默认值是基于PersistenceManagerFactory的底层DataSource的DefaultJdoDialect.
	 */
	public void setJdoDialect(JdoDialect jdoDialect) {
		this.jdoDialect = jdoDialect;
	}

	/**
	 * 返回用于此代理的JDO方言.
	 */
	protected JdoDialect getJdoDialect() {
		return this.jdoDialect;
	}

	/**
	 * 指定要公开的PersistenceManager接口, 可能包括供应商扩展.
	 * <p>默认值是标准{@code javax.jdo.PersistenceManager}接口.
	 */
	public void setPersistenceManagerInterface(Class<? extends PersistenceManager> persistenceManagerInterface) {
		this.persistenceManagerInterface = persistenceManagerInterface;
		Assert.notNull(persistenceManagerInterface, "persistenceManagerInterface must not be null");
		Assert.isAssignable(PersistenceManager.class, persistenceManagerInterface);
	}

	/**
	 * 返回进行公开的PersistenceManager接口.
	 */
	protected Class<? extends PersistenceManager> getPersistenceManagerInterface() {
		return this.persistenceManagerInterface;
	}

	/**
	 * 设置当没有为当前线程找到事务性PersistenceManager时, 是否允许PersistenceManagerFactory代理创建非事务性PersistenceManager.
	 * <p>默认"true". 可以关闭以强制访问事务性PersistenceManagers, 它可以安全地允许编写的DAO获取PersistenceManager, 而无需显式关闭
	 * (i.e. {@code PersistenceManagerFactory.getPersistenceManager()}调用, 没有相应的{@code PersistenceManager.close()}调用).
	 */
	public void setAllowCreate(boolean allowCreate) {
		this.allowCreate = allowCreate;
	}

	/**
	 * 返回当没有为当前线程找到事务性PersistenceManager时,
	 * 是否允许PersistenceManagerFactory代理创建非事务性PersistenceManager.
	 */
	protected boolean isAllowCreate() {
		return this.allowCreate;
	}

	@Override
	public void afterPropertiesSet() {
		if (getPersistenceManagerFactory() == null) {
			throw new IllegalArgumentException("Property 'persistenceManagerFactory' is required");
		}
		// 如果没有明确指定, 则构建默认的JdoDialect.
		if (this.jdoDialect == null) {
			this.jdoDialect = new DefaultJdoDialect(getPersistenceManagerFactory().getConnectionFactory());
		}
		this.proxy = (PersistenceManager) Proxy.newProxyInstance(
				getPersistenceManagerFactory().getClass().getClassLoader(),
				new Class<?>[] {getPersistenceManagerInterface()}, new PersistenceManagerInvocationHandler());
	}


	@Override
	public PersistenceManager getObject() {
		return this.proxy;
	}

	@Override
	public Class<? extends PersistenceManager> getObjectType() {
		return getPersistenceManagerInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 调用处理器, 它将PersistenceManagers上的close调用委托给PersistenceManagerFactoryUtils, 以便了解线程绑定事务.
	 */
	private class PersistenceManagerInvocationHandler implements InvocationHandler {

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
			else if (method.getName().equals("toString")) {
				// 在不触及目标EntityManager的情况下传递toString.
				return "Spring PersistenceManager proxy for target factory [" + getPersistenceManagerFactory() + "]";
			}
			else if (method.getName().equals("getPersistenceManagerFactory")) {
				// 返回PersistenceManagerFactory, 而不创建PersistenceManager.
				return getPersistenceManagerFactory();
			}
			else if (method.getName().equals("isClosed")) {
				// 代理始终可用.
				return false;
			}
			else if (method.getName().equals("close")) {
				// 抑制关闭方法.
				return null;
			}

			// 在目标PersistenceManager上调用方法.
			PersistenceManager pm = PersistenceManagerFactoryUtils.doGetPersistenceManager(
					getPersistenceManagerFactory(), isAllowCreate());
			try {
				Object retVal = method.invoke(pm, args);
				if (retVal instanceof Query) {
					PersistenceManagerFactoryUtils.applyTransactionTimeout(
							(Query) retVal, getPersistenceManagerFactory());
				}
				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
			finally {
				PersistenceManagerFactoryUtils.doReleasePersistenceManager(pm, getPersistenceManagerFactory());
			}
		}
	}
}
