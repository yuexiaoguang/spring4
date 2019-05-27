package org.springframework.core.type.filter;

import java.io.IOException;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * 类型过滤器, 将{@link org.springframework.core.type.ClassMetadata}对象公开给子类, 用于类测试.
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

	@Override
	public final boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		return match(metadataReader.getClassMetadata());
	}

	/**
	 * 根据给定的ClassMetadata对象确定匹配项.
	 * 
	 * @param metadata ClassMetadata对象
	 * 
	 * @return 此过滤器是否与指定类型匹配
	 */
	protected abstract boolean match(ClassMetadata metadata);

}
