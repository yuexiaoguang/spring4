package org.springframework.util.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * SAX {@code XMLReader}实现的抽象基类.
 * 包含{@link XMLReader}中定义的属性, 但不识别任何功能.
 */
abstract class AbstractXMLReader implements XMLReader {

	private DTDHandler dtdHandler;

	private ContentHandler contentHandler;

	private EntityResolver entityResolver;

	private ErrorHandler errorHandler;

	private LexicalHandler lexicalHandler;


	@Override
	public void setContentHandler(ContentHandler contentHandler) {
		this.contentHandler = contentHandler;
	}

	@Override
	public ContentHandler getContentHandler() {
		return this.contentHandler;
	}

	@Override
	public void setDTDHandler(DTDHandler dtdHandler) {
		this.dtdHandler = dtdHandler;
	}

	@Override
	public DTDHandler getDTDHandler() {
		return this.dtdHandler;
	}

	@Override
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	@Override
	public EntityResolver getEntityResolver() {
		return this.entityResolver;
	}

	@Override
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	protected LexicalHandler getLexicalHandler() {
		return this.lexicalHandler;
	}


	/**
	 * 此实现为"http://xml.org/sax/features/"命名空间之外的任何功能抛出{@code SAXNotRecognizedException}异常,
	 * 并为其中的任何功能返回{@code false}.
	 */
	@Override
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (name.startsWith("http://xml.org/sax/features/")) {
			return false;
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}

	/**
	 * 此实现为"http://xml.org/sax/features/"命名空间之外的任何功能抛出{@code SAXNotRecognizedException}异常,
	 * 并为其中的任何功能接受{@code false}值.
	 */
	@Override
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (name.startsWith("http://xml.org/sax/features/")) {
			if (value) {
				throw new SAXNotSupportedException(name);
			}
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}

	/**
	 * 当给定属性不表示词法处理器时, 引发{@code SAXNotRecognizedException}异常.
	 * 词法处理器的属性名是{@code http://xml.org/sax/properties/lexical-handler}.
	 */
	@Override
	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			return this.lexicalHandler;
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}

	/**
	 * 当给定属性不表示词法处理器时, 引发{@code SAXNotRecognizedException}异常.
	 * 词法处理器的属性名是{@code http://xml.org/sax/properties/lexical-handler}.
	 */
	@Override
	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
			this.lexicalHandler = (LexicalHandler) value;
		}
		else {
			throw new SAXNotRecognizedException(name);
		}
	}

}
