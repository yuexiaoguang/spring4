package org.springframework.orm.jpa.vendor;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServer2008Dialect;

/**
 * Hibernate EntityManager的{@link org.springframework.orm.jpa.JpaVendorAdapter}实现.
 * 针对Hibernate 3.6, 4.2/4.3以及5.x开发和测试.
 * <b>强烈建议将Hibernate 4.2+与Spring 4.0+一起使用.</b>
 *
 * <p>公开Hibernate的持久化提供者和EntityManager扩展接口, 并调整{@link AbstractJpaVendorAdapter}的常用配置设置.
 * 还支持检测带注解的包 (通过
 * {@link org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * e.g. 包含Hibernate {@link org.hibernate.annotations.FilterDef}注解,
 * 以及Spring驱动的实体扫描, 不需要{@code persistence.xml}
 * ({@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}).
 *
 * <p>请注意, Hibernate的JPA支持的包位置从4.2更改为4.3:
 * 从{@code org.hibernate.ejb.HibernateEntityManager(Factory)}到{@code org.hibernate.jpa.HibernateEntityManager(Factory)}.
 * 从Spring 4.0开始, 将根据运行时遇到的Hibernate版本公开正确的不弃用的变体, 以避免弃用日志条目.
 *
 * <p><b>关于{@code HibernateJpaVendorAdapter}与原生Hibernate设置的说明:</b>
 * 此适配器上的某些设置可能与本机Hibernate配置规则或自定义Hibernate属性冲突.
 * 例如, 指定{@link #setDatabase}, 或Hibernate的"hibernate.dialect_resolvers"属性, 而不是两个都指定.
 * 另外, 请注意Hibernate的连接释放模式: 此适配器更喜欢{@code ON_CLOSE}行为,
 * 与{@link HibernateJpaDialect#setPrepareConnection}保持一致, 至少对于非JTA场景;
 * 可以通过相应的本机Hibernate属性覆盖它.
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

	private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

	private final PersistenceProvider persistenceProvider;

	private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private final Class<? extends EntityManager> entityManagerInterface;


	@SuppressWarnings("unchecked")
	public HibernateJpaVendorAdapter() {
		ClassLoader cl = HibernateJpaVendorAdapter.class.getClassLoader();
		Class<? extends EntityManagerFactory> emfIfcToUse;
		Class<? extends EntityManager> emIfcToUse;
		Class<?> providerClass;
		PersistenceProvider providerToUse;
		try {
			try {
				// 尝试使用 Hibernate 4.3/5.0的 org.hibernate.jpa包, 以避免弃用警告
				emfIfcToUse = (Class<? extends EntityManagerFactory>) cl.loadClass("org.hibernate.jpa.HibernateEntityManagerFactory");
				emIfcToUse = (Class<? extends EntityManager>) cl.loadClass("org.hibernate.jpa.HibernateEntityManager");
				providerClass = cl.loadClass("org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider");
			}
			catch (ClassNotFoundException ex) {
				// 回退到Hibernate 3.6-4.2 org.hibernate.ejb包
				emfIfcToUse = (Class<? extends EntityManagerFactory>) cl.loadClass("org.hibernate.ejb.HibernateEntityManagerFactory");
				emIfcToUse = (Class<? extends EntityManager>) cl.loadClass("org.hibernate.ejb.HibernateEntityManager");
				providerClass = cl.loadClass("org.springframework.orm.jpa.vendor.SpringHibernateEjbPersistenceProvider");
			}
			providerToUse = (PersistenceProvider) providerClass.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to determine Hibernate PersistenceProvider", ex);
		}
		this.persistenceProvider = providerToUse;
		this.entityManagerFactoryInterface = emfIfcToUse;
		this.entityManagerInterface = emIfcToUse;
	}


	/**
	 * 设置是否准备事务性Hibernate会话的底层JDBC连接, 即是否将特定于事务的隔离级别和/或事务的只读标志应用于底层JDBC连接.
	 * <p>有关详细信息, 请参阅{@link HibernateJpaDialect#setPrepareConnection(boolean)}.
	 * 这只是传递给{@code HibernateJpaDialect}的便利标志.
	 * <p>在Hibernate 5.1/5.2上, 默认情况下此标志保持{@code true}, 就像以前的Hibernate版本一样.
	 * 在这种情况下, 供应商适配器会手动强制Hibernate的新连接处理模式{@code DELAYED_ACQUISITION_AND_HOLD},
	 * 除非用户指定的连接处理模式属性另有说明;
	 * 将此标志切换为{@code false}以避免干扰.
	 * <p><b>NOTE: 对于事务类型为JTA的持久化单元(如WebLogic), 连接释放模式永远不会从其提供者默认值更改,
	 * i.e. 不会被此标志强制为{@code DELAYED_ACQUISITION_AND_HOLD}.</b>
	 * 或者, 在这种情况下, 将Hibernate 5.2的"hibernate.connection.handling_mode"属性
	 * 设置为"DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION"或甚至"DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT".
	 */
	public void setPrepareConnection(boolean prepareConnection) {
		this.jpaDialect.setPrepareConnection(prepareConnection);
	}


	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	@Override
	public String getPersistenceProviderRootPackage() {
		return "org.hibernate";
	}

	@Override
	public Map<String, Object> getJpaPropertyMap(PersistenceUnitInfo pui) {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection &&
				pui.getTransactionType() != PersistenceUnitTransactionType.JTA);
	}

	@Override
	public Map<String, Object> getJpaPropertyMap() {
		return buildJpaPropertyMap(this.jpaDialect.prepareConnection);
	}

	private Map<String, Object> buildJpaPropertyMap(boolean connectionReleaseOnClose) {
		Map<String, Object> jpaProperties = new HashMap<String, Object>();

		if (getDatabasePlatform() != null) {
			jpaProperties.put(Environment.DIALECT, getDatabasePlatform());
		}
		else if (getDatabase() != null) {
			Class<?> databaseDialectClass = determineDatabaseDialectClass(getDatabase());
			if (databaseDialectClass != null) {
				jpaProperties.put(Environment.DIALECT, databaseDialectClass.getName());
			}
		}

		if (isGenerateDdl()) {
			jpaProperties.put(Environment.HBM2DDL_AUTO, "update");
		}
		if (isShowSql()) {
			jpaProperties.put(Environment.SHOW_SQL, "true");
		}

		if (connectionReleaseOnClose) {
			// Hibernate 5.1/5.2: 手动强制连接释放模式ON_CLOSE (以前的默认值)
			try {
				// Try Hibernate 5.2
				Environment.class.getField("CONNECTION_HANDLING");
				jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
			}
			catch (NoSuchFieldException ex) {
				// Try Hibernate 5.1
				try {
					Environment.class.getField("ACQUIRE_CONNECTIONS");
					jpaProperties.put("hibernate.connection.release_mode", "ON_CLOSE");
				}
				catch (NoSuchFieldException ex2) {
					// 在Hibernate 5.0.x或更低版本 - 无需更改默认值
				}
			}
		}

		return jpaProperties;
	}

	/**
	 * 确定给定目标数据库的Hibernate数据库方言类.
	 * 
	 * @param database 目标数据库
	 * 
	 * @return Hibernate数据库方言类, 或{@code null}
	 */
	@SuppressWarnings("deprecation")
	protected Class<?> determineDatabaseDialectClass(Database database) {
		switch (database) {
			case DB2: return DB2Dialect.class;
			case DERBY: return DerbyDialect.class;  // DerbyDialect deprecated in 4.x
			case H2: return H2Dialect.class;
			case HSQL: return HSQLDialect.class;
			case INFORMIX: return InformixDialect.class;
			case MYSQL: return MySQL5Dialect.class;
			case ORACLE: return Oracle9iDialect.class;
			case POSTGRESQL: return PostgreSQLDialect.class;  // PostgreSQLDialect deprecated in 4.x
			case SQL_SERVER: return SQLServer2008Dialect.class;
			case SYBASE: return org.hibernate.dialect.SybaseDialect.class;  // SybaseDialect deprecated in 3.6 but not 4.x
			default: return null;
		}
	}

	@Override
	public HibernateJpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	@Override
	public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
		return this.entityManagerFactoryInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

}
