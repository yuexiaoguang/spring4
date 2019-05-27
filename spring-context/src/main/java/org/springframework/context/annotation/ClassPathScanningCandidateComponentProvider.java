package org.springframework.context.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 从基础包扫描类路径的组件提供程序.
 * 然后, 它会对结果类应用排除和包含过滤器以查找候选项.
 *
 * <p>此实现基于Spring的
 * {@link org.springframework.core.type.classreading.MetadataReader MetadataReader}功能,
 * 由ASM {@link org.springframework.asm.ClassReader ClassReader}支持.
 */
public class ClassPathScanningCandidateComponentProvider implements EnvironmentCapable, ResourceLoaderAware {

	static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";


	protected final Log logger = LogFactory.getLog(getClass());

	private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

	private final List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();

	private final List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();

	private Environment environment;

	private ConditionEvaluator conditionEvaluator;

	private ResourcePatternResolver resourcePatternResolver;

	private MetadataReaderFactory metadataReaderFactory;


	/**
	 * 受保护的构造函数, 用于灵活的子类初始化.
	 */
	protected ClassPathScanningCandidateComponentProvider() {
	}

	/**
	 * @param useDefaultFilters 是否注册
	 * {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, {@link Controller @Controller}构造型注解的默认过滤器
	 */
	public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
		this(useDefaultFilters, new StandardEnvironment());
	}

	/**
	 * @param useDefaultFilters 是否注册
	 * {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, {@link Controller @Controller}构造型注解的默认过滤器
	 * @param environment 要使用的Environment
	 */
	public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters, Environment environment) {
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
		setEnvironment(environment);
		setResourceLoader(null);
	}


	/**
	 * 设置扫描类路径时要使用的资源模式.
	 * 此值将附加到每个基本包名称.
	 */
	public void setResourcePattern(String resourcePattern) {
		Assert.notNull(resourcePattern, "'resourcePattern' must not be null");
		this.resourcePattern = resourcePattern;
	}

	/**
	 * 将包含类型过滤器添加到包含列表的<i>结尾</i>.
	 */
	public void addIncludeFilter(TypeFilter includeFilter) {
		this.includeFilters.add(includeFilter);
	}

	/**
	 * 将排除类型过滤器添加到排除列表的 <i>开头</i>.
	 */
	public void addExcludeFilter(TypeFilter excludeFilter) {
		this.excludeFilters.add(0, excludeFilter);
	}

	/**
	 * 重置配置的类型过滤器.
	 * 
	 * @param useDefaultFilters 是否重新注册
	 * the {@link Component @Component}, {@link Repository @Repository},
	 * {@link Service @Service}, {@link Controller @Controller}构造型注解的默认过滤器
	 */
	public void resetFilters(boolean useDefaultFilters) {
		this.includeFilters.clear();
		this.excludeFilters.clear();
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
	}

	/**
	 * 注册{@link Component @Component}的默认过滤器.
	 * <p>这将隐式注册所有具有{@link Component @Component}元注解的注解, 包括
	 * {@link Repository @Repository}, {@link Service @Service},
	 * {@link Controller @Controller}构造型注解.
	 * <p>还支持Java EE 6的{@link javax.annotation.ManagedBean}和JSR-330的{@link javax.inject.Named}注解.
	 */
	@SuppressWarnings("unchecked")
	protected void registerDefaultFilters() {
		this.includeFilters.add(new AnnotationTypeFilter(Component.class));
		ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
		try {
			this.includeFilters.add(new AnnotationTypeFilter(
					((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
			logger.debug("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");
		}
		catch (ClassNotFoundException ex) {
			// JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.
		}
		try {
			this.includeFilters.add(new AnnotationTypeFilter(
					((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
			logger.debug("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}

	/**
	 * 设置解析占位符时使用的Environment, 并评估带{@link Conditional @Conditional}注解的组件类.
	 * <p>默认是 {@link StandardEnvironment}.
	 * 
	 * @param environment 要使用的Environment
	 */
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
		this.conditionEvaluator = null;
	}

	@Override
	public final Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * 返回此扫描程序使用的 {@link BeanDefinitionRegistry}.
	 */
	protected BeanDefinitionRegistry getRegistry() {
		return null;
	}

	/**
	 * 设置用于资源位置的 {@link ResourceLoader}.
	 * 这通常是{@link ResourcePatternResolver}实现.
	 * <p>默认是 {@code PathMatchingResourcePatternResolver}, 还能够通过 {@code ResourcePatternResolver}接口解析资源模式.
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	/**
	 * 返回此组件提供程序使用的ResourceLoader.
	 */
	public final ResourceLoader getResourceLoader() {
		return this.resourcePatternResolver;
	}

	/**
	 * 设置要使用的{@link MetadataReaderFactory}.
	 * <p>默认是{@link CachingMetadataReaderFactory}, 对于指定的{@linkplain #setResourceLoader 资源加载器}.
	 * <p>在{@link #setResourceLoader}之后调用此setter方法, 以便给定的MetadataReaderFactory覆盖默认工厂.
	 */
	public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
		this.metadataReaderFactory = metadataReaderFactory;
	}

	/**
	 * 返回此组件提供程序使用的MetadataReaderFactory.
	 */
	public final MetadataReaderFactory getMetadataReaderFactory() {
		return this.metadataReaderFactory;
	}


	/**
	 * 扫描候选组件的类路径.
	 * 
	 * @param basePackage 要检查带注解的类的包
	 * 
	 * @return 相应的一组自动检测到的bean定义
	 */
	public Set<BeanDefinition> findCandidateComponents(String basePackage) {
		Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
		try {
			String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					resolveBasePackage(basePackage) + '/' + this.resourcePattern;
			Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
			boolean traceEnabled = logger.isTraceEnabled();
			boolean debugEnabled = logger.isDebugEnabled();
			for (Resource resource : resources) {
				if (traceEnabled) {
					logger.trace("Scanning " + resource);
				}
				if (resource.isReadable()) {
					try {
						MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
						if (isCandidateComponent(metadataReader)) {
							ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
							sbd.setResource(resource);
							sbd.setSource(resource);
							if (isCandidateComponent(sbd)) {
								if (debugEnabled) {
									logger.debug("Identified candidate component class: " + resource);
								}
								candidates.add(sbd);
							}
							else {
								if (debugEnabled) {
									logger.debug("Ignored because not a concrete top-level class: " + resource);
								}
							}
						}
						else {
							if (traceEnabled) {
								logger.trace("Ignored because not matching any filter: " + resource);
							}
						}
					}
					catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to read candidate component class: " + resource, ex);
					}
				}
				else {
					if (traceEnabled) {
						logger.trace("Ignored because not readable: " + resource);
					}
				}
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
		}
		return candidates;
	}


	/**
	 * 将指定的基础包解析为包搜索路径的模式规范.
	 * <p>默认实现根据系统属性解析占位符, 并将基于 "."的包路径转换为基于 "/"的资源路径.
	 * 
	 * @param basePackage 用户指定的基本包
	 * 
	 * @return 用于包搜索的模式规范
	 */
	protected String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(this.environment.resolveRequiredPlaceholders(basePackage));
	}

	/**
	 * 确定给定的类是否与任何排除过滤器不匹配, 并且确实匹配至少一个包含过滤器.
	 * 
	 * @param metadataReader 该类的ASM ClassReader
	 * 
	 * @return 该类是否有资格作为候选组件
	 */
	protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
		for (TypeFilter tf : this.excludeFilters) {
			if (tf.match(metadataReader, this.metadataReaderFactory)) {
				return false;
			}
		}
		for (TypeFilter tf : this.includeFilters) {
			if (tf.match(metadataReader, this.metadataReaderFactory)) {
				return isConditionMatch(metadataReader);
			}
		}
		return false;
	}

	/**
	 * 根据{@code @Conditional}注解, 确定给定的类是否为候选组件.
	 * 
	 * @param metadataReader 该类的ASM ClassReader
	 * 
	 * @return 该类是否有资格作为候选组件
	 */
	private boolean isConditionMatch(MetadataReader metadataReader) {
		if (this.conditionEvaluator == null) {
			this.conditionEvaluator = new ConditionEvaluator(getRegistry(), getEnvironment(), getResourceLoader());
		}
		return !this.conditionEvaluator.shouldSkip(metadataReader.getAnnotationMetadata());
	}

	/**
	 * 确定给定的b​​ean定义是否符合候选条件.
	 * <p>默认实现检查该类是否不是接口, 并且不依赖于封闭类.
	 * <p>可以在子类中重写.
	 * 
	 * @param beanDefinition 要检查的bean定义
	 * 
	 * @return bean定义是否有资格作为候选组件
	 */
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		AnnotationMetadata metadata = beanDefinition.getMetadata();
		return (metadata.isIndependent() && (metadata.isConcrete() ||
				(metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
	}


	/**
	 * 清除底层元数据缓存, 删除所有缓存的类元数据.
	 */
	public void clearCache() {
		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}

}
