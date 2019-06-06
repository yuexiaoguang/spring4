package org.springframework.oxm.support;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * 使用{@link Marshaller}的{@link Source}实现. 可以使用{@code Marshaller}和要编组的对象构建.
 *
 * <p>即使{@code MarshallingSource}从{@code SAXSource}扩展, 调用{@code SAXSource}的方法<strong>也不支持</strong>.
 * 通常，此类唯一支持的操作是使用通过{@link #getXMLReader()}获得的{@code XMLReader},
 * 解析通过{@link #getInputSource()}获得的输入源.
 * 调用{@link #setXMLReader(XMLReader)}或{@link #setInputSource(InputSource)}将导致{@code UnsupportedOperationException}.
 */
public class MarshallingSource extends SAXSource {

	private final Marshaller marshaller;

	private final Object content;


	/**
	 * @param marshaller 要使用的编组器
	 * @param content 要编组的对象
	 */
	public MarshallingSource(Marshaller marshaller, Object content) {
		super(new MarshallingXMLReader(marshaller, content), new InputSource());
		Assert.notNull(marshaller, "'marshaller' must not be null");
		Assert.notNull(content, "'content' must not be null");
		this.marshaller = marshaller;
		this.content = content;
	}


	/**
	 * 返回此{@code MarshallingSource}使用的{@code Marshaller}.
	 */
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * 返回要编组的对象.
	 */
	public Object getContent() {
		return this.content;
	}

	/**
	 * 抛出{@code UnsupportedOperationException}.
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * 抛出{@code UnsupportedOperationException}.
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}


	private static class MarshallingXMLReader implements XMLReader {

		private final Marshaller marshaller;

		private final Object content;

		private DTDHandler dtdHandler;

		private ContentHandler contentHandler;

		private EntityResolver entityResolver;

		private ErrorHandler errorHandler;

		private LexicalHandler lexicalHandler;

		private MarshallingXMLReader(Marshaller marshaller, Object content) {
			Assert.notNull(marshaller, "'marshaller' must not be null");
			Assert.notNull(content, "'content' must not be null");
			this.marshaller = marshaller;
			this.content = content;
		}

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

		@Override
		public boolean getFeature(String name) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		@Override
		public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		@Override
		public Object getProperty(String name) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				return lexicalHandler;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		@Override
		public void setProperty(String name, Object value) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				this.lexicalHandler = (LexicalHandler) value;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		@Override
		public void parse(InputSource input) throws SAXException {
			parse();
		}

		@Override
		public void parse(String systemId) throws SAXException {
			parse();
		}

		private void parse() throws SAXException {
			SAXResult result = new SAXResult(getContentHandler());
			result.setLexicalHandler(getLexicalHandler());
			try {
				this.marshaller.marshal(this.content, result);
			}
			catch (IOException ex) {
				SAXParseException saxException = new SAXParseException(ex.getMessage(), null, null, -1, -1, ex);
				ErrorHandler errorHandler = getErrorHandler();
				if (errorHandler != null) {
					errorHandler.fatalError(saxException);
				}
				else {
					throw saxException;
				}
			}
		}
	}

}
