package org.springframework.orm.jpa.persistenceunit;

/**
 * 用于后处理JPA PersistenceUnitInfo的回调接口.
 * 实现可以使用DefaultPersistenceUnitManager 或 LocalContainerEntityManagerFactoryBean注册.
 */
public interface PersistenceUnitPostProcessor {

	/**
	 * 对给定的PersistenceUnitInfo进行后处理, 例如注册其他实体类和jar文件.
	 * 
	 * @param pui 从{@code persistence.xml}中读取的选择的PersistenceUnitInfo.
	 */
	void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui);

}
