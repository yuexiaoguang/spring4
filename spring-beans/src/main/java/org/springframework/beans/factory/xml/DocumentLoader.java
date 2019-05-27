package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * 用于加载XML {@link Document}的策略接口.
 */
public interface DocumentLoader {

	/**
	 * 从提供的{@link InputSource source}加载 {@link Document document}.
	 * 
	 * @param inputSource 要加载的文档的源
	 * @param entityResolver 用于解析实体的解析器
	 * @param errorHandler 用于报告文档加载过程中的任何错误
	 * @param validationMode 验证的类型
	 * {@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_DTD DTD}
	 * or {@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware {@code true}是否要提供对XML命名空间的支持
	 * 
	 * @return 加载的{@link Document document}
	 * @throws Exception 发生错误
	 */
	Document loadDocument(
			InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception;

}
