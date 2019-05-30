package org.springframework.orm.jpa.persistenceunit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link PersistenceUnitManager}接口的默认实现.
 * 由{{@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}用作内部默认值.
 *
 * <p>支持{@code persistence.xml}文件的标准JPA扫描, 具有可配置的文件位置, JDBC DataSource 查找和加载时织入.
 *
 * <p>默认的XML文件位置是{@code classpath*:META-INF/persistence.xml}, 扫描类路径中的所有匹配文件 (如JPA规范中所定义).
 * 默认情况下, DataSource名称被解释为JNDI名称, 并且没有可用的加载时织入 (这需要在持久化提供者中关闭织入).
 *
 * <p><b>NOTE: 从Spring 4.0开始, Spring的JPA支持需要JPA 2.0或更高版本.</b>
 * Spring的持久化单元引导在运行时自动检测JPA 2.1.
 */
public class DefaultPersistenceUnitManager
		implements PersistenceUnitManager, ResourceLoaderAware, LoadTimeWeaverAware, InitializingBean {

	private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final String DEFAULT_ORM_XML_RESOURCE = "META-INF/orm.xml";

	private static final String PERSISTENCE_XML_FILENAME = "persistence.xml";

	/**
	 * {@code persistence.xml}文件的默认位置:
	 * "classpath*:META-INF/persistence.xml".
	 */
	public static final String DEFAULT_PERSISTENCE_XML_LOCATION = "classpath*:META-INF/" + PERSISTENCE_XML_FILENAME;

	/**
	 * 持久化单元根URL的默认位置:
	 * "classpath:", 表示类路径的根.
	 */
	public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";

	public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME = "default";


	private static final Set<TypeFilter> entityTypeFilters;

	static {
		entityTypeFilters = new LinkedHashSet<TypeFilter>(4);
		entityTypeFilters.add(new AnnotationTypeFilter(Entity.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Embeddable.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(MappedSuperclass.class, false));
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> converterAnnotation = (Class<? extends Annotation>)
					ClassUtils.forName("javax.persistence.Converter", DefaultPersistenceUnitManager.class.getClassLoader());
			entityTypeFilters.add(new AnnotationTypeFilter(converterAnnotation, false));
		}
		catch (ClassNotFoundException ex) {
			// JPA 2.1 API not available
		}
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private String[] persistenceXmlLocations = new String[] {DEFAULT_PERSISTENCE_XML_LOCATION};

	private String defaultPersistenceUnitRootLocation = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION;

	private String defaultPersistenceUnitName = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME;

	private String[] packagesToScan;

	private String[] mappingResources;

	private SharedCacheMode sharedCacheMode;

	private ValidationMode validationMode;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	private DataSource defaultDataSource;

	private DataSource defaultJtaDataSource;

	private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

	private LoadTimeWeaver loadTimeWeaver;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final Set<String> persistenceUnitInfoNames = new HashSet<String>();

	private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<String, PersistenceUnitInfo>();


	/**
	 * 指定要加载的{@code persistence.xml}文件的位置.
	 * 这些可以指定为Spring资源位置和/或位置模式.
	 * <p>默认"classpath*:META-INF/persistence.xml".
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.persistenceXmlLocations = new String[] {persistenceXmlLocation};
	}

	/**
	 * 指定要加载的{@code persistence.xml}文件的多个位置.
	 * 这些可以指定为Spring资源位置和/或位置模式.
	 * <p>默认"classpath*:META-INF/persistence.xml".
	 * 
	 * @param persistenceXmlLocations 一个Spring资源字符串数组, 用于标识要读取的{@code persistence.xml}文件的位置
	 */
	public void setPersistenceXmlLocations(String... persistenceXmlLocations) {
		this.persistenceXmlLocations = persistenceXmlLocations;
	}

	/**
	 * 设置默认持久化单元根位置, 如果无法确定单元特定的持久化单元根, 则应用该位置.
	 * <p>默认"classpath:", 当前类路径的根 (最近的根目录).
	 * 如果特定于单元的解析不起作用且类路径根也不合适, 则被覆盖.
	 */
	public void setDefaultPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.defaultPersistenceUnitRootLocation = defaultPersistenceUnitRootLocation;
	}

	/**
	 * 指定默认持久化单元的名称. 默认"default".
	 * <p>主要应用于没有{@code persistence.xml}的扫描持久化单元.
	 * 也适用于从几个可用的持久化单元中选择默认单元.
	 */
	public void setDefaultPersistenceUnitName(String defaultPersistenceUnitName) {
		this.defaultPersistenceUnitName = defaultPersistenceUnitName;
	}

	/**
	 * 设置是否对类路径中的实体类使用基于Spring的扫描, 而不是使用JPA对包含{@code persistence.xml}标记的jar文件的标准扫描.
	 * 在基于Spring的扫描的情况下, 不需要{@code persistence.xml}; 在此处指定要搜索的基础包.
	 * <p>默认无. 指定包以在类路径中搜索实体类的自动检测.
	 * 这类似于Spring的组件扫描功能
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p>这样的包扫描在Spring中定义了一个"默认持久化单元", 它可能位于源自{@code persistence.xml}的常规定义单元旁边.
	 * 它的名称由{@link #setDefaultPersistenceUnitName}决定: 默认"default".
	 * <p><p>Note: 与常规JPA扫描相比, 可能存在限制.</b>
	 * 特别是, JPA提供者可以仅在由{@code persistence.xml}驱动时, 为特定于提供者的注解选取带注解的包.
	 * 从4.1开始, 如果给定{@link org.springframework.orm.jpa.JpaVendorAdapter}支持,
	 * Spring的扫描也可以检测带注解的包 (e.g. 用于Hibernate).
	 * <p>如果除了这些包之外没有指定显式的{@link #setMappingResources 映射资源},
	 * 则此管理器在类路径中查找默认的{@code META-INF/orm.xml}文件,
	 * 如果映射文件不与{@code persistence.xml}文件共存, 则将其注册为默认单元的映射资源
	 * (在这种情况下, 假设它仅用于与那里定义的持久化单元一起使用, 例如在标准JPA中).
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 为默认持久化单元指定一个或多个映射资源 (相当于{@code persistence.xml}中的{@code <mapping-file>}条目).
	 * 可以单独使用, 也可以与类路径中的实体扫描结合使用, 避免{@code persistence.xml}.
	 * <p>请注意, 映射资源必须相对于类路径根,
	 * e.g. "META-INF/mappings.xml"或"com/mycompany/repository/mappings.xml",
	 * 以便可以通过{@code ClassLoader.getResource}加载它们.
	 * <p>如果在{@link #setPackagesToScan 要扫描的包}旁边没有指定显式映射资源,
	 * 则此管理器在类路径中查找默认的{@code META-INF/orm.xml}文件,
	 * 如果映射文件不与{@code persistence.xml}文件共存, 则将其注册为默认单元的映射资源
	 * (在这种情况下, 假设它仅用于与那里定义的持久化单元一起使用, 例如在标准JPA中).
	 * <p>请注意, 在此处指定空数组/列表会禁止默认的{@code META-INF/orm.xml}检查.
	 * 另一方面, 在这里明确指定{@code META-INF/orm.xml}将注册该文件, 即使它恰好与{@code persistence.xml}文件位于同一位置.
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * 为所有此管理器的持久化单元指定JPA 2.0共享缓存模式, 如果设置则覆盖{@code persistence.xml}中的任何值.
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	/**
	 * 为所有此管理器的持久化单元指定JPA 2.0验证模式, 如果设置则覆盖{@code persistence.xml}中的任何值.
	 */
	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 指定JPA持久化提供者应该用于访问数据库的JDBC DataSource,
	 * 在{@code persistence.xml}中针对Spring管理的DataSources解析数据源名称.
	 * <p>指定的Map需要为特定的DataSource对象定义数据源名称, 与{@code persistence.xml}中使用的数据源名称相匹配.
	 * 如果未指定, 数据源名称将被解析为JNDI名称 (由标准JPA定义).
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSourceLookup = new MapDataSourceLookup(dataSources);
	}

	/**
	 * 指定为持久化提供者提供DataSource的JDBC DataSourceLookup,
	 * 在{@code persistence.xml}中针对Spring管理的DataSource实例解析数据源名称.
	 * <p>默认为JndiDataSourceLookup, 它将DataSource名称解析为JNDI名称 (由标准JPA定义).
	 * 如果希望针对Spring bean名称解析DataSource名称, 指定BeanFactoryDataSourceLookup实例.
	 * <p>或者, 考虑通过"dataSources"属性传入名称和DataSource实例的Map.
	 * 如果{@code persistence.xml}文件根本没有定义DataSource名称, 通过"defaultDataSource"属性指定默认数据源.
	 */
	public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}

	/**
	 * 返回为持久化提供者提供DataSource的JDBC DataSourceLookup,
	 * 在{@code persistence.xml}中针对Spring管理的DataSource实例解析数据源名称.
	 */
	public DataSourceLookup getDataSourceLookup() {
		return this.dataSourceLookup;
	}

	/**
	 * 如果在{@code persistence.xml}中未指定任何内容, 则指定JPA持久化提供者应该用于访问数据库的JDBC DataSource.
	 * 此变体表示没有特殊的事务设置, i.e. 典型的资源本地.
	 * <p>在JPA中, 这里传入的DataSource将用作传递给PersistenceProvider的PersistenceUnitInfo的"nonJtaDataSource",
	 * 前提是之前没有注册过.
	 */
	public void setDefaultDataSource(DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	/**
	 * 如果在{@code persistence.xml}中没有指定, 则返回JPA持久化提供者用于访问数据库的JDBC DataSource.
	 */
	public DataSource getDefaultDataSource() {
		return this.defaultDataSource;
	}

	/**
	 * 如果在{@code persistence.xml}中未指定任何内容, 则指定JPA持久化提供者应该用于访问数据库的JDBC DataSource.
	 * 此变体表示JTA应该用作事务类型.
	 * <p>在JPA中, 这里传入的DataSource将用作传递给PersistenceProvider的PersistenceUnitInfo的"jtaDataSource",
	 * 前提是之前没有注册过.
	 */
	public void setDefaultJtaDataSource(DataSource defaultJtaDataSource) {
		this.defaultJtaDataSource = defaultJtaDataSource;
	}

	/**
	 * 如果在{@code persistence.xml}中未指定任何内容, 则返回JPA持久化提供者应该用于访问数据库的JDBC DataSource.
	 */
	public DataSource getDefaultJtaDataSource() {
		return this.defaultJtaDataSource;
	}

	/**
	 * 设置PersistenceUnitPostProcessor, 应用于此管理器已解析的每个PersistenceUnitInfo.
	 * <p>除了从{@code persistence.xml}读取的元数据之外, 这样的后处理器还可以注册其他实体类和jar文件.
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
		this.persistenceUnitPostProcessors = postProcessors;
	}

	/**
	 * 返回PersistenceUnitPostProcessor, 应用于此管理器已解析的每个PersistenceUnitInfo.
	 */
	public PersistenceUnitPostProcessor[] getPersistenceUnitPostProcessors() {
		return this.persistenceUnitPostProcessors;
	}

	/**
	 * 根据JPA类变换器约定, 指定用于类检测的Spring LoadTimeWeaver.
	 * <p>不需要指定LoadTimeWeaver:
	 * 大多数提供商都能够在没有类检测的情况下提供其功能的子集, 或者使用在JVM启动时指定的自己的VM代理进行操作.
	 * 此外, 如果Spring的基于代理的检测在运行时可用, DefaultPersistenceUnitManager将回退到InstrumentationLoadTimeWeaver.
	 * <p>就Spring提供的织入选项而言, 最重要的是InstrumentationLoadTimeWeaver,
	 * 它需要在JVM启动时指定Spring特定的 (但非常通用)VM代理,
	 * 以及ReflectiveLoadTimeWeaver, 它基于可用的特定扩展方法与底层ClassLoader交互
	 * (例如, 与Spring的TomcatInstrumentableClassLoader交互).
	 * 考虑使用{@code context:load-time-weaver} XML标记来创建这样的共享LoadTimeWeaver (默认情况下自动检测环境).
	 */
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * 根据JPA类变换器约定, 返回用于类检测的Spring LoadTimeWeaver.
	 */
	public LoadTimeWeaver getLoadTimeWeaver() {
		return this.loadTimeWeaver;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	@Override
	public void afterPropertiesSet() {
		if (this.loadTimeWeaver == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(this.resourcePatternResolver.getClassLoader());
		}
		preparePersistenceUnitInfos();
	}

	/**
	 * 根据此管理器的配置准备PersistenceUnitInfo:
	 * 扫描{@code persistence.xml}文件, 解析所有匹配的文件, 配置和后处理.
	 * <p>在调用此准备方法之前, 无法获取PersistenceUnitInfo.
	 */
	public void preparePersistenceUnitInfos() {
		this.persistenceUnitInfoNames.clear();
		this.persistenceUnitInfos.clear();

		List<SpringPersistenceUnitInfo> puis = readPersistenceUnitInfos();
		for (SpringPersistenceUnitInfo pui : puis) {
			if (pui.getPersistenceUnitRootUrl() == null) {
				pui.setPersistenceUnitRootUrl(determineDefaultPersistenceUnitRootUrl());
			}
			if (pui.getJtaDataSource() == null) {
				pui.setJtaDataSource(this.defaultJtaDataSource);
			}
			if (pui.getNonJtaDataSource() == null) {
				pui.setNonJtaDataSource(this.defaultDataSource);
			}
			if (this.sharedCacheMode != null) {
				pui.setSharedCacheMode(this.sharedCacheMode);
			}
			if (this.validationMode != null) {
				pui.setValidationMode(this.validationMode);
			}
			if (this.loadTimeWeaver != null) {
				pui.init(this.loadTimeWeaver);
			}
			else {
				pui.init(this.resourcePatternResolver.getClassLoader());
			}
			postProcessPersistenceUnitInfo(pui);
			String name = pui.getPersistenceUnitName();
			if (!this.persistenceUnitInfoNames.add(name) && !isPersistenceUnitOverrideAllowed()) {
				StringBuilder msg = new StringBuilder();
				msg.append("Conflicting persistence unit definitions for name '").append(name).append("': ");
				msg.append(pui.getPersistenceUnitRootUrl()).append(", ");
				msg.append(this.persistenceUnitInfos.get(name).getPersistenceUnitRootUrl());
				throw new IllegalStateException(msg.toString());
			}
			this.persistenceUnitInfos.put(name, pui);
		}
	}

	/**
	 * 按照JPA规范中的定义, 从{@code persistence.xml}读取所有持久化单元信息.
	 */
	private List<SpringPersistenceUnitInfo> readPersistenceUnitInfos() {
		List<SpringPersistenceUnitInfo> infos = new LinkedList<SpringPersistenceUnitInfo>();
		String defaultName = this.defaultPersistenceUnitName;
		boolean buildDefaultUnit = (this.packagesToScan != null || this.mappingResources != null);
		boolean foundDefaultUnit = false;

		PersistenceUnitReader reader = new PersistenceUnitReader(this.resourcePatternResolver, this.dataSourceLookup);
		SpringPersistenceUnitInfo[] readInfos = reader.readPersistenceUnitInfos(this.persistenceXmlLocations);
		for (SpringPersistenceUnitInfo readInfo : readInfos) {
			infos.add(readInfo);
			if (defaultName != null && defaultName.equals(readInfo.getPersistenceUnitName())) {
				foundDefaultUnit = true;
			}
		}

		if (buildDefaultUnit) {
			if (foundDefaultUnit) {
				if (logger.isInfoEnabled()) {
					logger.info("Found explicit default unit with name '" + defaultName + "' in persistence.xml - " +
							"overriding local default unit settings ('packagesToScan'/'mappingResources')");
				}
			}
			else {
				infos.add(buildDefaultPersistenceUnitInfo());
			}
		}
		return infos;
	}

	/**
	 * 对实体类执行基于Spring的扫描.
	 */
	private SpringPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
		SpringPersistenceUnitInfo scannedUnit = new SpringPersistenceUnitInfo();
		scannedUnit.setPersistenceUnitName(this.defaultPersistenceUnitName);
		scannedUnit.setExcludeUnlistedClasses(true);

		if (this.packagesToScan != null) {
			for (String pkg : this.packagesToScan) {
				try {
					String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
							ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
					Resource[] resources = this.resourcePatternResolver.getResources(pattern);
					MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
					for (Resource resource : resources) {
						if (resource.isReadable()) {
							MetadataReader reader = readerFactory.getMetadataReader(resource);
							String className = reader.getClassMetadata().getClassName();
							if (matchesFilter(reader, readerFactory)) {
								scannedUnit.addManagedClassName(className);
								if (scannedUnit.getPersistenceUnitRootUrl() == null) {
									URL url = resource.getURL();
									if (ResourceUtils.isJarURL(url)) {
										scannedUnit.setPersistenceUnitRootUrl(ResourceUtils.extractJarFileURL(url));
									}
								}
							}
							else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
								scannedUnit.addManagedPackage(
										className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
							}
						}
					}
				}
				catch (IOException ex) {
					throw new PersistenceException("Failed to scan classpath for unlisted entity classes", ex);
				}
			}
		}

		if (this.mappingResources != null) {
			for (String mappingFileName : this.mappingResources) {
				scannedUnit.addMappingFileName(mappingFileName);
			}
		}
		else {
			Resource ormXml = getOrmXmlForDefaultPersistenceUnit();
			if (ormXml != null) {
				scannedUnit.addMappingFileName(DEFAULT_ORM_XML_RESOURCE);
				if (scannedUnit.getPersistenceUnitRootUrl() == null) {
					try {
						scannedUnit.setPersistenceUnitRootUrl(
								PersistenceUnitReader.determinePersistenceUnitRootUrl(ormXml));
					}
					catch (IOException ex) {
						logger.debug("Failed to determine persistence unit root URL from orm.xml location", ex);
					}
				}
			}
		}

		return scannedUnit;
	}

	/**
	 * 检查任何已配置的实体类型过滤器是否与元数据读取器中包含的当前类描述符匹配.
	 */
	private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		for (TypeFilter filter : entityTypeFilters) {
			if (filter.match(reader, readerFactory)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 尝试根据给定的"defaultPersistenceUnitRootLocation"确定持久化单元根URL.
	 * 
	 * @return 传递给JPA PersistenceProvider的持久化单元根URL
	 */
	private URL determineDefaultPersistenceUnitRootUrl() {
		if (this.defaultPersistenceUnitRootLocation == null) {
			return null;
		}
		try {
			URL url = this.resourcePatternResolver.getResource(this.defaultPersistenceUnitRootLocation).getURL();
			return (ResourceUtils.isJarURL(url) ? ResourceUtils.extractJarFileURL(url) : url);
		}
		catch (IOException ex) {
			throw new PersistenceException("Unable to resolve persistence unit root URL", ex);
		}
	}

	/**
	 * 确定JPA的默认"META-INF/orm.xml"资源, 以便与Spring的默认持久化单元一起使用.
	 * <p>检查类路径中是否存在"META-INF/orm.xml"文件, 如果它与"META-INF/persistence.xml"文件不在同一位置, 则使用它.
	 */
	private Resource getOrmXmlForDefaultPersistenceUnit() {
		Resource ormXml = this.resourcePatternResolver.getResource(
				this.defaultPersistenceUnitRootLocation + DEFAULT_ORM_XML_RESOURCE);
		if (ormXml.exists()) {
			try {
				Resource persistenceXml = ormXml.createRelative(PERSISTENCE_XML_FILENAME);
				if (!persistenceXml.exists()) {
					return ormXml;
				}
			}
			catch (IOException ex) {
				// 无法解析相对的persistence.xml文件 - 假设它不在那里.
				return ormXml;
			}
		}
		return null;
	}


	/**
	 * 从此管理器处理的持久化单元的缓存中返回指定的PersistenceUnitInfo, 将其保留在缓存中
	 * (i.e. 不'获取'它以供使用, 而只是访问它以进行后处理).
	 * <p>这可以在{@link #postProcessPersistenceUnitInfo}实现中使用,
	 * 检测具有相同名称的现有持久化单元并可能将它们合并.
	 * 
	 * @param persistenceUnitName 所需的持久化单元的名称
	 * 
	 * @return 可变形式的PersistenceUnitInfo, 或 {@code null}
	 */
	protected final MutablePersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.get(persistenceUnitName);
		return (MutablePersistenceUnitInfo) pui;
	}

	/**
	 * Hook方法, 允许子类自定义每个PersistenceUnitInfo.
	 * <p>默认实现委托给所有已注册的PersistenceUnitPostProcessor.
	 * 通常最好在那里注册更多的实体类, jar文件等, 而不是在这个管理器的子类中, 以便能够重用后处理器.
	 * 
	 * @param pui 从{@code persistence.xml}中读取的所选PersistenceUnitInfo.
	 */
	protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
		PersistenceUnitPostProcessor[] postProcessors = getPersistenceUnitPostProcessors();
		if (postProcessors != null) {
			for (PersistenceUnitPostProcessor postProcessor : postProcessors) {
				postProcessor.postProcessPersistenceUnitInfo(pui);
			}
		}
	}

	/**
	 * 返回是否允许覆盖同名的持久化单元.
	 * <p>默认 {@code false}.
	 * 可以重写以返回{@code true}, 例如如果{@link #postProcessPersistenceUnitInfo}能够处理该情况.
	 */
	protected boolean isPersistenceUnitOverrideAllowed() {
		return false;
	}


	@Override
	public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() {
		if (this.persistenceUnitInfoNames.isEmpty()) {
			throw new IllegalStateException("No persistence units parsed from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations));
		}
		if (this.persistenceUnitInfos.isEmpty()) {
			throw new IllegalStateException("All persistence units from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations) + " already obtained");
		}
		if (this.persistenceUnitInfos.size() > 1) {
			return obtainPersistenceUnitInfo(this.defaultPersistenceUnitName);
		}
		PersistenceUnitInfo pui = this.persistenceUnitInfos.values().iterator().next();
		this.persistenceUnitInfos.clear();
		return pui;
	}

	@Override
	public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.remove(persistenceUnitName);
		if (pui == null) {
			if (!this.persistenceUnitInfoNames.contains(persistenceUnitName)) {
				throw new IllegalArgumentException(
						"No persistence unit with name '" + persistenceUnitName + "' found");
			}
			else {
				throw new IllegalStateException(
						"Persistence unit with name '" + persistenceUnitName + "' already obtained");
			}
		}
		return pui;
	}
}
