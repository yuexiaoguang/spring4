package org.springframework.oxm.xmlbeans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xmlbeans.XMLStreamValidationException;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.XmlValidationError;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.xml.StaxUtils;

/**
 * Apache XMLBeans的{@link Marshaller}接口的实现.
 *
 * <p>可以通过设置{@code xmlOptions}属性来设置选项.
 * 提供{@link XmlOptionsFactoryBean}以轻松设置{@link XmlOptions}实例.
 *
 * <p>可以通过设置{@code validating}属性或直接调用{@link #validate(XmlObject)}方法来验证未编组的对象.
 * 无效的对象将导致{@link ValidationFailureException}.
 *
 * <p><b>NOTE:</b> 由于XMLBeans的性质, 此编组器要求所有传递的对象都是{@link XmlObject}类型.
 *
 * @deprecated as of Spring 4.2, following the XMLBeans retirement at Apache
 */
@Deprecated
public class XmlBeansMarshaller extends AbstractMarshaller {

	private XmlOptions xmlOptions;

	private boolean validating = false;


	/**
	 * Set the {@code XmlOptions}.
	 */
	public void setXmlOptions(XmlOptions xmlOptions) {
		this.xmlOptions = xmlOptions;
	}

	/**
	 * Return the {@code XmlOptions}.
	 */
	public XmlOptions getXmlOptions() {
		return this.xmlOptions;
	}

	/**
	 * 设置此编组器是否应验证输入和输出文档.
	 * Default is {@code false}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	/**
	 * 返回此编组器是否应验证输入和输出文档.
	 */
	public boolean isValidating() {
		return this.validating;
	}


	/**
	 * 如果给定的类是{@link XmlObject}的实现, 则此实现返回true.
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return XmlObject.class.isAssignableFrom(clazz);
	}


	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		Document document = (node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node : node.getOwnerDocument());
		Node xmlBeansNode = ((XmlObject) graph).newDomNode(getXmlOptions());
		NodeList xmlBeansChildNodes = xmlBeansNode.getChildNodes();
		for (int i = 0; i < xmlBeansChildNodes.getLength(); i++) {
			Node xmlBeansChildNode = xmlBeansChildNodes.item(i);
			Node importedNode = document.importNode(xmlBeansChildNode, true);
			node.appendChild(importedNode);
		}
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) {
		ContentHandler contentHandler = StaxUtils.createContentHandler(eventWriter);
		LexicalHandler lexicalHandler = null;
		if (contentHandler instanceof LexicalHandler) {
			lexicalHandler = (LexicalHandler) contentHandler;
		}
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		ContentHandler contentHandler = StaxUtils.createContentHandler(streamWriter);
		LexicalHandler lexicalHandler = null;
		if (contentHandler instanceof LexicalHandler) {
			lexicalHandler = (LexicalHandler) contentHandler;
		}
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException {
		try {
			((XmlObject) graph).save(contentHandler, lexicalHandler, getXmlOptions());
		}
		catch (SAXException ex) {
			throw convertXmlBeansException(ex, true);
		}
	}

	@Override
	protected void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException {

		((XmlObject) graph).save(outputStream, getXmlOptions());
	}

	@Override
	protected void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		((XmlObject) graph).save(writer, getXmlOptions());
	}


	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		try {
			XmlObject object = XmlObject.Factory.parse(node, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
		XMLReader reader = StaxUtils.createXMLReader(eventReader);
		try {
			return unmarshalSaxReader(reader, new InputSource());
		}
		catch (IOException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
		try {
			XmlObject object = XmlObject.Factory.parse(streamReader, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		XmlSaxHandler saxHandler = XmlObject.Factory.newXmlSaxHandler(getXmlOptions());
		xmlReader.setContentHandler(saxHandler.getContentHandler());
		try {
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", saxHandler.getLexicalHandler());
		}
		catch (SAXNotRecognizedException ex) {
			// ignore
		}
		catch (SAXNotSupportedException ex) {
			// ignore
		}
		try {
			xmlReader.parse(inputSource);
			XmlObject object = saxHandler.getObject();
			validate(object);
			return object;
		}
		catch (SAXException ex) {
			throw convertXmlBeansException(ex, false);
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		try {
			InputStream nonClosingInputStream = new NonClosingInputStream(inputStream);
			XmlObject object = XmlObject.Factory.parse(nonClosingInputStream, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}

	@Override
	protected Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		try {
			Reader nonClosingReader = new NonClosingReader(reader);
			XmlObject object = XmlObject.Factory.parse(nonClosingReader, getXmlOptions());
			validate(object);
			return object;
		}
		catch (XmlException ex) {
			throw convertXmlBeansException(ex, false);
		}
	}


	/**
	 * 验证给定的{@code XmlObject}.
	 * 
	 * @param object 要验证的xml对象
	 * 
	 * @throws ValidationFailureException 如果给定的对象无效
	 */
	protected void validate(XmlObject object) throws ValidationFailureException {
		if (isValidating() && object != null) {
			XmlOptions validateOptions = getXmlOptions();
			if (validateOptions == null) {
				// 创建临时XmlOptions仅用于验证
				validateOptions = new XmlOptions();
			}
			List<XmlError> errorsList = new ArrayList<XmlError>();
			validateOptions.setErrorListener(errorsList);
			if (!object.validate(validateOptions)) {
				StringBuilder sb = new StringBuilder("Failed to validate XmlObject: ");
				boolean first = true;
				for (XmlError error : errorsList) {
					if (error instanceof XmlValidationError) {
						if (!first) {
							sb.append("; ");
						}
						sb.append(error.toString());
						first = false;
					}
				}
				throw new ValidationFailureException("XMLBeans validation failure",
						new XmlException(sb.toString(), null, errorsList));
			}
		}
	}

	/**
	 * 将给定的XMLBeans异常转换为{@code org.springframework.oxm}层次结构中的适当异常.
	 * <p>布尔标志用于指示在编组或解组期间是否发生此异常, 因为XMLBeans本身不会在其异常层次结构中进行此区分.
	 * 
	 * @param ex 发生XMLBeans异常
	 * @param marshalling 指示在编组({@code true}), 或解组 ({@code false})期间是否发生异常
	 * 
	 * @return 相应的{@code XmlMappingException}
	 */
	protected XmlMappingException convertXmlBeansException(Exception ex, boolean marshalling) {
		if (ex instanceof XMLStreamValidationException) {
			return new ValidationFailureException("XMLBeans validation exception", ex);
		}
		else if (ex instanceof XmlException || ex instanceof SAXException) {
			if (marshalling) {
				return new MarshallingFailureException("XMLBeans marshalling exception",  ex);
			}
			else {
				return new UnmarshallingFailureException("XMLBeans unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XMLBeans exception", ex);
		}
	}


	private static class NonClosingInputStream extends InputStream {

		private final WeakReference<InputStream> in;

		public NonClosingInputStream(InputStream in) {
			this.in = new WeakReference<InputStream>(in);
		}

		private InputStream getInputStream() {
			return this.in.get();
		}

		@Override
		public int read() throws IOException {
			InputStream in = getInputStream();
			return (in != null ? in.read() : -1);
		}

		@Override
		public int read(byte[] b) throws IOException {
			InputStream in = getInputStream();
			return (in != null ? in.read(b) : -1);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			InputStream in = getInputStream();
			return (in != null ? in.read(b, off, len) : -1);
		}

		@Override
		public long skip(long n) throws IOException {
			InputStream in = getInputStream();
			return (in != null ? in.skip(n) : 0);
		}

		@Override
		public boolean markSupported() {
			InputStream in = getInputStream();
			return (in != null && in.markSupported());
		}

		@Override
		public void mark(int readlimit) {
			InputStream in = getInputStream();
			if (in != null) {
				in.mark(readlimit);
			}
		}

		@Override
		public void reset() throws IOException {
			InputStream in = getInputStream();
			if (in != null) {
				in.reset();
			}
		}

		@Override
		public int available() throws IOException {
			InputStream in = getInputStream();
			return (in != null ? in.available() : 0);
		}

		@Override
		public void close() throws IOException {
			InputStream in = getInputStream();
			if (in != null) {
				this.in.clear();
			}
		}
	}


	private static class NonClosingReader extends Reader {

		private final WeakReference<Reader> reader;

		public NonClosingReader(Reader reader) {
			this.reader = new WeakReference<Reader>(reader);
		}

		private Reader getReader() {
			return this.reader.get();
		}

		@Override
		public int read(CharBuffer target) throws IOException {
			Reader rdr = getReader();
			return (rdr != null ? rdr.read(target) : -1);
		}

		@Override
		public int read() throws IOException {
			Reader rdr = getReader();
			return (rdr != null ? rdr.read() : -1);
		}

		@Override
		public int read(char[] cbuf) throws IOException {
			Reader rdr = getReader();
			return (rdr != null ? rdr.read(cbuf) : -1);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			Reader rdr = getReader();
			return (rdr != null ? rdr.read(cbuf, off, len) : -1);
		}

		@Override
		public long skip(long n) throws IOException {
			Reader rdr = getReader();
			return (rdr != null ? rdr.skip(n) : 0);
		}

		@Override
		public boolean ready() throws IOException {
			Reader rdr = getReader();
			return (rdr != null && rdr.ready());
		}

		@Override
		public boolean markSupported() {
			Reader rdr = getReader();
			return (rdr != null && rdr.markSupported());
		}

		@Override
		public void mark(int readAheadLimit) throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				rdr.mark(readAheadLimit);
			}
		}

		@Override
		public void reset() throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				rdr.reset();
			}
		}

		@Override
		public void close() throws IOException {
			Reader rdr = getReader();
			if (rdr != null) {
				this.reader.clear();
			}
		}
	}

}
