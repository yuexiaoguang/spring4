package org.springframework.core.type.filter;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

/**
 * 一个简单的过滤器, 它匹配具有给定注解的类, 同时检查继承的注解.
 *
 * <p>匹配逻辑镜像{@link java.lang.Class#isAnnotationPresent(Class)}.
 */
public class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<? extends Annotation> annotationType;

	private final boolean considerMetaAnnotations;


	/**
	 * 为给定的注解类型创建一个新的AnnotationTypeFilter.
	 * 此过滤器还将匹配元注解.
	 * 要禁用元注解匹配, 使用接受 '{@code considerMetaAnnotations}'参数的构造函数.
	 * 过滤器不匹配接口.
	 * 
	 * @param annotationType 要匹配的注解类型
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, true, false);
	}

	/**
	 * 为给定的注解类型创建一个新的AnnotationTypeFilter.
	 * 过滤器不匹配接口.
	 * 
	 * @param annotationType 要匹配的注解类型
	 * @param considerMetaAnnotations 是否也匹配元注解
	 */
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
		this(annotationType, considerMetaAnnotations, false);
	}

	/**
	 * 为给定的注解类型创建一个新的{@link AnnotationTypeFilter}.
	 * 
	 * @param annotationType 要匹配的注解类型
	 * @param considerMetaAnnotations 是否也匹配元注解
	 * @param considerInterfaces 是否也匹配接口
	 */
	public AnnotationTypeFilter(
			Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {

		super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
		this.annotationType = annotationType;
		this.considerMetaAnnotations = considerMetaAnnotations;
	}


	@Override
	protected boolean matchSelf(MetadataReader metadataReader) {
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
		return metadata.hasAnnotation(this.annotationType.getName()) ||
				(this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
	}

	@Override
	protected Boolean matchSuperClass(String superClassName) {
		return hasAnnotation(superClassName);
	}

	@Override
	protected Boolean matchInterface(String interfaceName) {
		return hasAnnotation(interfaceName);
	}

	protected Boolean hasAnnotation(String typeName) {
		if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			if (!this.annotationType.getName().startsWith("java")) {
				// 标准Java类型没有非标准注解 -> 跳过任何加载尝试, 特别是对于Java语言接口.
				return false;
			}
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				return ((this.considerMetaAnnotations ? AnnotationUtils.getAnnotation(clazz, this.annotationType) :
						clazz.getAnnotation(this.annotationType)) != null);
			}
			catch (Throwable ex) {
				// 类不可定期加载 - 无法确定匹配方式.
			}
		}
		return null;
	}

}
