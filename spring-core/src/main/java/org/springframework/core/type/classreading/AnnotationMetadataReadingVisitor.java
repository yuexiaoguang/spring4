package org.springframework.core.type.classreading;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * ASM类访问器, 它查找类名, 实现类型以及在类上定义的注解,
 * 通过{@link org.springframework.core.type.AnnotationMetadata}接口公开它们.
 */
public class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

	protected final ClassLoader classLoader;

	protected final Set<String> annotationSet = new LinkedHashSet<String>(4);

	protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<String, Set<String>>(4);

	/**
	 * 声明为{@link LinkedMultiValueMap}, 而不是{@link MultiValueMap}, 以确保保留条目的层次排序.
	 */
	protected final LinkedMultiValueMap<String, AnnotationAttributes> attributesMap =
			new LinkedMultiValueMap<String, AnnotationAttributes>(4);

	protected final Set<MethodMetadata> methodMetadataSet = new LinkedHashSet<MethodMetadata>(4);


	public AnnotationMetadataReadingVisitor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// 跳过桥接方法 - 只对原始注解定义用户方法感兴趣.
		// 在JDK 8上, 否则会对同一个带注解的方法进行双重检测...
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		return new MethodMetadataReadingVisitor(name, access, getClassName(),
				Type.getReturnType(desc).getClassName(), this.classLoader, this.methodMetadataSet);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		String className = Type.getType(desc).getClassName();
		this.annotationSet.add(className);
		return new AnnotationAttributesReadingVisitor(
				className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
	}


	@Override
	public Set<String> getAnnotationTypes() {
		return this.annotationSet;
	}

	@Override
	public Set<String> getMetaAnnotationTypes(String annotationName) {
		return this.metaAnnotationMap.get(annotationName);
	}

	@Override
	public boolean hasAnnotation(String annotationName) {
		return this.annotationSet.contains(annotationName);
	}

	@Override
	public boolean hasMetaAnnotation(String metaAnnotationType) {
		Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
		for (Set<String> metaTypes : allMetaTypes) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAnnotated(String annotationName) {
		return (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) &&
				this.attributesMap.containsKey(annotationName));
	}

	@Override
	public AnnotationAttributes getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	@Override
	public AnnotationAttributes getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		AnnotationAttributes raw = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(
				this.attributesMap, this.metaAnnotationMap, annotationName);
		return AnnotationReadingVisitorUtils.convertClassValues(
				"class '" + getClassName() + "'", this.classLoader, raw, classValuesAsString);
	}

	@Override
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	@Override
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
		MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<String, Object>();
		List<AnnotationAttributes> attributes = this.attributesMap.get(annotationName);
		if (attributes == null) {
			return null;
		}
		for (AnnotationAttributes raw : attributes) {
			for (Map.Entry<String, Object> entry : AnnotationReadingVisitorUtils.convertClassValues(
					"class '" + getClassName() + "'", this.classLoader, raw, classValuesAsString).entrySet()) {
				allAttributes.add(entry.getKey(), entry.getValue());
			}
		}
		return allAttributes;
	}

	@Override
	public boolean hasAnnotatedMethods(String annotationName) {
		for (MethodMetadata methodMetadata : this.methodMetadataSet) {
			if (methodMetadata.isAnnotated(annotationName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>(4);
		for (MethodMetadata methodMetadata : this.methodMetadataSet) {
			if (methodMetadata.isAnnotated(annotationName)) {
				annotatedMethods.add(methodMetadata);
			}
		}
		return annotatedMethods;
	}
}
