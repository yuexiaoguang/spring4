package org.springframework.orm.jpa.persistenceunit;

import java.util.List;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * 扩展标准JPA PersistenceUnitInfo接口, 用于Spring的{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * 和{@link PersistenceUnitManager}实现之间的高级协作.
 */
public interface SmartPersistenceUnitInfo extends PersistenceUnitInfo {

	/**
	 * 返回管理的Java包的列表, 由持久化提供者进行内省.
	 * 通常通过扫描找到, 但不能通过{@link #getManagedClassNames()}公开.
	 * 
	 * @return 管理的Java包的名称列表 (可能为空)
	 */
	List<String> getManagedPackages();

	/**
	 * 设置持久化提供者自己的程序包名称, 以排除类转换.
	 */
	void setPersistenceProviderPackageName(String persistenceProviderPackageName);

}
