package org.springframework.orm.jpa.vendor;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.PersistenceProviderImpl;

/**
 * Apache OpenJPA的{@link org.springframework.orm.jpa.JpaVendorAdapter}实现.
 * 针对OpenJPA 2.2开发和测试.
 *
 * <p>公开OpenJPA的持久化提供者和EntityManager扩展接口, 并调整{@link AbstractJpaVendorAdapter}的常用配置设置.
 * 由于OpenJPA不使用包级元数据, 因此不支持检测带注解的包 (通过
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}).
 */
public class OpenJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final PersistenceProvider persistenceProvider = new PersistenceProviderImpl();

	private final OpenJpaDialect jpaDialect = new OpenJpaDialect();


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public String getPersistenceProviderRootPackage() {
		return "org.apache.openjpa";
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		Map<String, Object> jpaProperties = new HashMap<String, Object>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put("openjpa.jdbc.DBDictionary", getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			String databaseDictonary = determineDatabaseDictionary(getDatabase());
			if (databaseDictonary != null) {
				jpaProperties.put("openjpa.jdbc.DBDictionary", databaseDictonary);
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
		}
		if (isShowSql()) {
			// Taken from the OpenJPA 0.9.6 docs ("Standard OpenJPA Log Configuration + All SQL Statements")
			jpaProperties.put("openjpa.Log", "DefaultLevel=WARN, Runtime=INFO, Tool=INFO, SQL=TRACE");
		}

		return jpaProperties;
	}

	/**
	 * 确定给定数据库的OpenJPA数据库字典名称.
	 * 
	 * @param database 指定的数据库
	 * 
	 * @return OpenJPA数据库字典名称, 或{@code null}
	 */
	protected String determineDatabaseDictionary(Database database) {
		switch (database) {
			case DB2: return "db2";
			case DERBY: return "derby";
			case HSQL: return "hsql(SimulateLocking=true)";
			case INFORMIX: return "informix";
			case MYSQL: return "mysql";
			case ORACLE: return "oracle";
			case POSTGRESQL: return "postgres";
			case SQL_SERVER: return "sqlserver";
			case SYBASE: return "sybase";
			default: return null;
		}
	}

	@Override
	public OpenJpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return OpenJPAEntityManagerFactorySPI.class;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return OpenJPAEntityManagerSPI.class;
	}

}
