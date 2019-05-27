package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.support.GenericBeanDefinition}类的扩展,
 * 基于 ASM ClassReader, 支持通过{@link AnnotatedBeanDefinition}接口公开的注解元数据.
 *
 * <p>这个类没有实时加载bean {@code Class}.
 * 它更确切地从".class"文件中检索所有相关元数据, 并使用ASM ClassReader进行解析.
 * 功能上等同于
 * {@link AnnotatedGenericBeanDefinition#AnnotatedGenericBeanDefinition(AnnotationMetadata)},
 * 但是对通过已扫描的类型bean, 与已通过其他方式注册或检测的类型bean进行区分.
 */
@SuppressWarnings("serial")
public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

	private final AnnotationMetadata metadata;


	/**
	 * 为给定MetadataReader描述的类创建新的ScannedGenericBeanDefinition.
	 * 
	 * @param metadataReader 扫描到的目标类的MetadataReader
	 */
	public ScannedGenericBeanDefinition(MetadataReader metadataReader) {
		Assert.notNull(metadataReader, "MetadataReader must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		setBeanClassName(this.metadata.getClassName());
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public MethodMetadata getFactoryMethodMetadata() {
		return null;
	}

}
