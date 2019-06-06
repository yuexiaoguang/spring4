package org.springframework.oxm.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.xml.StaxUtils;

/**
 * {@code Marshaller}和{@code Unmarshaller}接口的抽象实现.
 * 此实现检查给定的{@code Source}或{@code Result}, 并将进一步处理委托给可覆盖的模板方法.
 */
public abstract class AbstractMarshaller implements Marshaller, Unmarshaller {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;

	private DocumentBuilderFactory documentBuilderFactory;

	private final Object documentBuilderFactoryMonitor = new Object();


	/**
	 * 指示是否应支持DTD解析.
	 * <p>默认值为{@code false}, 表示DTD已禁用.
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
	}

	/**
	 * 是否支持DTD解析.
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * 指示解组时是否处理外部XML实体.
	 * <p>默认{@code false}, 表示未解析外部实体.
	 * 请注意, 只有在传递给{@link #unmarshal(Source)}的{@code Source}为{@link SAXSource}或{@link StreamSource}时,
	 * 才会启用/禁用外部实体的处理.
	 * 它对{@link DOMSource}或{@link StAXSource}实例没有影响.
	 * <p><strong>Note:</strong> 将此选项设置为{@code true}也会自动将{@link #setSupportDtd}设置为{@code true}.
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			setSupportDtd(true);
		}
	}

	/**
	 * 返回是否允许XML外部实体的配置值.
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	/**
	 * 从此编组器的{@link DocumentBuilderFactory}构建一个新的{@link Document}, 作为DOM节点的占位符.
	 */
	protected Document buildDocument() {
		try {
			DocumentBuilder documentBuilder;
			synchronized (this.documentBuilderFactoryMonitor) {
				if (this.documentBuilderFactory == null) {
					this.documentBuilderFactory = createDocumentBuilderFactory();
				}
				documentBuilder = createDocumentBuilder(this.documentBuilderFactory);
			}
			return documentBuilder.newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new UnmarshallingFailureException("Could not create document placeholder: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 创建一个{@code DocumentBuilder}, 这个编组器在传递空的{@code DOMSource}时, 将用于创建DOM文档.
	 * <p>生成的{@code DocumentBuilderFactory}被缓存, 因此此方法只会被调用一次.
	 * 
	 * @return the DocumentBuilderFactory
	 * @throws ParserConfigurationException 如果由JAXP方法抛出
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
		factory.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
		return factory;
	}

	/**
	 * 创建一个{@code DocumentBuilder}, 这个编组器在传递空的{@code DOMSource}时, 将用于创建DOM文档.
	 * <p>可以在子类中重写, 添加构建器的进一步初始化.
	 * 
	 * @param factory 创建DocumentBuilder的{@code DocumentBuilderFactory}
	 * 
	 * @return the {@code DocumentBuilder}
	 * @throws ParserConfigurationException 如果由JAXP方法抛出
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory)
			throws ParserConfigurationException {

		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		if (!isProcessExternalEntities()) {
			documentBuilder.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return documentBuilder;
	}

	/**
	 * 创建一个{@code XMLReader}, 当这个编组器传递一个空的{@code SAXSource}时.
	 * 
	 * @return the XMLReader
	 * @throws SAXException 如果由JAXP方法抛出
	 */
	protected XMLReader createXmlReader() throws SAXException {
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
		xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
		if (!isProcessExternalEntities()) {
			xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return xmlReader;
	}

	/**
	 * 确定用于从字节流进行编组或解组的默认编码, 或{@code null}.
	 * <p>默认实现返回{@code null}.
	 */
	protected String getDefaultEncoding() {
		return null;
	}


	// Marshalling

	/**
	 * 将给定根的对象图编组到提供的{@code javax.xml.transform.Result}中.
	 * <p>此实现检查给定的结果, 并调用{@code marshalDomResult}, {@code marshalSaxResult}, 或{@code marshalStreamResult}.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param result 要编组到的结果
	 * 
	 * @throws IOException 如果发生I/O异常
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 * @throws IllegalArgumentException 如果{@code result}既不是{@code DOMResult}, 也不是{@code SAXResult}, 也不是{@code StreamResult}
	 */
	@Override
	public final void marshal(Object graph, Result result) throws IOException, XmlMappingException {
		if (result instanceof DOMResult) {
			marshalDomResult(graph, (DOMResult) result);
		}
		else if (StaxUtils.isStaxResult(result)) {
			marshalStaxResult(graph, result);
		}
		else if (result instanceof SAXResult) {
			marshalSaxResult(graph, (SAXResult) result);
		}
		else if (result instanceof StreamResult) {
			marshalStreamResult(graph, (StreamResult) result);
		}
		else {
			throw new IllegalArgumentException("Unknown Result type: " + result.getClass());
		}
	}

	/**
	 * 处理{@code DOMResult}的模板方法.
	 * <p>此实现委托给{@code marshalDomNode}.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param domResult the {@code DOMResult}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 * @throws IllegalArgumentException 如果{@code domResult}为空
	 */
	protected void marshalDomResult(Object graph, DOMResult domResult) throws XmlMappingException {
		if (domResult.getNode() == null) {
			domResult.setNode(buildDocument());
		}
		marshalDomNode(graph, domResult.getNode());
	}

	/**
	 * 处理{@code StaxResult}的模板方法.
	 * <p>此实现委托给{@code marshalXMLSteamWriter}或{@code marshalXMLEventConsumer}, 具体取决于{@code StaxResult}中包含的内容.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param staxResult a JAXP 1.4 {@link StAXSource}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 * @throws IllegalArgumentException 如果{@code domResult}为空
	 */
	protected void marshalStaxResult(Object graph, Result staxResult) throws XmlMappingException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			marshalXmlStreamWriter(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				marshalXmlEventWriter(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StaxResult contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/**
	 * 处理{@code SAXResult}的模板方法.
	 * <p>此实现委托给{@code marshalSaxHandlers}.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param saxResult the {@code SAXResult}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 */
	protected void marshalSaxResult(Object graph, SAXResult saxResult) throws XmlMappingException {
		ContentHandler contentHandler = saxResult.getHandler();
		Assert.notNull(contentHandler, "ContentHandler not set on SAXResult");
		LexicalHandler lexicalHandler = saxResult.getLexicalHandler();
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	/**
	 * 处理{@code StreamResult}的模板方法.
	 * <p>此实现委托给{@code marshalOutputStream}或{@code marshalWriter}, 具体取决于{@code StreamResult}中包含的内容
	 * 
	 * @param graph 要编组的对象图的根
	 * @param streamResult the {@code StreamResult}
	 * 
	 * @throws IOException 如果发生I/O异常
	 * @throws XmlMappingException 如果给定的对象无法编组到结果中
	 * @throws IllegalArgumentException 如果{@code streamResult}既不包含{@code OutputStream}也不包含{@code Writer}
	 */
	protected void marshalStreamResult(Object graph, StreamResult streamResult)
			throws XmlMappingException, IOException {

		if (streamResult.getOutputStream() != null) {
			marshalOutputStream(graph, streamResult.getOutputStream());
		}
		else if (streamResult.getWriter() != null) {
			marshalWriter(graph, streamResult.getWriter());
		}
		else {
			throw new IllegalArgumentException("StreamResult contains neither OutputStream nor Writer");
		}
	}


	// Unmarshalling

	/**
	 * 将给定的{@code javax.xml.transform.Source}解组到对象图中.
	 * <p>此实现检查给定的结果, 并调用 {@code unmarshalDomSource}, {@code unmarshalSaxSource}, 或{@code unmarshalStreamSource}.
	 * 
	 * @param source 要解组的源
	 * 
	 * @return 对象图
	 * @throws IOException 如果发生I/O异常
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 * @throws IllegalArgumentException 如果{@code source}既不是{@code DOMSource}, 也不是{@code SAXSource}, 也不是{@code StreamSource}
	 */
	@Override
	public final Object unmarshal(Source source) throws IOException, XmlMappingException {
		if (source instanceof DOMSource) {
			return unmarshalDomSource((DOMSource) source);
		}
		else if (StaxUtils.isStaxSource(source)) {
			return unmarshalStaxSource(source);
		}
		else if (source instanceof SAXSource) {
			return unmarshalSaxSource((SAXSource) source);
		}
		else if (source instanceof StreamSource) {
			return unmarshalStreamSource((StreamSource) source);
		}
		else {
			throw new IllegalArgumentException("Unknown Source type: " + source.getClass());
		}
	}

	/**
	 * 处理{@code DOMSource}的模板方法.
	 * <p>此实现委托给{@code unmarshalDomNode}.
	 * 如果给定的源为空, 则将创建空源{@code Document}作为占位符.
	 * 
	 * @param domSource the {@code DOMSource}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 * @throws IllegalArgumentException 如果{@code domSource}为空
	 */
	protected Object unmarshalDomSource(DOMSource domSource) throws XmlMappingException {
		if (domSource.getNode() == null) {
			domSource.setNode(buildDocument());
		}
		try {
			return unmarshalDomNode(domSource.getNode());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.", ex);
			}
			throw ex;
		}
	}

	/**
	 * 处理{@code StaxSource}的模板方法.
	 * <p>此实现委托给@code unmarshalXmlStreamReader} 或 {@code unmarshalXmlEventReader}.
	 * 
	 * @param staxSource the {@code StaxSource}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 */
	protected Object unmarshalStaxSource(Source staxSource) throws XmlMappingException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return unmarshalXmlStreamReader(streamReader);
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return unmarshalXmlEventReader(eventReader);
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	/**
	 * 处理{@code SAXSource}的模板方法.
	 * <p>此实现委托给{@code unmarshalSaxReader}.
	 * 
	 * @param saxSource the {@code SAXSource}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 * @throws IOException 如果发生I/O异常
	 */
	protected Object unmarshalSaxSource(SAXSource saxSource) throws XmlMappingException, IOException {
		if (saxSource.getXMLReader() == null) {
			try {
				saxSource.setXMLReader(createXmlReader());
			}
			catch (SAXException ex) {
				throw new UnmarshallingFailureException("Could not create XMLReader for SAXSource", ex);
			}
		}
		if (saxSource.getInputSource() == null) {
			saxSource.setInputSource(new InputSource());
		}
		try {
			return unmarshalSaxReader(saxSource.getXMLReader(), saxSource.getInputSource());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.");
			}
			throw ex;
		}
	}

	/**
	 * 处理{@code StreamSource}的模板方法.
	 * <p>此实现委托给{@code unmarshalInputStream}或{@code unmarshalReader}.
	 * 
	 * @param streamSource the {@code StreamSource}
	 * 
	 * @return 对象图
	 * @throws IOException 如果发生I/O异常
	 * @throws XmlMappingException 如果给定的源无法映射到对象
	 */
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalInputStream(streamSource.getInputStream());
			}
			else {
				InputSource inputSource = new InputSource(streamSource.getInputStream());
				inputSource.setEncoding(getDefaultEncoding());
				return unmarshalSaxSource(new SAXSource(inputSource));
			}
		}
		else if (streamSource.getReader() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalReader(streamSource.getReader());
			}
			else {
				return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getReader())));
			}
		}
		else {
			return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getSystemId())));
		}
	}


	// Abstract template methods

	/**
	 * 用于将给定对象图编组到DOM {@code Node}的抽象模板方法.
	 * <p>实际上, 节点是{@code Document}节点, {@code DocumentFragment}节点或{@code Element}节点.
	 * 换句话说, 一个接受子节点的节点.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param node 包含结果树的DOM节点
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到DOM节点
	 */
	protected abstract void marshalDomNode(Object graph, Node node)
			throws XmlMappingException;

	/**
	 * 用于将给定对象编组到StAX {@code XMLEventWriter}的抽象模板方法.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param eventWriter 要写入的{@code XMLEventWriter}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到DOM节点
	 */
	protected abstract void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter)
			throws XmlMappingException;

	/**
	 * 用于将给定对象编组到StAX {@code XMLStreamWriter}的抽象模板方法.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param streamWriter 要写入的{@code XMLStreamWriter}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到DOM节点
	 */
	protected abstract void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter)
			throws XmlMappingException;

	/**
	 * 用于将给定对象图编组到SAX {@code ContentHandler}的抽象模板方法.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param contentHandler the SAX {@code ContentHandler}
	 * @param lexicalHandler the SAX2 {@code LexicalHandler}. Can be {@code null}.
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到处理器
	 */
	protected abstract void marshalSaxHandlers(
			Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException;

	/**
	 * 用于将给定对象图编组到{@code OutputStream}的抽象模板方法.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param outputStream 要写入的{@code OutputStream}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到写入器
	 * @throws IOException 如果发生I/O异常
	 */
	protected abstract void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException;

	/**
	 * 用于将给定对象图编组到{@code Writer}的抽象模板方法.
	 * 
	 * @param graph 要编组的对象图的根
	 * @param writer 要写入的{@code Writer}
	 * 
	 * @throws XmlMappingException 如果给定的对象无法编组到写入器
	 * @throws IOException 如果发生I/O异常
	 */
	protected abstract void marshalWriter(Object graph, Writer writer)
			throws XmlMappingException, IOException;

	/**
	 * 从给定DOM {@code Node}解组的抽象模板方法.
	 * 
	 * @param node 包含要解组的对象的DOM节点
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的DOM节点无法映射到对象
	 */
	protected abstract Object unmarshalDomNode(Node node) throws XmlMappingException;

	/**
	 * 从给定Stax {@code XMLEventReader}解组的抽象模板方法.
	 * 
	 * @param eventReader 要读取的{@code XMLEventReader}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的事件读取器无法转换为对象
	 */
	protected abstract Object unmarshalXmlEventReader(XMLEventReader eventReader)
			throws XmlMappingException;

	/**
	 * 从给定的Stax {@code XMLStreamReader}解组的抽象模板方法.
	 * 
	 * @param streamReader 要读取的{@code XMLStreamReader}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的流读取器无法转换为对象
	 */
	protected abstract Object unmarshalXmlStreamReader(XMLStreamReader streamReader)
			throws XmlMappingException;

	/**
	 * 使用给定SAX {@code XMLReader}和{@code InputSource}进行解组的抽象模板方法.
	 * 
	 * @param xmlReader 要解析的SAX {@code XMLReader}
	 * @param inputSource 要解析的输入源
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的读取器和输入源无法转换为对象
	 * @throws IOException 如果发生I/O异常
	 */
	protected abstract Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException;

	/**
	 * 从给定{@code InputStream}解组的抽象模板方法.
	 * 
	 * @param inputStream 要读取的{@code InputStreamStream}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的流不能转换为对象
	 * @throws IOException 如果发生I/O异常
	 */
	protected abstract Object unmarshalInputStream(InputStream inputStream)
			throws XmlMappingException, IOException;

	/**
	 * 从给定{@code Reader}解组的抽象模板方法.
	 * 
	 * @param reader 要读取的{@code Reader}
	 * 
	 * @return 对象图
	 * @throws XmlMappingException 如果给定的读取器无法转换为对象
	 * @throws IOException 如果发生I/O异常
	 */
	protected abstract Object unmarshalReader(Reader reader)
			throws XmlMappingException, IOException;


	private static final EntityResolver NO_OP_ENTITY_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(""));
		}
	};

}
