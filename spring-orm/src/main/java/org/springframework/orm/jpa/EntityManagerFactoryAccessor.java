package org.springframework.orm.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 需要访问JPA {@link EntityManagerFactory}的类的基类, 通常为了获得JPA {@link EntityManager}.
 * Defines common properties.
 */
public abstract class EntityManagerFactoryAccessor implements BeanFactoryAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private EntityManagerFactory entityManagerFactory;

	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();


	/**
	 * 设置应该用于创建EntityManagers的JPA EntityManagerFactory.
	 */
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.entityManagerFactory = emf;
	}

	/**
	 * 返回应该用于创建EntityManagers的JPA EntityManagerFactory.
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}

	/**
	 * 设置持久性单元的名称以访问EntityManagerFactory.
	 * <p>这是通过直接引用指定EntityManagerFactory的替代方法, 而是通过其持久性单元名称来解析它.
	 * 如果未指定EntityManagerFactory, 且没有指定持久性单元名称,
	 * 则将通过查找EntityManagerFactory类型的单个唯一bean来检索默认的EntityManagerFactory.
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回持久性单元的名称以访问EntityManagerFactory.
	 */
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定JPA属性, 传递到{@code EntityManagerFactory.createEntityManager(Map)}.
	 * <p>可以使用String "value" (通过PropertiesEditor解析) 或XML bean定义中的"props"元素填充.
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 将JPA属性指定为Map, 以传递到{@code EntityManagerFactory.createEntityManager(Map)}.
	 * <p>可以在XML bean定义中使用"map" 或 "props"元素填充.
	 */
	public void setJpaPropertyMap(Map<String, Object> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许将对JPA属性的Map访问传递给持久性提供者, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过 "jpaPropertyMap[myKey]".
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * 如果没有显式设置, 则通过持久性单元名称检索EntityManagerFactory.
	 * 如果未指定持久性单元, 则回退到默认的EntityManagerFactory bean.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getEntityManagerFactory() == null) {
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
						"in a non-listable BeanFactory: " + beanFactory);
			}
			ListableBeanFactory lbf = (ListableBeanFactory) beanFactory;
			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
		}
	}


	/**
	 * 从此访问者的EntityManagerFactory获取新的EntityManager.
	 * <p>可以在子类中重写以创建特定的EntityManager变体.
	 * 
	 * @return 新的EntityManager
	 * @throws IllegalStateException 如果此访问者未配置EntityManagerFactory
	 */
	protected EntityManager createEntityManager() throws IllegalStateException {
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "No EntityManagerFactory specified");
		Map<String, Object> properties = getJpaPropertyMap();
		return (!CollectionUtils.isEmpty(properties) ? emf.createEntityManager(properties) : emf.createEntityManager());
	}

	/**
	 * 获取此访问者的EntityManagerFactory的事务性EntityManager.
	 * 
	 * @return 事务性EntityManager, 或{@code null}
	 * @throws IllegalStateException 如果此访问者未配置EntityManagerFactory
	 */
	protected EntityManager getTransactionalEntityManager() throws IllegalStateException{
		EntityManagerFactory emf = getEntityManagerFactory();
		Assert.state(emf != null, "No EntityManagerFactory specified");
		return EntityManagerFactoryUtils.getTransactionalEntityManager(emf, getJpaPropertyMap());
	}

}
