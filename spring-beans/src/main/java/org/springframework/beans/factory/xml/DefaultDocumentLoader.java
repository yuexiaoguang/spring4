package org.springframework.beans.factory.xml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * Spring的默认{@link DocumentLoader}实现.
 *
 * <p>只需使用标准的JAXP配置的XML解析器加载{@link Document documents}.
 * 如果要更改用于加载文档的 {@link DocumentBuilder}, 一种策略是在启动JVM时定义相应的Java系统属性.
 * 例如, 要使用Oracle {@link DocumentBuilder}, 您可以按如下方式启动应用程序:
 *
 * <pre code="class">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * JAXP属性用于配置模式语言, 以进行验证.
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * JAXP属性值, 指示XSD架构语言.
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	private static final Log logger = LogFactory.getLog(DefaultDocumentLoader.class);


	/**
	 * 使用标准的JAXP配置的XML解析器, 在提供的{@link InputSource}上加载{@link Document}.
	 */
	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {

		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isDebugEnabled()) {
			logger.debug("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
	}

	/**
	 * 创建{@link DocumentBuilderFactory}实例.
	 * 
	 * @param validationMode 验证的类型:
	 * {@link XmlValidationModeDetector#VALIDATION_DTD DTD} {@link XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware 返回的工厂是否为XML命名空间提供支持
	 * 
	 * @return the JAXP DocumentBuilderFactory
	 * @throws ParserConfigurationException 如果无法构建合适的DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			factory.setValidating(true);
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// 为XSD实施名称空间感知...
				factory.setNamespaceAware(true);
				try {
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}

	/**
	 * 创建一个JAXP DocumentBuilder, 该bean定义读取器将用于解析XML文档.
	 * 可以在子类中重写, 添加构建器的进一步初始化.
	 * 
	 * @param factory 创建DocumentBuilder的JAXP DocumentBuilderFactory
	 * @param entityResolver 要使用的SAX EntityResolver
	 * @param errorHandler 要使用的SAX ErrorHandler
	 * 
	 * @return the JAXP DocumentBuilder
	 * @throws ParserConfigurationException 如果由JAXP方法抛出
	 */
	protected DocumentBuilder createDocumentBuilder(
			DocumentBuilderFactory factory, EntityResolver entityResolver, ErrorHandler errorHandler)
			throws ParserConfigurationException {

		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
