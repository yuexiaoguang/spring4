package org.springframework.beans.factory.xml;

/**
 * 由{@link org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader}
 * 用于查找特定命名空间URI的 {@link NamespaceHandler}实现.
 */
public interface NamespaceHandlerResolver {

	/**
	 * 解析命名空间URI, 并返回定位的{@link NamespaceHandler}实现.
	 * 
	 * @param namespaceUri 相关的命名空间URI
	 * 
	 * @return 定位的 {@link NamespaceHandler} (may be {@code null})
	 */
	NamespaceHandler resolve(String namespaceUri);

}
