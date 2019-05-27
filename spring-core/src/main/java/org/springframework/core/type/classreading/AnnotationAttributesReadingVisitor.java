package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * ASM访问器, 查找在类或方法上定义的注解, 包括元注解.
 *
 * <p>此访问者是完全递归的, 考虑了任何嵌套注解或嵌套注解数组.
 */
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

	private final MultiValueMap<String, AnnotationAttributes> attributesMap;

	private final Map<String, Set<String>> metaAnnotationMap;


	public AnnotationAttributesReadingVisitor(String annotationType,
			MultiValueMap<String, AnnotationAttributes> attributesMap, Map<String, Set<String>> metaAnnotationMap,
			ClassLoader classLoader) {

		super(annotationType, new AnnotationAttributes(annotationType, classLoader), classLoader);
		this.attributesMap = attributesMap;
		this.metaAnnotationMap = metaAnnotationMap;
	}


	@Override
	public void visitEnd() {
		super.visitEnd();

		Class<? extends Annotation> annotationClass = this.attributes.annotationType();
		if (annotationClass != null) {
			List<AnnotationAttributes> attributeList = this.attributesMap.get(this.annotationType);
			if (attributeList == null) {
				this.attributesMap.add(this.annotationType, this.attributes);
			}
			else {
				attributeList.add(0, this.attributes);
			}
			if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationClass.getName())) {
				Set<Annotation> visited = new LinkedHashSet<Annotation>();
				Annotation[] metaAnnotations = AnnotationUtils.getAnnotations(annotationClass);
				if (!ObjectUtils.isEmpty(metaAnnotations)) {
					for (Annotation metaAnnotation : metaAnnotations) {
						recursivelyCollectMetaAnnotations(visited, metaAnnotation);
					}
				}
				if (this.metaAnnotationMap != null) {
					Set<String> metaAnnotationTypeNames = new LinkedHashSet<String>(visited.size());
					for (Annotation ann : visited) {
						metaAnnotationTypeNames.add(ann.annotationType().getName());
					}
					this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
				}
			}
		}
	}

	private void recursivelyCollectMetaAnnotations(Set<Annotation> visited, Annotation annotation) {
		Class<? extends Annotation> annotationType = annotation.annotationType();
		String annotationName = annotationType.getName();
		if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) && visited.add(annotation)) {
			try {
				// 仅对公共注解进行属性扫描; 否则会遇到IllegalAccessException, 不想在SecurityManager环境中搞乱可访问性.
				if (Modifier.isPublic(annotationType.getModifiers())) {
					this.attributesMap.add(annotationName,
							AnnotationUtils.getAnnotationAttributes(annotation, false, true));
				}
				for (Annotation metaMetaAnnotation : annotationType.getAnnotations()) {
					recursivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
				}
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to introspect meta-annotations on " + annotation + ": " + ex);
				}
			}
		}
	}

}
