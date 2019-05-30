package org.springframework.orm.jpa.support;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean}, 公开给定EntityManagerFactory的共享JPA {@link javax.persistence.EntityManager}引用.
 * 通常用于由{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}创建的EntityManagerFactory,
 * 作为Java EE服务器的EntityManager引用的JNDI查找的直接替代.
 *
 * <p>共享的EntityManager的行为就像从应用程序服务器的JNDI环境中获取的EntityManager一样, 由JPA规范定义.
 * 如果有的话, 它会将所有调用委托给当前的事务性EntityManager; 否则, 它将回退到每个操作新创建的EntityManager.
 *
 * <p>可以传递给期望共享的EntityManager引用而不是EntityManagerFactory的DAO.
 * 请注意, Spring的 {@link org.springframework.orm.jpa.JpaTransactionManager}总是需要一个EntityManagerFactory,
 * 才能创建新的事务性EntityManager实例.
 */
public class SharedEntityManagerBean extends EntityManagerFactoryAccessor
		implements FactoryBean<EntityManager>, InitializingBean {

	private Class<? extends EntityManager> entityManagerInterface;

	private boolean synchronizedWithTransaction = true;

	private EntityManager shared;


	/**
	 * 指定要公开的EntityManager接口.
	 * <p>默认是EntityManagerFactoryInfo定义的EntityManager接口.
	 * 否则, 将使用标准的{@code javax.persistence.EntityManager}接口.
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> entityManagerInterface) {
		Assert.notNull(entityManagerInterface, "'entityManagerInterface' must not be null");
		this.entityManagerInterface = entityManagerInterface;
	}

	/**
	 * 设置是否自动加入正在进行的事务 (根据JPA 2.1 SynchronizationType规则).
	 * 默认 "true".
	 */
	public void setSynchronizedWithTransaction(boolean synchronizedWithTransaction) {
		this.synchronizedWithTransaction = synchronizedWithTransaction;
	}


	@Override
	public final void afterPropertiesSet() {
		EntityManagerFactory emf = getEntityManagerFactory();
		if (emf == null) {
			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
		}
		if (emf instanceof EntityManagerFactoryInfo) {
			EntityManagerFactoryInfo emfInfo = (EntityManagerFactoryInfo) emf;
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = emfInfo.getEntityManagerInterface();
				if (this.entityManagerInterface == null) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
		}
		else {
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = EntityManager.class;
			}
		}
		this.shared = SharedEntityManagerCreator.createSharedEntityManager(
				emf, getJpaPropertyMap(), this.synchronizedWithTransaction, this.entityManagerInterface);
	}


	@Override
	public EntityManager getObject() {
		return this.shared;
	}

	@Override
	public Class<? extends EntityManager> getObjectType() {
		return (this.entityManagerInterface != null ? this.entityManagerInterface : EntityManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
