package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.SingleDataSourceLookup;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean},
 * 根据JPA的标准<i>容器</i>bootstrap约定, 创建JPA {@link javax.persistence.EntityManagerFactory}.
 * 这是在Spring应用程序上下文中设置共享JPA EntityManagerFactory的最强大方法;
 * 然后可以通过依赖注入将EntityManagerFactory传递给基于JPA的DAO.
 * 请注意, 切换到JNDI查找或{@link LocalEntityManagerFactoryBean}定义只是配置问题!
 *
 * <p>与{@link LocalEntityManagerFactoryBean}一样, 配置通常从{@code META-INF/persistence.xml}配置文件中读取,
 * 根据一般JPA配置约定驻留在类路径中.
 * 但是这个FactoryBean更灵活, 因为可以覆盖{@code persistence.xml}文件的位置, 指定要链接的JDBC DataSources等.
 * 此外, 它允许通过Spring的{@link org.springframework.instrument.classloading.LoadTimeWeaver}抽象实现可插入的类检测,
 * 而不是绑定到JVM启动时指定的特殊VM代理.
 *
 * <p>在内部, 此FactoryBean解析{@code persistence.xml}文件本身,
 * 并创建相应的{@link javax.persistence.spi.PersistenceUnitInfo}对象 (进一步配置合并, 如JDBC DataSources 和Spring LoadTimeWeaver),
 * 传递到选定的JPA {@link javax.persistence.spi.PersistenceProvider}.
 * 这对应于本地JPA容器, 完全支持标准JPA容器约定.
 *
 * <p>公开的EntityManagerFactory对象将实现PersistenceProvider返回的底层本机EntityManagerFactory的所有接口,
 * 以及{@link EntityManagerFactoryInfo}接口, 该接口公开由此FactoryBean组装的其他元数据.
 *
 * <p><b>NOTE: 从Spring 4.0开始, Spring的JPA支持需要JPA 2.0或更高版本.</b>
 * 仍然支持基于JPA 1.0的应用程序; 但是, 在运行时需要一个兼容JPA 2.0/2.1的持久化提供者.
 * Spring的持久化单元引导通过检查类路径上的JPA API自动检测JPA 2.0与2.1.
 */
@SuppressWarnings("serial")
public class LocalContainerEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean
		implements ResourceLoaderAware, LoadTimeWeaverAware {

	private PersistenceUnitManager persistenceUnitManager;

	private final DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

	private PersistenceUnitInfo persistenceUnitInfo;


	/**
	 * 设置PersistenceUnitManager, 用于获取此FactoryBean应构建EntityManagerFactory的JPA持久化单元.
	 * <p>默认设置是依赖于此FactoryBean上指定的本地设置, 例如"persistenceXmlLocation", "dataSource" 和 "loadTimeWeaver".
	 * <p>要重用现有的持久化单元配置或更高级的自定义持久化单元处理形式, 请考虑定义一个单独的PersistenceUnitManager bean
	 * (通常是DefaultPersistenceUnitManager实例)并在此处链接它.
	 * {@code persistence.xml}位置, DataSource配置和LoadTimeWeaver将在这种情况下,
	 * 在单独的DefaultPersistenceUnitManager bean上定义.
	 */
	public void setPersistenceUnitManager(PersistenceUnitManager persistenceUnitManager) {
		this.persistenceUnitManager = persistenceUnitManager;
	}

	/**
	 * 设置要使用的{@code persistence.xml}文件的位置. 这是一个Spring资源位置.
	 * <p>默认"classpath:META-INF/persistence.xml".
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 * 
	 * @param persistenceXmlLocation 一个Spring资源字符串,
	 * 用于标识此LocalContainerEntityManagerFactoryBean应解析的{@code persistence.xml}文件的位置
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.internalPersistenceUnitManager.setPersistenceXmlLocation(persistenceXmlLocation);
	}

	/**
	 * 如果适用, 使用指定的持久化单元名称作为默认持久化单元的名称.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	@Override
	public void setPersistenceUnitName(String persistenceUnitName) {
		super.setPersistenceUnitName(persistenceUnitName);
		this.internalPersistenceUnitManager.setDefaultPersistenceUnitName(persistenceUnitName);
	}

	/**
	 * 设置默认持久化单元的持久化单元根位置.
	 * <p>默认 "classpath:", 也就是当前类路径的根 (最近的根目录).
	 * 如果特定于单元的解析不起作用, 且类路径根也不合适, 则被覆盖.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.internalPersistenceUnitManager.setDefaultPersistenceUnitRootLocation(defaultPersistenceUnitRootLocation);
	}

	/**
	 * 设置是否对类路径中的实体类使用基于Spring的扫描,
	 * 而不是使用JPA对包含{@code persistence.xml}标记的jar文件的标准扫描.
	 * 在基于Spring的扫描的情况下, 不需要{@code persistence.xml}; 需要做的就是在此处指定要搜索的基础包.
	 * <p>默认无. 指定在类路径中搜索实体类的自动检测的包.
	 * 这类似于Spring的组件扫描功能
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p><p>Note: 与常规JPA扫描相比, 可能存在限制.</b>
	 * 特别是, JPA提供者可以仅在由{@code persistence.xml}驱动时, 为特定于提供者的注解选取带注解的包.
	 * 从4.1开始, 如果给定的{@link JpaVendorAdapter}支持, Spring的扫描也可以检测带注解的包 (e.g. 用于Hibernate).
	 * <p>如果除了这些包之外没有指定明确的{@link #setMappingResources 映射资源},
	 * Spring的设置会在类路径中查找默认的{@code META-INF/orm.xml}文件,
	 * 如果映射文件与{@code persistence.xml}文件不在同一位置, 则将其注册为默认单元的映射资源
	 * (在这种情况下, 假设它仅用于与那里定义的持久化单元一起使用, 就像在标准JPA中一样).
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 * 
	 * @param packagesToScan 一个或多个要搜索的基础包, 类似于Spring的常规Spring组件的组件扫描配置
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.internalPersistenceUnitManager.setPackagesToScan(packagesToScan);
	}

	/**
	 * 为默认持久化单元指定一个或多个映射资源 (相当于{@code persistence.xml}中的{@code <mapping-file>}条目).
	 * 可以单独使用, 也可以与类路径中的实体扫描结合使用, 避免{@code persistence.xml}.
	 * <p>请注意, 映射资源必须相对于类路径根, e.g. "META-INF/mappings.xml" 或 "com/mycompany/repository/mappings.xml",
	 * 以便可以通过{@code ClassLoader.getResource}加载它们.
	 * <p>如果在{@link #setPackagesToScan 要扫描的包}旁边没有指定显式映射资源,
	 * 则Spring的设置会在类路径中查找默认的{@code META-INF/orm.xml}文件,
	 * 如果映射文件与{@code persistence.xml}文件不在同一位置, 则将其注册为默认单元的映射资源
	 * (在这种情况下, 假设它仅用于与那里定义的持久化单元一起使用, 就像在标准JPA中一样).
	 * <p>请注意, 在此处指定空数组/列表会禁止默认的{@code META-INF/orm.xml}检查.
	 * 另一方面, 在这里明确指定{@code META-INF/orm.xml}将注册该文件, 即使它恰好与{@code persistence.xml}文件位于同一位置.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setMappingResources(String... mappingResources) {
		this.internalPersistenceUnitManager.setMappingResources(mappingResources);
	}

	/**
	 * 为此持久化单元指定JPA 2.0共享缓存模式, 如果设置, 则覆盖{@code persistence.xml}中的值.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.internalPersistenceUnitManager.setSharedCacheMode(sharedCacheMode);
	}

	/**
	 * 为此持久化单元指定JPA 2.0验证模式, 如果设置, 则覆盖{@code persistence.xml}中的值.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setValidationMode(ValidationMode validationMode) {
		this.internalPersistenceUnitManager.setValidationMode(validationMode);
	}

	/**
	 * 指定JPA持久化提供者应该用于访问数据库的JDBC DataSource.
	 * 这是将JDBC配置保留在{@code persistence.xml}中的替代方法, 传入Spring管理的DataSource.
	 * <p>在JPA中, 这里传入的DataSource将用作传递给PersistenceProvider的PersistenceUnitInfo上的"nonJtaDataSource",
	 * 以及覆盖{@code persistence.xml}中的数据源配置.
	 * 请注意, 此变体通常也适用于JTA事务管理; 如果不适用, 考虑使用显式{@link #setJtaDataSource}.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setDataSource(DataSource dataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(dataSource));
		this.internalPersistenceUnitManager.setDefaultDataSource(dataSource);
	}

	/**
	 * 指定JPA持久化提供者应该用于访问数据库的JDBC DataSource.
	 * 这是将JDBC配置保留在{@code persistence.xml}中的替代方法, 传入Spring管理的DataSource.
	 * <p>在JPA中, 这里传入的DataSource将用作传递给PersistenceProvider的PersistenceUnitInfo上的"jtaDataSource",
	 * 以及覆盖{@code persistence.xml}中的数据源配置.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setJtaDataSource(DataSource jtaDataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(jtaDataSource));
		this.internalPersistenceUnitManager.setDefaultJtaDataSource(jtaDataSource);
	}

	/**
	 * 设置PersistenceUnitPostProcessor, 应用于创建此EntityManagerFactory的PersistenceUnitInfo.
	 * <p>除了从{@code persistence.xml}读取的元数据之外, 这样的后处理器还可以注册其他实体类和jar文件.
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
		this.internalPersistenceUnitManager.setPersistenceUnitPostProcessors(postProcessors);
	}

	/**
	 * 根据JPA类变换器约定, 指定用于类检测的Spring LoadTimeWeaver.
	 * <p>不需要指定LoadTimeWeaver:
	 * 大多数提供商都能够在没有类检测的情况下提供其功能的子集, 或者使用在JVM启动时指定的VM代理进行操作.
	 * <p>就Spring提供的织入选项而言, 最重要的是InstrumentationLoadTimeWeaver,
	 * 它需要在JVM启动时指定的Spring特定 (但非常通用) VM代理,
	 * 以及ReflectiveLoadTimeWeaver, 它基于可用的特定扩展方法与底层ClassLoader交互 (例如, 与Spring的TomcatInstrumentableClassLoader交互).
	 * <p><b>NOTE:</b> 从Spring 2.5开始, 将自动获取上下文的默认LoadTimeWeaver (定义为名为"loadTimeWeaver"的bean), 如果可用,
	 * 从而无需在每个受影响的目标bean上配置LoadTimeWeaver.</b>
	 * 考虑使用{@code context:load-time-weaver} XML标记来创建这样的共享LoadTimeWeaver (默认情况下自动检测环境).
	 * <p><b>NOTE: 仅在未指定外部PersistenceUnitManager时应用.</b>
	 * 否则, 外部{@link #setPersistenceUnitManager PersistenceUnitManager}负责织入配置.
	 */
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.internalPersistenceUnitManager.setLoadTimeWeaver(loadTimeWeaver);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.internalPersistenceUnitManager.setResourceLoader(resourceLoader);
	}


	@Override
	public void afterPropertiesSet() throws PersistenceException {
		PersistenceUnitManager managerToUse = this.persistenceUnitManager;
		if (this.persistenceUnitManager == null) {
			this.internalPersistenceUnitManager.afterPropertiesSet();
			managerToUse = this.internalPersistenceUnitManager;
		}

		this.persistenceUnitInfo = determinePersistenceUnitInfo(managerToUse);
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null && this.persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
			((SmartPersistenceUnitInfo) this.persistenceUnitInfo).setPersistenceProviderPackageName(
					jpaVendorAdapter.getPersistenceProviderRootPackage());
		}

		super.afterPropertiesSet();
	}

	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		Assert.state(this.persistenceUnitInfo != null, "PersistenceUnitInfo not initialized");

		PersistenceProvider provider = getPersistenceProvider();
		if (provider == null) {
			String providerClassName = this.persistenceUnitInfo.getPersistenceProviderClassName();
			if (providerClassName == null) {
				throw new IllegalArgumentException(
						"No PersistenceProvider specified in EntityManagerFactory configuration, " +
						"and chosen PersistenceUnitInfo does not specify a provider class name either");
			}
			Class<?> providerClass = ClassUtils.resolveClassName(providerClassName, getBeanClassLoader());
			provider = (PersistenceProvider) BeanUtils.instantiateClass(providerClass);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Building JPA container EntityManagerFactory for persistence unit '" +
					this.persistenceUnitInfo.getPersistenceUnitName() + "'");
		}
		EntityManagerFactory emf =
				provider.createContainerEntityManagerFactory(this.persistenceUnitInfo, getJpaPropertyMap());
		postProcessEntityManagerFactory(emf, this.persistenceUnitInfo);

		return emf;
	}


	/**
	 * 确定要用于由此bean创建的EntityManagerFactory的PersistenceUnitInfo.
	 * <p>默认实现从{@code persistence.xml}中读取所有持久化单元信息, 如JPA规范中所定义.
	 * 如果未指定实体管理器名称, 则它将获取读取器返回的数组中的第一个信息. 否则, 它会检查匹配的名称.
	 * 
	 * @param persistenceUnitManager 从中获取的PersistenceUnitManager
	 * 
	 * @return 选择的PersistenceUnitInfo
	 */
	protected PersistenceUnitInfo determinePersistenceUnitInfo(PersistenceUnitManager persistenceUnitManager) {
		if (getPersistenceUnitName() != null) {
			return persistenceUnitManager.obtainPersistenceUnitInfo(getPersistenceUnitName());
		}
		else {
			return persistenceUnitManager.obtainDefaultPersistenceUnitInfo();
		}
	}

	/**
	 * Hook方法, 允许子类在通过PersistenceProvider创建后自定义EntityManagerFactory.
	 * <p>默认实现为空.
	 * 
	 * @param emf 正在使用的新创建的EntityManagerFactory
	 * @param pui 用于配置EntityManagerFactory的PersistenceUnitInfo
	 */
	protected void postProcessEntityManagerFactory(EntityManagerFactory emf, PersistenceUnitInfo pui) {
	}


	@Override
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return this.persistenceUnitInfo;
	}

	@Override
	public String getPersistenceUnitName() {
		if (this.persistenceUnitInfo != null) {
			return this.persistenceUnitInfo.getPersistenceUnitName();
		}
		return super.getPersistenceUnitName();
	}

	@Override
	public DataSource getDataSource() {
		if (this.persistenceUnitInfo != null) {
			return (this.persistenceUnitInfo.getJtaDataSource() != null ?
					this.persistenceUnitInfo.getJtaDataSource() :
					this.persistenceUnitInfo.getNonJtaDataSource());
		}
		return (this.internalPersistenceUnitManager.getDefaultJtaDataSource() != null ?
				this.internalPersistenceUnitManager.getDefaultJtaDataSource() :
				this.internalPersistenceUnitManager.getDefaultDataSource());
	}

}
