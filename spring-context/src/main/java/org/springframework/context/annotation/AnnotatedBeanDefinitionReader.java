package org.springframework.context.annotation;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

/**
 * 适配器, 用于被注解的bean类的编程注册.
 * 这是{@link ClassPathBeanDefinitionScanner}的替代方法, 应用相同的注解解析, 但仅适用于显式注册的类.
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private ConditionEvaluator conditionEvaluator;


	/**
	 * 为给定的注册表创建一个新的{@code AnnotatedBeanDefinitionReader}.
	 * 如果注册表是{@link EnvironmentCapable}, e.g. 是一个{@code ApplicationContext},
	 * {@link Environment}将被继承, 否则将创建并使用新的{@link StandardEnvironment}.
	 * 
	 * @param registry {@code BeanFactory}以{@code BeanDefinitionRegistry}的形式加载bean定义
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * 为给定的注册表创建一个新的{@code AnnotatedBeanDefinitionReader}, 并使用给定的{@link Environment}.
	 * 
	 * @param registry {@code BeanFactory}以{@code BeanDefinitionRegistry}的形式加载bean定义
	 * @param environment 评估bean定义配置文件时要使用的{@code Environment}.
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * 返回此扫描程序操作的BeanDefinitionRegistry.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置要在评估是否应注册被注解的{@link Conditional @Conditional}组件类时使用的Environment.
	 * <p>默认是 {@link StandardEnvironment}.
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
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
	 * <p>默认是 {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 注册一个或多个要处理的被注解的类.
	 * <p>对{@code register}的调用是幂等的; 多次添加相同的被注解的类没有额外的效果.
	 * 
	 * @param annotatedClasses 一个或多个被注解的类, e.g. {@link Configuration @Configuration}类
	 */
	public void register(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			registerBean(annotatedClass);
		}
	}

	/**
	 * 从给定的bean类注册bean, 从类声明的注解中派生其元数据.
	 * 
	 * @param annotatedClass bean类
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass) {
		registerBean(annotatedClass, null, (Class<? extends Annotation>[]) null);
	}

	/**
	 * 从给定的bean类注册bean, 从类声明的注解中派生其元数据.
	 * 
	 * @param annotatedClass bean类
	 * @param qualifiers 除了bean类级别的限定符之外, 还要考虑特定的限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
		registerBean(annotatedClass, null, qualifiers);
	}

	/**
	 * 从给定的bean类注册bean, 从类声明的注解中派生其元数据.
	 * 
	 * @param annotatedClass bean类
	 * @param name bean的显式名称
	 * @param qualifiers 除了bean类级别的限定符之外, 还要考虑特定的限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, String name, Class<? extends Annotation>... qualifiers) {
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}

		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * 从给定的注册表中获取环境, 或者返回一个新的StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
