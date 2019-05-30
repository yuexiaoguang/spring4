package org.springframework.orm.hibernate4;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.AttributeConverter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.service.ServiceRegistry;

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
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring提供的标准Hibernate {@link Configuration}类的扩展,
 * 添加{@link SpringSessionContext}作为默认值, 并提供指定DataSource和应用程序类加载器的便捷方法.
 *
 * <p>这是专为程序化使用而设计的, e.g. 在{@code @Bean}工厂方法中.
 * 考虑将{@link LocalSessionFactoryBean}用于XML bean定义文件.
 *
 * <p><b>需要Hibernate 4.0或更高版本.</b> 从Spring 4.0开始, 它与(非常重构的)Hibernate 4.3兼容.
 * 建议使用最新的Hibernate 4.2.x或4.3.x版本,
 * 具体取决于是否需要在运行时保持JPA 2.0兼容 (Hibernate 4.2) 或升级到JPA 2.1 (Hibernate 4.3).
 *
 * <p><b>NOTE:</b> 要为Spring驱动的JTA事务设置Hibernate 4, 请确保使用{@link #setJtaTransactionManager}方法
 * 或将"hibernate.transaction.factory_class"方法或将{@link CMTTransactionFactory}.
 * 否则, Hibernate的智能刷新机制将无法正常工作.
 */
@SuppressWarnings("serial")
public class LocalSessionFactoryBuilder extends Configuration {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false)};


	private static TypeFilter converterTypeFilter;

	static {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> converterAnnotation = (Class<? extends Annotation>)
					ClassUtils.forName("javax.persistence.Converter", LocalSessionFactoryBuilder.class.getClassLoader());
			converterTypeFilter = new AnnotationTypeFilter(converterAnnotation, false);
		}
		catch (ClassNotFoundException ex) {
			// JPA 2.1 API not available - Hibernate <4.3
		}
	}


	private final ResourcePatternResolver resourcePatternResolver;

	private RegionFactory cacheRegionFactory;

	private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;


	/**
	 * @param dataSource 生成的Hibernate SessionFactory应该使用的JDBC DataSource (may be {@code null})
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource) {
		this(dataSource, new PathMatchingResourcePatternResolver());
	}

	/**
	 * @param dataSource 生成的Hibernate SessionFactory应该使用的JDBC DataSource (may be {@code null})
	 * @param classLoader 从中加载应用程序类的ClassLoader
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource, ClassLoader classLoader) {
		this(dataSource, new PathMatchingResourcePatternResolver(classLoader));
	}

	/**
	 * @param dataSource 生成的Hibernate SessionFactory应该使用的JDBC DataSource (may be {@code null})
	 * @param resourceLoader 从中加载应用程序类的ResourceLoader
	 */
	@SuppressWarnings("deprecation")  // 能够针对Hibernate 4.3构建
	public LocalSessionFactoryBuilder(DataSource dataSource, ResourceLoader resourceLoader) {
		getProperties().put(Environment.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		if (dataSource != null) {
			getProperties().put(Environment.DATASOURCE, dataSource);
		}
		// 自Hibernate 4.3起, APP_CLASSLOADER已弃用, 但我们需要保持与4.0+兼容
		getProperties().put(AvailableSettings.APP_CLASSLOADER, resourceLoader.getClassLoader());
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * 设置与Hibernate一起使用的Spring {@link JtaTransactionManager}或JTA {@link TransactionManager}.
	 * 允许使用Spring管理的事务管理器用于Hibernate 4的会话和缓存同步, 并自动设置"hibernate.transaction.jta.platform".
	 * 还将"hibernate.transaction.factory_class"设置为{@link CMTTransactionFactory}, 指示Hibernate与外部管理的事务进行交互.
	 * <p>传入的Spring {@link JtaTransactionManager}需要包含一个可在此处使用的JTA {@link TransactionManager}引用,
	 * 但WebSphere情况除外, 将相应地自动设置{@code WebSphereExtendedJtaPlatform}.
	 * <p>Note: 如果设置了此项, 则Hibernate设置不应包含JTA平台设置, 以避免无意义的双重配置.
	 */
	public LocalSessionFactoryBuilder setJtaTransactionManager(Object jtaTransactionManager) {
		Assert.notNull(jtaTransactionManager, "Transaction manager reference must not be null");
		if (jtaTransactionManager instanceof JtaTransactionManager) {
			boolean webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", getClass().getClassLoader());
			if (webspherePresent) {
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						ConfigurableJtaPlatform.getJtaPlatformBasePackage() + "internal.WebSphereExtendedJtaPlatform");
			}
			else {
				JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
				if (jtaTm.getTransactionManager() == null) {
					throw new IllegalArgumentException(
							"Can only apply JtaTransactionManager which has a TransactionManager reference set");
				}
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
								jtaTm.getTransactionSynchronizationRegistry()).getJtaPlatformProxy());
			}
		}
		else if (jtaTransactionManager instanceof TransactionManager) {
			getProperties().put(AvailableSettings.JTA_PLATFORM,
					new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null).getJtaPlatformProxy());
		}
		else {
			throw new IllegalArgumentException(
					"Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
		}
		getProperties().put(AvailableSettings.TRANSACTION_STRATEGY, new CMTTransactionFactory());
		return this;
	}

	/**
	 * 将Hibernate 4.1/4.2/4.3 {@code MultiTenantConnectionProvider}设置为传递给SessionFactory: 作为实例, Class或String类名.
	 * <p>请注意, {@code MultiTenantConnectionProvider}接口的包位置在Hibernate 4.2和4.3之间发生了变化. 此方法接受两种变体.
	 */
	public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(Object multiTenantConnectionProvider) {
		getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
		return this;
	}

	/**
	 * 将Hibernate 4.1/4.2/4.3 {@code CurrentTenantIdentifierResolver}设置为传递给SessionFactory: 作为实例, Class或String类名.
	 */
	public LocalSessionFactoryBuilder setCurrentTenantIdentifierResolver(Object currentTenantIdentifierResolver) {
		getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		return this;
	}

	/**
	 * 设置Hibernate RegionFactory用于SessionFactory.
	 * 允许使用Spring管理的RegionFactory实例.
	 * <p>Note: 如果设置了此项, 则Hibernate设置不应定义缓存提供者, 以避免无意义的双重配置.
	 */
	public LocalSessionFactoryBuilder setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
		return this;
	}

	/**
	 * 为实体类指定基于Spring扫描的自定义类型过滤器.
	 * <p>默认是搜索所有指定的包, 查找带有{@code @javax.persistence.Entity},
	 * {@code @javax.persistence.Embeddable}或 {@code @javax.persistence.MappedSuperclass}注解的类.
	 */
	public LocalSessionFactoryBuilder setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
		return this;
	}

	/**
	 * 批量添加给定的带注解的类.
	 */
	public LocalSessionFactoryBuilder addAnnotatedClasses(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			addAnnotatedClass(annotatedClass);
		}
		return this;
	}

	/**
	 * 批量添加给定的带注解的包.
	 */
	public LocalSessionFactoryBuilder addPackages(String... annotatedPackages) {
		for (String annotatedPackage : annotatedPackages) {
			addPackage(annotatedPackage);
		}
		return this;
	}

	/**
	 * 对实体类执行基于Spring的扫描, 并使用此{@code Configuration}将它们注册为带注解的类.
	 * 
	 * @param packagesToScan 一个或多个Java包名称
	 * 
	 * @throws HibernateException 如果扫描失败
	 */
	public LocalSessionFactoryBuilder scanPackages(String... packagesToScan) throws HibernateException {
		Set<String> entityClassNames = new TreeSet<String>();
		Set<String> converterClassNames = new TreeSet<String>();
		Set<String> packageNames = new TreeSet<String>();
		try {
			for (String pkg : packagesToScan) {
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
				Resource[] resources = this.resourcePatternResolver.getResources(pattern);
				MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
				for (Resource resource : resources) {
					if (resource.isReadable()) {
						MetadataReader reader = readerFactory.getMetadataReader(resource);
						String className = reader.getClassMetadata().getClassName();
						if (matchesEntityTypeFilter(reader, readerFactory)) {
							entityClassNames.add(className);
						}
						else if (converterTypeFilter != null && converterTypeFilter.match(reader, readerFactory)) {
							converterClassNames.add(className);
						}
						else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
							packageNames.add(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
						}
					}
				}
			}
		}
		catch (IOException ex) {
			throw new MappingException("Failed to scan classpath for unlisted classes", ex);
		}
		try {
			ClassLoader cl = this.resourcePatternResolver.getClassLoader();
			for (String className : entityClassNames) {
				addAnnotatedClass(cl.loadClass(className));
			}
			for (String className : converterClassNames) {
				ConverterRegistrationDelegate.registerConverter(this, cl.loadClass(className));
			}
			for (String packageName : packageNames) {
				addPackage(packageName);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MappingException("Failed to load annotated classes from classpath", ex);
		}
		return this;
	}

	/**
	 * 检查任何已配置的实体类型过滤器是否与元数据读取器中包含的当前类描述符匹配.
	 */
	private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		if (this.entityTypeFilters != null) {
			for (TypeFilter filter : this.entityTypeFilters) {
				if (filter.match(reader, readerFactory)) {
					return true;
				}
			}
		}
		return false;
	}


	// Overridden methods from Hibernate's Configuration class

	@Override
	public Settings buildSettings(Properties props, ServiceRegistry serviceRegistry) throws HibernateException {
		Settings settings = super.buildSettings(props, serviceRegistry);
		if (this.cacheRegionFactory != null) {
			try {
				Method setRegionFactory = Settings.class.getDeclaredMethod("setRegionFactory", RegionFactory.class);
				setRegionFactory.setAccessible(true);
				setRegionFactory.invoke(settings, this.cacheRegionFactory);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to invoke Hibernate's setRegionFactory method", ex);
			}
		}
		return settings;
	}

	/**
	 * 构建{@code SessionFactory}.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public SessionFactory buildSessionFactory() throws HibernateException {
		ClassLoader appClassLoader = (ClassLoader) getProperties().get(AvailableSettings.APP_CLASSLOADER);
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader =
				(appClassLoader != null && !appClassLoader.equals(threadContextClassLoader));
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(appClassLoader);
		}
		try {
			return super.buildSessionFactory();
		}
		finally {
			if (overrideClassLoader) {
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}


	/**
	 * 内部类, 避免硬依赖于JPA 2.1 / Hibernate 4.3.
	 */
	private static class ConverterRegistrationDelegate {

		@SuppressWarnings("unchecked")
		public static void registerConverter(Configuration config, Class<?> converterClass) {
			config.addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) converterClass);
		}
	}
}
