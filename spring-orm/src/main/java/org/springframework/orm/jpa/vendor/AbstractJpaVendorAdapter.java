package org.springframework.orm.jpa.vendor;

import java.util.Collections;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaVendorAdapter;

/**
 * 抽象{@link JpaVendorAdapter}实现, 定义公共属性, 由具体子类转换为特定于供应商的JPA属性.
 */
public abstract class AbstractJpaVendorAdapter implements JpaVendorAdapter {

	private Database database = Database.DEFAULT;

	private String databasePlatform;

	private boolean generateDdl = false;

	private boolean showSql = false;


	/**
	 * 指定要对其进行操作的目标数据库, 作为{@code Database}枚举的值:
	 * DB2, DERBY, H2, HSQL, INFORMIX, MYSQL, ORACLE, POSTGRESQL, SQL_SERVER, SYBASE
	 * <p><b>NOTE:</b> 此设置将覆盖JPA提供者的默认算法.
	 * 自定义供应商属性仍可以微调数据库方言.
	 * 但是, 可能存在冲突:
	 * 例如, 指定此设置, 或Hibernate的"hibernate.dialect_resolvers"属性, 而不是两个都指定.
	 */
	public void setDatabase(Database database) {
		this.database = database;
	}

	/**
	 * 返回要进行操作的目标数据库.
	 */
	protected Database getDatabase() {
		return this.database;
	}

	/**
	 * 指定要操作的目标数据库的名称.
	 * 支持的值是与供应商相关的平台标识符.
	 */
	public void setDatabasePlatform(String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	/**
	 * 返回要操作的目标数据库的名称.
	 */
	protected String getDatabasePlatform() {
		return this.databasePlatform;
	}

	/**
	 * 设置是否在初始化EntityManagerFactory后生成DDL, 创建/更新所有相关表.
	 * <p>请注意, 此标志的确切语义取决于底层持久化提供者.
	 * 对于任何更高级的需求, 请将适当的特定于供应商的设置指定为"jpaProperties".
	 * <p><b>NOTE: 在设置JPA 2.1的{@code javax.persistence.schema-generation.database.action}属性时, 不要将此标志设置为'true'.</b>
	 * 这两种模式生成机制 - 标准JPA与本机提供者 - 是互斥的, e.g. 与Hibernate 5.
	 */
	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	/**
	 * 在初始化EntityManagerFactory后, 是否生成DDL, 创建/更新所有相关表.
	 */
	protected boolean isGenerateDdl() {
		return this.generateDdl;
	}

	/**
	 * 设置是否在日志中 (或在控制台中)显示SQL.
	 * <p>对于更具体的日志记录配置, 请将适当的特定于供应商的设置指定为"jpaProperties".
	 */
	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	/**
	 * 返回是否在日志中 (或在控制台中)显示SQL.
	 */
	protected boolean isShowSql() {
		return this.showSql;
	}


	@Override
	public String getPersistenceProviderRootPackage() {
		return null;
	}

	@Override
	public Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return getJpaPropertyMap();
	}

	@Override
	public Map<String, ?> getJpaPropertyMap() {
		return Collections.emptyMap();
	}

	@Override
	public JpaDialect getJpaDialect() {
		return null;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return EntityManagerFactory.class;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return EntityManager.class;
	}

	@Override
	public void postProcessEntityManagerFactory(EntityManagerFactory emf) {
	}

}
