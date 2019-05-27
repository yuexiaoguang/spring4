package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * SPI用于解析包含Spring bean定义的XML文档.
 * 由{@link XmlBeanDefinitionReader}用于实际解析DOM文档.
 *
 * <p>实例化每个要解析的文档:
 * 实现可以在执行 {@code registerBeanDefinitions} method &mdash期间保存实例变量中的状态;
 * 例如, 为文档中的所有bean定义定义的全局设置.
 */
public interface BeanDefinitionDocumentReader {

	/**
	 * 从给定的DOM文档中读取bean定义, 并在给定的读取器上下文中将其注册到注册表中.
	 * 
	 * @param doc DOM文档
	 * @param readerContext 读取器的当前上下文 (包括目标注册表和正在解析的资源)
	 * 
	 * @throws BeanDefinitionStoreException 解析错误
	 */
	void registerBeanDefinitions(Document doc, XmlReaderContext readerContext)
			throws BeanDefinitionStoreException;

}
