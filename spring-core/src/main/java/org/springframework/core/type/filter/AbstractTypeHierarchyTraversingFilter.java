package org.springframework.core.type.filter;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * 遍历层次结构的过滤器.
 *
 * <p>当需要基于整个类/接口层次结构进行匹配时, 此过滤器非常有用.
 * 所采用的算法使用成功快速策略: 如果在任何时候声明匹配, 则不执行进一步处理.
 */
public abstract class AbstractTypeHierarchyTraversingFilter implements TypeFilter {

	protected final Log logger = LogFactory.getLog(getClass());

	private final boolean considerInherited;

	private final boolean considerInterfaces;


	protected AbstractTypeHierarchyTraversingFilter(boolean considerInherited, boolean considerInterfaces) {
		this.considerInherited = considerInherited;
		this.considerInterfaces = considerInterfaces;
	}


	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		// 此方法优化了避免不必要的ClassReader创建以及访问这些读取器.
		if (matchSelf(metadataReader)) {
			return true;
		}
		ClassMetadata metadata = metadataReader.getClassMetadata();
		if (matchClassName(metadata.getClassName())) {
			return true;
		}

		if (this.considerInherited) {
			if (metadata.hasSuperClass()) {
				// 优化以避免为超类创建ClassReader.
				Boolean superClassMatch = matchSuperClass(metadata.getSuperClassName());
				if (superClassMatch != null) {
					if (superClassMatch.booleanValue()) {
						return true;
					}
				}
				else {
					// 需要读取超类来确定匹配...
					try {
						if (match(metadata.getSuperClassName(), metadataReaderFactory)) {
							return true;
						}
					}
					catch (IOException ex) {
						logger.debug("Could not read super class [" + metadata.getSuperClassName() +
								"] of type-filtered class [" + metadata.getClassName() + "]");
					}
 				}
			}
		}

		if (this.considerInterfaces) {
			for (String ifc : metadata.getInterfaceNames()) {
				// 优化以避免为超类创建ClassReader
				Boolean interfaceMatch = matchInterface(ifc);
				if (interfaceMatch != null) {
					if (interfaceMatch.booleanValue()) {
						return true;
					}
				}
				else {
					// 需要读取接口来确定匹配...
					try {
						if (match(ifc, metadataReaderFactory)) {
							return true;
						}
					}
					catch (IOException ex) {
						logger.debug("Could not read interface [" + ifc + "] for type-filtered class [" +
								metadata.getClassName() + "]");
					}
				}
			}
		}

		return false;
	}

	private boolean match(String className, MetadataReaderFactory metadataReaderFactory) throws IOException {
		return match(metadataReaderFactory.getMetadataReader(className), metadataReaderFactory);
	}

	/**
	 * 覆盖它以仅匹配自身特征.
	 * 通常, 实现将使用访问器提取信息以执行匹配.
	 */
	protected boolean matchSelf(MetadataReader metadataReader) {
		return false;
	}

	/**
	 * 覆盖此项以匹配类型名称.
	 */
	protected boolean matchClassName(String className) {
		return false;
	}

	/**
	 * 覆盖此项以匹配超类型名称.
	 */
	protected Boolean matchSuperClass(String superClassName) {
		return null;
	}

	/**
	 * 覆盖它以匹配接口类型名称.
	 */
	protected Boolean matchInterface(String interfaceName) {
		return null;
	}

}
