package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * {@link MetadataReader}实例的工厂接口.
 * 允许按原始资源缓存MetadataReader.
 */
public interface MetadataReaderFactory {

	/**
	 * 获取给定类名的MetadataReader.
	 * 
	 * @param className 类名 (被解析为".class"文件)
	 * 
	 * @return ClassReader实例的持有者 (never {@code null})
	 * @throws IOException 发生I/O错误
	 */
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * 获取给定资源的MetadataReader.
	 * 
	 * @param resource 资源 (指向".class"文件)
	 * 
	 * @return ClassReader实例的持有者 (never {@code null})
	 * @throws IOException 发生 I/O错误
	 */
	MetadataReader getMetadataReader(Resource resource) throws IOException;

}
