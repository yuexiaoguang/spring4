package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * 用于访问类元数据的简单外观, 由ASM {@link org.springframework.asm.ClassReader}读取.
 */
public interface MetadataReader {

	/**
	 * 返回类文件的资源引用.
	 */
	Resource getResource();

	/**
	 * 读取底层类的基础类元数据.
	 */
	ClassMetadata getClassMetadata();

	/**
	 * 读取底层类的完整注解元数据, 包括带注解的方法的元数据.
	 */
	AnnotationMetadata getAnnotationMetadata();

}
