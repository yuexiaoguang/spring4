package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * 在解析过程中已处理的导入.
 */
public class ImportDefinition implements BeanMetadataElement {

	private final String importedResource;

	private final Resource[] actualResources;

	private final Object source;


	/**
	 * @param importedResource 导入的资源的位置
	 */
	public ImportDefinition(String importedResource) {
		this(importedResource, null, null);
	}

	/**
	 * @param importedResource 导入的资源的位置
	 * @param source 源对象 (may be {@code null})
	 */
	public ImportDefinition(String importedResource, Object source) {
		this(importedResource, null, source);
	}

	/**
	 * @param importedResource 导入的资源的位置
	 * @param source 源对象 (may be {@code null})
	 */
	public ImportDefinition(String importedResource, Resource[] actualResources, Object source) {
		Assert.notNull(importedResource, "Imported resource must not be null");
		this.importedResource = importedResource;
		this.actualResources = actualResources;
		this.source = source;
	}


	/**
	 * 返回导入的资源的位置.
	 */
	public final String getImportedResource() {
		return this.importedResource;
	}

	public final Resource[] getActualResources() {
		return this.actualResources;
	}

	@Override
	public final Object getSource() {
		return this.source;
	}

}
