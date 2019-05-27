package org.springframework.orm.jdo.support;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * 实现{@link javax.jdo.PersistenceManager}接口的代理, 在每次调用时委托给线程绑定的PersistenceManager - 由JDO 3.0规范定义.
 * 这个类使得这样一个标准的JDO PersistenceManager代理可用于bean引用.
 *
 * <p>这个代理的主要优点是它允许DAO使用JDO 3.0风格的普通JDO PersistenceManager引用
 * (see {@link javax.jdo.PersistenceManagerFactory#getPersistenceManagerProxy()}),
 * 公开目标JDO提供者实现的确切行为.
 */
public class StandardPersistenceManagerProxyBean implements FactoryBean<PersistenceManager> {

	private PersistenceManager proxy;


	/**
	 * 设置此代理应委托给的目标JDO PersistenceManagerFactory.
	 * 这应该是原始的PersistenceManagerFactory, 由JdoTransactionManager访问.
	 */
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		Assert.notNull(pmf, "PersistenceManagerFactory must not be null");
		this.proxy = pmf.getPersistenceManagerProxy();
	}


	@Override
	public PersistenceManager getObject() {
		return this.proxy;
	}

	@Override
	public Class<? extends PersistenceManager> getObjectType() {
		return (this.proxy != null ? this.proxy.getClass() : PersistenceManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
