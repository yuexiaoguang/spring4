package org.springframework.orm.hibernate5;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.springframework.core.InfrastructureProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring提供的标准Hibernate {@link Configuration}类的扩展, 添加{@link SpringSessionContext}作为默认值,
 * 并提供指定DataSource和应用程序类加载器的便捷方法.
 *
 * <p>这是专为程序化使用而设计的, e.g. 在{@code @Bean}工厂方法中.
 * 考虑将{@link LocalSessionFactoryBean}用于XML bean定义文件.
 *
 * <p>从Spring 4.3开始, 与Hibernate 5.0/5.1 和 5.2兼容.
 */
@SuppressWarnings("serial")
public class LocalSessionFactoryBuilder extends Configuration {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false)};

	private static final TypeFilter CONVERTER_TYPE_FILTER = new AnnotationTypeFilter(Converter.class, false);


	private final ResourcePatternResolver resourcePatternResolver;

	private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;


	/**
	 * @param dataSource 生成使用的Hibernate SessionFactory的JDBC DataSource (may be {@code null})
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource) {
		this(dataSource, new PathMatchingResourcePatternResolver());
	}

	/**
	 * @param dataSource 生成使用的Hibernate SessionFactory的JDBC DataSource (may be {@code null})
	 * @param classLoader 从中加载应用程序类的ClassLoader
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource, ClassLoader classLoader) {
		this(dataSource, new PathMatchingResourcePatternResolver(classLoader));
	}

	/**
	 * @param dataSource 生成使用的Hibernate SessionFactory的JDBC DataSource (may be {@code null})
	 * @param resourceLoader 从中加载应用程序类的ResourceLoader
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource, ResourceLoader resourceLoader) {
		this(dataSource, resourceLoader, new MetadataSources(
				new BootstrapServiceRegistryBuilder().applyClassLoader(resourceLoader.getClassLoader()).build()));
	}

	/**
	 * @param dataSource 生成使用的Hibernate SessionFactory的JDBC DataSource (may be {@code null})
	 * @param resourceLoader 从中加载应用程序类的ResourceLoader
	 * @param metadataSources 要使用的Hibernate MetadataSources服务 (e.g. 重用现有服务)
	 */
	public LocalSessionFactoryBuilder(DataSource dataSource, ResourceLoader resourceLoader, MetadataSources metadataSources) {
		super(metadataSources);

		getProperties().put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		if (dataSource != null) {
			getProperties().put(AvailableSettings.DATASOURCE, dataSource);
		}

		// Hibernate 5.1/5.2: 手动强制执行连接释放模式 ON_CLOSE (以前的默认值)
		try {
			// Try Hibernate 5.2
			AvailableSettings.class.getField("CONNECTION_HANDLING");
			getProperties().put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
		}
		catch (NoSuchFieldException ex) {
			// Try Hibernate 5.1
			try {
				AvailableSettings.class.getField("ACQUIRE_CONNECTIONS");
				getProperties().put("hibernate.connection.release_mode", "ON_CLOSE");
			}
			catch (NoSuchFieldException ex2) {
				// on Hibernate 5.0.x or lower - no need to change the default there
			}
		}

		getProperties().put(AvailableSettings.CLASSLOADERS, Collections.singleton(resourceLoader.getClassLoader()));
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * 设置Hibernate使用的Spring {@link JtaTransactionManager} 或JTA {@link TransactionManager}.
	 * 允许使用Spring管理的事务管理器进行Hibernate 5的会话和缓存同步, 并自动设置"hibernate.transaction.jta.platform".
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
						"org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform");
			}
			else {
				JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
				if (jtaTm.getTransactionManager() == null) {
					throw new IllegalArgumentException(
							"Can only apply JtaTransactionManager which has a TransactionManager reference set");
				}
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
								jtaTm.getTransactionSynchronizationRegistry()));
			}
		}
		else if (jtaTransactionManager instanceof TransactionManager) {
			getProperties().put(AvailableSettings.JTA_PLATFORM,
					new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null));
		}
		else {
			throw new IllegalArgumentException(
					"Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
		}

		// Hibernate 5.1/5.2: 手动强制执行连接释放模式AFTER_STATEMENT (JTA默认值)
		try {
			// Try Hibernate 5.2
			AvailableSettings.class.getField("CONNECTION_HANDLING");
			getProperties().put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT");
		}
		catch (NoSuchFieldException ex) {
			// Try Hibernate 5.1
			try {
				AvailableSettings.class.getField("ACQUIRE_CONNECTIONS");
				getProperties().put("hibernate.connection.release_mode", "AFTER_STATEMENT");
			}
			catch (NoSuchFieldException ex2) {
				// on Hibernate 5.0.x or lower - no need to change the default there
			}
		}

		return this;
	}

	/**
	 * 设置传递给SessionFactory的{@link MultiTenantConnectionProvider}.
	 */
	public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
		return this;
	}

	/**
	 * 重写以可靠地将{@link CurrentTenantIdentifierResolver}传递给SessionFactory.
	 */
	@Override
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		super.setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
	}

	/**
	 * 为实体类指定基于Spring的扫描的自定义类型过滤器.
	 * <p>默认是搜索所有指定的包, 查找带{@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * 或{@code @javax.persistence.MappedSuperclass}注解的类.
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
	@SuppressWarnings("unchecked")
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
						else if (CONVERTER_TYPE_FILTER.match(reader, readerFactory)) {
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
				addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) cl.loadClass(className));
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

	/**
	 * 通过后台引导构建Hibernate {@code SessionFactory}, 使用给定的执行器, 用于并行初始化阶段
	 * (e.g. a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}).
	 * <p>{@code SessionFactory}初始化将切换到后台引导模式, 并立即返回{@code SessionFactory}代理以进行注入,
	 * 而不是等待Hibernate的引导完成.
	 * 但是, 请注意, 对{@code SessionFactory}方法的第一次实际调用将阻塞, 直到Hibernate的引导完成, 如果还没有准备就绪.
	 * 为了获得最大收益, 确保避免在相关bean的init方法中进行实时{@code SessionFactory}调用, 即使是用于元数据内省目的.
	 */
	public SessionFactory buildSessionFactory(AsyncTaskExecutor bootstrapExecutor) {
		Assert.notNull(bootstrapExecutor, "AsyncTaskExecutor must not be null");
		return (SessionFactory) Proxy.newProxyInstance(this.resourcePatternResolver.getClassLoader(),
				new Class<?>[] {SessionFactoryImplementor.class, InfrastructureProxy.class},
				new BootstrapSessionFactoryInvocationHandler(bootstrapExecutor));
	}


	/**
	 * 用于后台引导的代理调用处理器, 仅在实际需要时强制完全初始化目标{@code SessionFactory}.
	 */
	private class BootstrapSessionFactoryInvocationHandler implements InvocationHandler {

		private final Future<SessionFactory> sessionFactoryFuture;

		public BootstrapSessionFactoryInvocationHandler(AsyncTaskExecutor bootstrapExecutor) {
			this.sessionFactoryFuture = bootstrapExecutor.submit(new Callable<SessionFactory>() {
				@Override
				public SessionFactory call() throws Exception {
					return buildSessionFactory();
				}
			});
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("equals")) {
					// 只有当代理相同时才考虑相等.
					return (proxy == args[0]);
				}
				else if (method.getName().equals("hashCode")) {
					// 使用EntityManagerFactory代理的hashCode.
					return System.identityHashCode(proxy);
				}
				else if (method.getName().equals("getProperties")) {
					return getProperties();
				}
				else if (method.getName().equals("getWrappedObject")) {
					// 通过InfrastructureProxy接口调用...
					return getSessionFactory();
				}
				// 委托到目标SessionFactory, 强制执行其完全初始化...
				return method.invoke(getSessionFactory(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private SessionFactory getSessionFactory() {
			try {
				return this.sessionFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted during initialization of Hibernate SessionFactory", ex);
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof HibernateException) {
					// 直接重新抛出提供者配置异常 (可能具有嵌套原因)
					throw (HibernateException) cause;
				}
				throw new IllegalStateException("Failed to asynchronously initialize Hibernate SessionFactory: " +
						ex.getMessage(), cause);
			}
		}
	}
}
