package org.springframework.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

/**
 * Spring管理的JPA {@link EntityManagerFactory}的元数据接口.
 *
 * <p>通过将EntityManagerFactory句柄强制转换为此接口, 可以从Spring管理的EntityManagerFactory代理获取此工具.
 */
public interface EntityManagerFactoryInfo {

	/**
	 * 返回原始底层EntityManagerFactory.
	 * 
	 * @return 底层EntityManagerFactory (never {@code null})
	 */
	EntityManagerFactory getNativeEntityManagerFactory();

	/**
	 * 返回创建底层EntityManagerFactory的底层PersistenceProvider.
	 * 
	 * @return 用于创建此EntityManagerFactory的PersistenceProvider,
	 * 或{@code null} 如果使用标准JPA提供者自动检测进程来配置EntityManagerFactory
	 */
	PersistenceProvider getPersistenceProvider();

	/**
	 * 如果使用容器内API, 返回用于创建此EntityManagerFactory的PersistenceUnitInfo.
	 * 
	 * @return 用于创建此EntityManagerFactory的PersistenceUnitInfo,
	 * 或{@code null} 如果容器内约定未用于配置EntityManagerFactory
	 */
	PersistenceUnitInfo getPersistenceUnitInfo();

	/**
	 * 返回用于创建此EntityManagerFactory的持久性单元的名称, 或{@code null} 如果是未命名的默认值.
	 * <p>如果{@code getPersistenceUnitInfo()}返回非null,
	 * 则 {@code getPersistenceUnitName()}的结果必须等于{@code PersistenceUnitInfo.getPersistenceUnitName()}返回的值.
	 */
	String getPersistenceUnitName();

	/**
	 * 返回此EntityManagerFactory从中获取其JDBC连接的JDBC DataSource.
	 * 
	 * @return JDBC DataSource, 或{@code null}
	 */
	DataSource getDataSource();

	/**
	 * 返回此工厂的EntityManagers将实现的 (可能特定于供应商的) EntityManager接口.
	 * <p>{@code null}返回值表明应该发生自动检测: 基于目标{@code EntityManager}实例,
	 * 或者只是默认为{@code javax.persistence.EntityManager}.
	 */
	Class<? extends EntityManager> getEntityManagerInterface();

	/**
	 * 返回此EntityManagerFactory的特定于供应商的JpaDialect实现, 或{@code null}.
	 */
	JpaDialect getJpaDialect();

	/**
	 * 返回加载应用程序bean的ClassLoader.
	 * <p>将在此ClassLoader中生成代理.
	 */
	ClassLoader getBeanClassLoader();

}
