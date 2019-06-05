package org.springframework.orm.hibernate5;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * 创建Hibernate {@link SessionFactory}的{@link FactoryBean}.
 * 这是在Spring应用程序上下文中设置共享Hibernate SessionFactory的常用方法;
 * 然后, 可以通过依赖注入将SessionFactory传递给基于Hibernate的数据访问对象.
 *
 * <p>从Spring 4.3开始, 与 Hibernate 5.0/5.1 和 5.2兼容.
 */
public class LocalSessionFactoryBean extends HibernateExceptionTranslator
		implements FactoryBean<SessionFactory>, ResourceLoaderAware, InitializingBean, DisposableBean {

	private DataSource dataSource;

	private Resource[] configLocations;

	private String[] mappingResources;

	private Resource[] mappingLocations;

	private Resource[] cacheableMappingLocations;

	private Resource[] mappingJarLocations;

	private Resource[] mappingDirectoryLocations;

	private Interceptor entityInterceptor;

	private ImplicitNamingStrategy implicitNamingStrategy;

	private PhysicalNamingStrategy physicalNamingStrategy;

	private Object jtaTransactionManager;

	private MultiTenantConnectionProvider multiTenantConnectionProvider;

	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	private TypeFilter[] entityTypeFilters;

	private Properties hibernateProperties;

	private Class<?>[] annotatedClasses;

	private String[] annotatedPackages;

	private String[] packagesToScan;

	private AsyncTaskExecutor bootstrapExecutor;

	private boolean metadataSourcesAccessed = false;

	private MetadataSources metadataSources;

	private ResourcePatternResolver resourcePatternResolver;

	private Configuration configuration;

	private SessionFactory sessionFactory;


	/**
	 * 设置SessionFactory使用的DataSource.
	 * 如果设置, 这将覆盖Hibernate属性中的相应设置.
	 * <p>如果设置了此项, 则Hibernate设置不应定义连接提供者, 以避免无意义的双重配置.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置单个Hibernate XML配置文件的位置, 例如作为类路径资源"classpath:hibernate.cfg.xml".
	 * <p>Note: 当通过此bean在本地指定所有必需的属性和映射资源时, 可以省略.
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocations = new Resource[] {configLocation};
	}

	/**
	 * 设置多个Hibernate XML配置文件的位置, 例如作为类路径资源"classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
	 * <p>Note: 当通过此bean在本地指定所有必需的属性和映射资源时, 可以省略.
	 */
	public void setConfigLocations(Resource... configLocations) {
		this.configLocations = configLocations;
	}

	/**
	 * 设置在类路径中找到的Hibernate映射资源, 如"example.hbm.xml"或"mypackage/example.hbm.xml".
	 * 类似于Hibernate XML配置文件中的映射条目. 替代更通用的setMappingLocations方法.
	 * <p>可用于从Hibernate XML配置文件添加映射, 或在本地指定所有映射.
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * 设置Hibernate映射文件的位置, 例如作为类路径资源"classpath:example.hbm.xml".
	 * 通过Spring的资源抽象支持任何资源位置, 例如在应用程序上下文中运行时的相对路径,
	 * 如"WEB-INF/mappings/example.hbm.xml".
	 * <p>可用于从Hibernate XML配置文件添加映射, 或在本地指定所有映射.
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * 设置可缓存的Hibernate映射文件的位置, 例如作为Web应用程序资源"/WEB-INF/mapping/example.hbm.xml".
	 * 只要资源可以在文件系统中解析, 就可以通过Spring的资源抽象支持任何资源位置.
	 * <p>可用于从Hibernate XML配置文件添加映射, 或在本地指定所有映射.
	 */
	public void setCacheableMappingLocations(Resource... cacheableMappingLocations) {
		this.cacheableMappingLocations = cacheableMappingLocations;
	}

	/**
	 * 设置包含Hibernate映射资源的jar文件的位置, 例如"WEB-INF/lib/example.hbm.jar".
	 * <p>可用于从Hibernate XML配置文件添加映射, 或在本地指定所有映射.
	 */
	public void setMappingJarLocations(Resource... mappingJarLocations) {
		this.mappingJarLocations = mappingJarLocations;
	}

	/**
	 * 设置包含Hibernate映射资源的目录的位置, 例如"WEB-INF/mappings".
	 * <p>可用于从Hibernate XML配置文件添加映射, 或在本地指定所有映射.
	 */
	public void setMappingDirectoryLocations(Resource... mappingDirectoryLocations) {
		this.mappingDirectoryLocations = mappingDirectoryLocations;
	}

	/**
	 * 设置一个Hibernate实体拦截器, 允许在写入和读取数据库之前检查和更改属性值.
	 * 将应用于此工厂创建的任何新会话.
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * 为SessionFactory设置Hibernate 5.0 ImplicitNamingStrategy.
	 */
	public void setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	/**
	 * 为SessionFactory设置Hibernate 5.0 PhysicalNamingStrategy.
	 */
	public void setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	/**
	 * 设置Hibernate使用的Spring {@link org.springframework.transaction.jta.JtaTransactionManager}
	 * 或JTA {@link javax.transaction.TransactionManager}. 隐式设置{@code JtaPlatform}.
	 */
	public void setJtaTransactionManager(Object jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
	}

	/**
	 * 设置传递给SessionFactory的{@link MultiTenantConnectionProvider}.
	 */
	public void setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		this.multiTenantConnectionProvider = multiTenantConnectionProvider;
	}

	/**
	 * 设置要传递给SessionFactory的{@link CurrentTenantIdentifierResolver}.
	 */
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
	}

	/**
	 * 指定实体类的基于Spring的扫描的自定义类型过滤器.
	 * <p>默认是搜索所有指定的包, 查找带{@code @javax.persistence.Entity}, {@code @javax.persistence.Embeddable}
	 * 或{@code @javax.persistence.MappedSuperclass}注解的类.
	 */
	public void setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
	}

	/**
	 * 设置Hibernate属性, 例如"hibernate.dialect".
	 * <p>Note: 使用Spring驱动的事务时, 不要在此处指定事务提供者.
	 * 建议省略连接提供者设置并使用Spring设置的 DataSource.
	 */
	public void setHibernateProperties(Properties hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	/**
	 * 返回Hibernate属性.
	 * 主要用于通过指定单个键的属性路径进行配置.
	 */
	public Properties getHibernateProperties() {
		if (this.hibernateProperties == null) {
			this.hibernateProperties = new Properties();
		}
		return this.hibernateProperties;
	}

	/**
	 * 指定带注解的实体类, 以向此Hibernate SessionFactory注册.
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * 指定带注解的包的名称, 以便读取包级别的注解元数据.
	 */
	public void setAnnotatedPackages(String... annotatedPackages) {
		this.annotatedPackages = annotatedPackages;
	}

	/**
	 * 指定在类路径中搜索实体类的自动检测的包.
	 * 这类似于Spring的组件扫描功能
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 指定后台引导的异步执行器, e.g. {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * <p>然后{@code SessionFactory}初始化将切换到后台引导模式, 并立即返回{@code SessionFactory}代理以进行注入,
	 * 而不是等待Hibernate的引导完成.
	 * 但是, 对{@code SessionFactory}方法的第一次实际调用将阻塞, 直到Hibernate的引导完成, 如果还没有准备就绪.
	 * 为了获得最大收益, 确保避免在相关bean的init方法中进行实时{@code SessionFactory}调用, 即使是为了元数据内省目的.
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * 指定要使用的Hibernate {@link MetadataSources}服务 (e.g. 重用现有服务),
	 * 可能还会填充自定义Hibernate引导 {@link org.hibernate.service.ServiceRegistry}.
	 */
	public void setMetadataSources(MetadataSources metadataSources) {
		Assert.notNull(metadataSources, "MetadataSources must not be null");
		this.metadataSourcesAccessed = true;
		this.metadataSources = metadataSources;
	}

	/**
	 * 确定要使用的Hibernate {@link MetadataSources}.
	 * <p>也可以在外部调用以初始化和预填充{@link MetadataSources}实例, 然后将其用于{@link SessionFactory}构建.
	 * 
	 * @return 要使用的MetadataSources (never {@code null})
	 */
	public MetadataSources getMetadataSources() {
		this.metadataSourcesAccessed = true;
		if (this.metadataSources == null) {
			BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
			if (this.resourcePatternResolver != null) {
				builder = builder.applyClassLoader(this.resourcePatternResolver.getClassLoader());
			}
			this.metadataSources = new MetadataSources(builder.build());
		}
		return this.metadataSources;
	}

	/**
	 * 指定用于Hibernate元数据的Spring {@link ResourceLoader}.
	 * 
	 * @param resourceLoader 要使用的ResourceLoader (never {@code null})
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	/**
	 * 确定用于Hibernate元数据的Spring {@link ResourceLoader}.
	 * 
	 * @return 要使用的ResourceLoader (never {@code null})
	 */
	public ResourceLoader getResourceLoader() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}


	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.metadataSources != null && !this.metadataSourcesAccessed) {
			// 重复初始化, 没有用户自定义的MetadataSources -> 清除它.
			this.metadataSources = null;
		}

		LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder(
				this.dataSource, getResourceLoader(), getMetadataSources());

		if (this.configLocations != null) {
			for (Resource resource : this.configLocations) {
				// 从给定位置加载Hibernate配置.
				sfb.configure(resource.getURL());
			}
		}

		if (this.mappingResources != null) {
			// 注册给定的Hibernate映射定义, 包含在资源文件中.
			for (String mapping : this.mappingResources) {
				Resource mr = new ClassPathResource(mapping.trim(), this.resourcePatternResolver.getClassLoader());
				sfb.addInputStream(mr.getInputStream());
			}
		}

		if (this.mappingLocations != null) {
			// 注册给定的Hibernate映射定义, 包含在资源文件中.
			for (Resource resource : this.mappingLocations) {
				sfb.addInputStream(resource.getInputStream());
			}
		}

		if (this.cacheableMappingLocations != null) {
			// 注册给定的可缓存Hibernate映射定义, 从文件系统中读取.
			for (Resource resource : this.cacheableMappingLocations) {
				sfb.addCacheableFile(resource.getFile());
			}
		}

		if (this.mappingJarLocations != null) {
			// 注册给定的Hibernate映射定义, 包含在jar文件中.
			for (Resource resource : this.mappingJarLocations) {
				sfb.addJar(resource.getFile());
			}
		}

		if (this.mappingDirectoryLocations != null) {
			// 在给定目录中注册所有Hibernate映射定义.
			for (Resource resource : this.mappingDirectoryLocations) {
				File file = resource.getFile();
				if (!file.isDirectory()) {
					throw new IllegalArgumentException(
							"Mapping directory location [" + resource + "] does not denote a directory");
				}
				sfb.addDirectory(file);
			}
		}

		if (this.entityInterceptor != null) {
			sfb.setInterceptor(this.entityInterceptor);
		}

		if (this.implicitNamingStrategy != null) {
			sfb.setImplicitNamingStrategy(this.implicitNamingStrategy);
		}

		if (this.physicalNamingStrategy != null) {
			sfb.setPhysicalNamingStrategy(this.physicalNamingStrategy);
		}

		if (this.jtaTransactionManager != null) {
			sfb.setJtaTransactionManager(this.jtaTransactionManager);
		}

		if (this.multiTenantConnectionProvider != null) {
			sfb.setMultiTenantConnectionProvider(this.multiTenantConnectionProvider);
		}

		if (this.currentTenantIdentifierResolver != null) {
			sfb.setCurrentTenantIdentifierResolver(this.currentTenantIdentifierResolver);
		}

		if (this.entityTypeFilters != null) {
			sfb.setEntityTypeFilters(this.entityTypeFilters);
		}

		if (this.hibernateProperties != null) {
			sfb.addProperties(this.hibernateProperties);
		}

		if (this.annotatedClasses != null) {
			sfb.addAnnotatedClasses(this.annotatedClasses);
		}

		if (this.annotatedPackages != null) {
			sfb.addPackages(this.annotatedPackages);
		}

		if (this.packagesToScan != null) {
			sfb.scanPackages(this.packagesToScan);
		}

		// Build SessionFactory instance.
		this.configuration = sfb;
		this.sessionFactory = buildSessionFactory(sfb);
	}

	/**
	 * 子类可以重写此方法以执行SessionFactory实例的自定义初始化,
	 * 通过此LocalSessionFactoryBean准备的给定Configuration对象创建它.
	 * <p>默认实现调用LocalSessionFactoryBuilder的buildSessionFactory.
	 * 自定义实现可以以特定方式准备实例 (e.g. 应用自定义ServiceRegistry), 或使用自定义SessionFactoryImpl子类.
	 * 
	 * @param sfb 由此LocalSessionFactoryBean准备的LocalSessionFactoryBuilder
	 * 
	 * @return SessionFactory实例
	 */
	protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
		return (this.bootstrapExecutor != null ? sfb.buildSessionFactory(this.bootstrapExecutor) :
				sfb.buildSessionFactory());
	}

	/**
	 * 返回用于构建SessionFactory的Hibernate Configuration对象.
	 * 允许访问存储在那里的配置元数据 (很少需要).
	 * 
	 * @throws IllegalStateException 如果尚未初始化Configuration对象
	 */
	public final Configuration getConfiguration() {
		if (this.configuration == null) {
			throw new IllegalStateException("Configuration not initialized yet");
		}
		return this.configuration;
	}


	@Override
	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		this.sessionFactory.close();
	}

}
