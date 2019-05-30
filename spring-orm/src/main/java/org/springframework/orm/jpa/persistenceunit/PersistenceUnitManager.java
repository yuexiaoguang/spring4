package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.PersistenceUnitInfo;

/**
 * 定义用于查找和管理JPA PersistenceUnitInfos的抽象的接口.
 * 由{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}使用,
 * 获取{@link javax.persistence.spi.PersistenceUnitInfo}以构建具体的{@link javax.persistence.EntityManagerFactory}.
 *
 * <p>获取PersistenceUnitInfo实例是一个独占进程.
 * 一旦获得, PersistenceUnitInfo实例就不再可用于进一步调用.
 */
public interface PersistenceUnitManager {

	/**
	 * 从此管理器获取默认的PersistenceUnitInfo.
	 * 
	 * @return the PersistenceUnitInfo (never {@code null})
	 * @throws IllegalStateException 如果没有定义默认的PersistenceUnitInfo或已经获得它
	 */
	PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException;

	/**
	 * 从此管理器获取指定的PersistenceUnitInfo.
	 * 
	 * @param persistenceUnitName 所需的持久化单元的名称
	 * 
	 * @return the PersistenceUnitInfo (never {@code null})
	 * @throws IllegalArgumentException 如果没有定义具有给定名称的PersistenceUnitInfo
	 * @throws IllegalStateException 如果已经获得具有给定名称的PersistenceUnitInfo
	 */
	PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
			throws IllegalArgumentException, IllegalStateException;

}
