package org.springframework.context.annotation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * 一个bean定义扫描程序, 用于检测类路径上的bean候选项,
 * 使用给定的注册表注册相应的bean定义 ({@code BeanFactory} 或 {@code ApplicationContext}).
 *
 * <p>通过可配置的类型过滤器检测候选类.
 * 默认过滤器包括使用Spring的
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Repository @Repository},
 * {@link org.springframework.stereotype.Service @Service},
 * {@link org.springframework.stereotype.Controller @Controller}构造型注解的类.
 *
 * <p>还支持Java EE 6的 {@link javax.annotation.ManagedBean} 和JSR-330的 {@link javax.inject.Named}注解.
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

	private final BeanDefinitionRegistry registry;

	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

	private String[] autowireCandidatePatterns;

	private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private boolean includeAnnotationConfig = true;


	/**
	 * @param registry 以{@code BeanDefinitionRegistry}的形式加载bean定义的{@code BeanFactory}
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
		this(registry, true);
	}

	/**
	 * 为给定的bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}.
	 * <p>如果传入的bean工厂不仅实现了 {@code BeanDefinitionRegistry}接口, 还实现了{@code ResourceLoader}接口,
	 * 它也将用作默认的 {@code ResourceLoader}.
	 * 这通常是 {@link org.springframework.context.ApplicationContext}实现的情况.
	 * <p>如果给定一个普通的 {@code BeanDefinitionRegistry}, 默认的{@code ResourceLoader} 将是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * <p>如果传入的bean工厂也实现了 {@link EnvironmentCapable}, 则此读取器将使用其环境.
	 * 否则, 读取器将初始化并使用 {@link org.springframework.core.env.StandardEnvironment}.
	 * 所有的{@code ApplicationContext}实现都是{@code EnvironmentCapable}, 而普通的{@code BeanFactory}实现不是.
	 * 
	 * @param registry 以{@code BeanDefinitionRegistry}的形式加载bean定义的{@code BeanFactory}
	 * @param useDefaultFilters 是否包含
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service},
	 * {@link org.springframework.stereotype.Controller @Controller}构造型注解的默认过滤器
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
	}

	/**
	 * 在评估bean定义配置文件元数据时,
	 * 为给定的bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}并使用给定的{@link Environment}.
	 * <p>如果传入的bean工厂不仅实现了 {@code BeanDefinitionRegistry}接口, 还实现了 {@link ResourceLoader}接口,
	 * 它也将用作默认的 {@code ResourceLoader}.
	 * 这通常是 {@link org.springframework.context.ApplicationContext}实现的情况.
	 * <p>如果给定一个普通的 {@code BeanDefinitionRegistry}, 默认的{@code ResourceLoader} 将是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * 
	 * @param registry 以{@code BeanDefinitionRegistry}的形式加载bean定义的{@code BeanFactory}
	 * @param useDefaultFilters 是否包含
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service},
	 * {@link org.springframework.stereotype.Controller @Controller}构造型注解的默认过滤器
	 * @param environment 在评估bean定义概要文件元数据时, 使用的Spring {@link Environment}
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment) {

		this(registry, useDefaultFilters, environment,
				(registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
	}

	/**
	 * 在评估bean定义配置文件元数据时, 为给定的bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}, 并使用给定的{@link Environment}.
	 * 
	 * @param registry 以{@code BeanDefinitionRegistry}的形式加载bean定义的{@code BeanFactory}
	 * @param useDefaultFilters 是否包含
	 * {@link org.springframework.stereotype.Component @Component},
	 * {@link org.springframework.stereotype.Repository @Repository},
	 * {@link org.springframework.stereotype.Service @Service},
	 * {@link org.springframework.stereotype.Controller @Controller}构造型注解的默认过滤器
	 * @param environment 在评估bean定义概要文件元数据时, 使用的Spring {@link Environment}
	 * @param resourceLoader 要使用的{@link ResourceLoader}
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment, ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		if (useDefaultFilters) {
			registerDefaultFilters();
		}
		setEnvironment(environment);
		setResourceLoader(resourceLoader);
	}


	/**
	 * 返回此扫描器操作的BeanDefinitionRegistry.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于检测bean的默认值.
	 */
	public void setBeanDefinitionDefaults(BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults =
				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
	}

	/**
	 * 返回用于检测bean的默认值 (never {@code null}).
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	/**
	 * 设置确定autowire候选者的名称匹配模式.
	 * 
	 * @param autowireCandidatePatterns 要匹配的模式
	 */
	public void setAutowireCandidatePatterns(String... autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
	}

	/**
	 * 设置用于检测bean类的BeanNameGenerator.
	 * <p>默认是 {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new AnnotationBeanNameGenerator());
	}

	/**
	 * 设置用于检测bean类的ScopeMetadataResolver.
	 * 请注意, 这将覆盖自定义 "scopedProxyMode"设置.
	 * <p>默认是 {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}

	/**
	 * 指定非单例作用域bean的代理行为.
	 * 请注意, 这将覆盖自定义 "scopeMetadataResolver"设置.
	 * <p>默认是 {@link ScopedProxyMode#NO}.
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * 指定是否注册注解配置后处理器.
	 * <p>默认是注册后处理器. 将其关闭可以忽略注解, 或以不同方式处理它们.
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	/**
	 * 在指定的基础包中执行扫描.
	 * 
	 * @param basePackages 用于检查带注解的类的包
	 * 
	 * @return 注册的bean数量
	 */
	public int scan(String... basePackages) {
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

		doScan(basePackages);

		// 注册注解配置处理器.
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}

		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}

	/**
	 * 在指定的基础包中执行扫描, 返回已注册的bean定义.
	 * <p>此方法不会注册注解配置处理器，而是将其留给调用者.
	 * 
	 * @param basePackages 要检查带注解的类的包
	 * 
	 * @return 为工具注册目的而注册的一组bean (never {@code null})
	 */
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates) {
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}

	/**
	 * 除了从扫描组件类检索到的内容之外, 还将其他设置应用于给定的bean定义.
	 * 
	 * @param beanDefinition 扫描到的bean定义
	 * @param beanName 给定bean的生成的bean名称
	 */
	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
		if (this.autowireCandidatePatterns != null) {
			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
		}
	}

	/**
	 * 使用给定的注册表注册指定的bean.
	 * <p>可以在子类中重写, e.g. 调整注册过程, 或为每个扫描到的bean注册更多的bean定义.
	 * 
	 * @param definitionHolder bean的bean定义以及bean名称
	 * @param registry 用于注册bean的BeanDefinitionRegistry
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}


	/**
	 * 检查给定候选者的bean名称, 确定是否需要注册相应的bean定义, 或与现有的定义冲突.
	 * 
	 * @param beanName bean的建议名称
	 * @param beanDefinition 相应的bean定义
	 * 
	 * @return {@code true} 如果bean可以按原样注册;
	 * {@code false} 是否应该跳过它, 因为存在指定名称的现有的兼容的bean定义
	 * @throws ConflictingBeanDefinitionException 如果找到了指定名称的现有的不兼容的bean定义
	 */
	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return true;
		}
		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
		if (originatingDef != null) {
			existingDef = originatingDef;
		}
		if (isCompatible(beanDefinition, existingDef)) {
			return false;
		}
		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
	}

	/**
	 * 确定给定的新bean定义是否与给定的现有bean定义兼容.
	 * <p>当现有bean定义来自同一源或来自非扫描源时, 默认实现将它们视为兼容.
	 * 
	 * @param newDefinition 新的bean定义, 源于扫描
	 * @param existingDefinition 现有的bean定义, 可能是明确定义的bean定义, 也可能是先前从扫描中生成的定义
	 * 
	 * @return 这些定义是否被认为是兼容的, 跳过新定义, 而不是现有定义
	 */
	protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
		return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
				newDefinition.getSource().equals(existingDefinition.getSource()) ||  // scanned same file twice
				newDefinition.equals(existingDefinition));  // scanned equivalent class twice
	}


	/**
	 * 如果可能, 从给定的注册表中获取Environment, 否则返回一个新的StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
