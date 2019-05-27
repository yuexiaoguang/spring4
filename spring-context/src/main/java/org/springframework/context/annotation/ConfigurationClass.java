package org.springframework.context.annotation;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 表示用户定义的{@link Configuration @Configuration}类.
 * 包括一组{@link Bean}方法, 包括在类的祖先中定义的所有这些方法, 以'扁平化'的方式.
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	private String beanName;

	private final Set<ConfigurationClass> importedBy = new LinkedHashSet<ConfigurationClass>(1);

	private final Set<BeanMethod> beanMethods = new LinkedHashSet<BeanMethod>();

	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<String, Class<? extends BeanDefinitionReader>>();

	private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
			new LinkedHashMap<ImportBeanDefinitionRegistrar, AnnotationMetadata>();

	final Set<String> skippedBeanMethods = new HashSet<String>();


	/**
	 * @param metadataReader 用于解析底层{@link Class}的读取器
	 * @param beanName 不能是{@code null}
	 */
	public ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.hasText(beanName, "Bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
	}

	/**
	 * 创建一个新的{@link ConfigurationClass},
	 * 表示使用{@link Import}注解导入的或自动处理的类作为嵌套的配置类 (如果importedBy 不是{@code null}).
	 * 
	 * @param metadataReader 用于解析底层{@link Class}的读取器
	 * @param importedBy 导入这个的配置类或{@code null}
	 */
	public ConfigurationClass(MetadataReader metadataReader, ConfigurationClass importedBy) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.importedBy.add(importedBy);
	}

	/**
	 * @param clazz 要表示的底层{@link Class}
	 * @param beanName {@code @Configuration}类bean的名称
	 */
	public ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.hasText(beanName, "Bean name must not be null");
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.getName());
		this.beanName = beanName;
	}

	/**
	 * 创建一个新的{@link ConfigurationClass},
	 * 表示使用{@link Import}注解导入的或自动处理的类作为嵌套的配置类 (如果imported是{@code true}).
	 * 
	 * @param clazz 要表示的底层{@link Class}
	 * @param importedBy 导入这个的配置类或{@code null}
	 */
	public ConfigurationClass(Class<?> clazz, ConfigurationClass importedBy) {
		this.metadata = new StandardAnnotationMetadata(clazz, true);
		this.resource = new DescriptiveResource(clazz.getName());
		this.importedBy.add(importedBy);
	}

	/**
	 * @param metadata 要表示的底层类的元数据
	 * @param beanName {@code @Configuration}类bean的名称
	 */
	public ConfigurationClass(AnnotationMetadata metadata, String beanName) {
		Assert.hasText(beanName, "Bean name must not be null");
		this.metadata = metadata;
		this.resource = new DescriptiveResource(metadata.getClassName());
		this.beanName = beanName;
	}


	public AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	public Resource getResource() {
		return this.resource;
	}

	public String getSimpleName() {
		return ClassUtils.getShortName(getMetadata().getClassName());
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回此配置类是通过 @{@link Import}注册, 还是由于嵌套在另一个配置类中而自动注册.
	 */
	public boolean isImported() {
		return !this.importedBy.isEmpty();
	}

	/**
	 * 将给定配置类中的import-by声明合并到此声明中.
	 */
	public void mergeImportedBy(ConfigurationClass otherConfigClass) {
		this.importedBy.addAll(otherConfigClass.importedBy);
	}

	/**
	 * 返回导入此类的配置类; 如果未导入此配置, 则返回空Set.
	 */
	public Set<ConfigurationClass> getImportedBy() {
		return this.importedBy;
	}

	public void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	public Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	public void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	public void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
		this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
	}

	public Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> getImportBeanDefinitionRegistrars() {
		return this.importBeanDefinitionRegistrars;
	}

	public Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	public void validate(ProblemReporter problemReporter) {
		// 配置类可能不是 final的 (CGLIB 限制)
		if (getMetadata().isAnnotated(Configuration.class.getName())) {
			if (getMetadata().isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
		}

		for (BeanMethod beanMethod : this.beanMethods) {
			beanMethod.validate(problemReporter);
		}
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof ConfigurationClass &&
				getMetadata().getClassName().equals(((ConfigurationClass) other).getMetadata().getClassName())));
	}

	@Override
	public int hashCode() {
		return getMetadata().getClassName().hashCode();
	}

	@Override
	public String toString() {
		return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
	}


	/**
	 * 配置类必须是非final的, 以适应CGLIB子类化.
	 */
	private class FinalConfigurationProblem extends Problem {

		public FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}

}
