package org.springframework.core.type.filter;

import java.io.IOException;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * 使用{@link org.springframework.core.type.classreading.MetadataReader}的类型过滤器的基础接口.
 */
public interface TypeFilter {

	/**
	 * 确定此过滤器是否与给定元数据描述的类匹配.
	 * 
	 * @param metadataReader 目标类的元数据读取器
	 * @param metadataReaderFactory 用于获取其他类(例如超类和接口)的元数据读取器的工厂
	 * 
	 * @return 此过滤器是否匹配
	 * @throws IOException 在读取元数据时发生 I/O 错误
	 */
	boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException;

}
