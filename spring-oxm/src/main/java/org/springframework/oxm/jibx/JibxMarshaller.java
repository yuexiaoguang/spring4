package org.springframework.oxm.jibx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.ValidationException;
import org.jibx.runtime.impl.MarshallingContext;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.StAXWriter;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * JiBX的{@code Marshaller}和{@code Unmarshaller}接口的实现.
 *
 * <p>典型的用法是在此bean上设置{@code targetClass}和可选的{@code bindingName}属性.
 */
public class JibxMarshaller extends AbstractMarshaller implements InitializingBean {

	private static final String DEFAULT_BINDING_NAME = "binding";


	private Class<?> targetClass;

	private String targetPackage;

	private String bindingName;

	private int indent = -1;

	private String encoding = "UTF-8";

	private Boolean standalone;

	private String docTypeRootElementName;

	private String docTypeSystemId;

	private String docTypePublicId;

	private String docTypeInternalSubset;

	private IBindingFactory bindingFactory;

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * 设置此实例的目标类.
	 * 设置此属性或{@link #setTargetPackage(String) targetPackage}属性是必需的.
	 * <p>如果设置了此属性, 则忽略{@link #setTargetPackage(String) targetPackage}.
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 设置此实例的目标包.
	 * 设置此属性或{@link #setTargetClass(Class) targetClass}属性是必需的.
	 * <p>如果设置了此属性, 则忽略{@link #setTargetClass(Class) targetClass}.
	 */
	public void setTargetPackage(String targetPackage) {
		this.targetPackage = targetPackage;
	}

	/**
	 * 设置此实例可选的绑定名称.
	 */
	public void setBindingName(String bindingName) {
		this.bindingName = bindingName;
	}

	/**
	 * 设置嵌套缩进空格的数量. 默认{@code -1}, i.e. 没有缩进.
	 */
	public void setIndent(int indent) {
		this.indent = indent;
	}

	/**
	 * 设置用于编组的文档编码. 默认UTF-8.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	protected String getDefaultEncoding() {
		return this.encoding;
	}

	/**
	 * 设置编组的文档独立标志. 默认情况下, 此标志不存在.
	 */
	public void setStandalone(Boolean standalone) {
		this.standalone = standalone;
	}

	/**
	 * 设置编组时写入的DTD声明的根元素名称.
	 * 默认{@code null} (i.e. 没有写入DTD声明).
	 * <p>如果设置值, 则还需要设置系统ID或公共ID.
	 */
	public void setDocTypeRootElementName(String docTypeRootElementName) {
		this.docTypeRootElementName = docTypeRootElementName;
	}

	/**
	 * 设置编组时写入的DTD声明的系统ID.
	 * 默认{@code null}. 仅在已设置根元素时使用.
	 * <p>设置此属性或{@code docTypePublicId}, 而不是两者都设置.
	 */
	public void setDocTypeSystemId(String docTypeSystemId) {
		this.docTypeSystemId = docTypeSystemId;
	}

	/**
	 * 设置编组时写入的DTD声明的公共ID.
	 * 默认{@code null}. 仅在已设置根元素时使用.
	 * <p>设置此属性或 {@code docTypeSystemId}, 而不是两者都设置.
	 */
	public void setDocTypePublicId(String docTypePublicId) {
		this.docTypePublicId = docTypePublicId;
	}

	/**
	 * 设置编组时写入的DTD声明的内部子集Id.
	 * 默认{@code null}. 仅在已设置根元素时使用.
	 */
	public void setDocTypeInternalSubset(String docTypeInternalSubset) {
		this.docTypeInternalSubset = docTypeInternalSubset;
	}


	@Override
	public void afterPropertiesSet() throws JiBXException {
		if (this.targetClass != null) {
			if (StringUtils.hasLength(this.bindingName)) {
				if (logger.isInfoEnabled()) {
					logger.info("Configured for target class [" + this.targetClass +
							"] using binding [" + this.bindingName + "]");
				}
				this.bindingFactory = BindingDirectory.getFactory(this.bindingName, this.targetClass);
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Configured for target class [" + this.targetClass + "]");
				}
				this.bindingFactory = BindingDirectory.getFactory(this.targetClass);
			}
		}
		else if (this.targetPackage != null) {
			if (!StringUtils.hasLength(this.bindingName)) {
				this.bindingName = DEFAULT_BINDING_NAME;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Configured for target package [" + this.targetPackage +
						"] using binding [" + this.bindingName + "]");
			}
			this.bindingFactory = BindingDirectory.getFactory(this.bindingName, this.targetPackage);
		}
		else {
			throw new IllegalArgumentException("Either 'targetClass' or 'targetPackage' is required");
		}
	}


	@Override
	public boolean supports(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (this.targetClass != null) {
			return (this.targetClass == clazz);
		}
		String[] mappedClasses = this.bindingFactory.getMappedClasses();
		String className = clazz.getName();
		for (String mappedClass : mappedClasses) {
			if (className.equals(mappedClass)) {
				return true;
			}
		}
		return false;
	}


	// Supported marshalling

	@Override
	protected void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException {
		try {
			IMarshallingContext marshallingContext = createMarshallingContext();
			marshallingContext.startDocument(this.encoding, this.standalone, outputStream);
			marshalDocument(marshallingContext, graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, true);
		}
	}

	@Override
	protected void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		try {
			IMarshallingContext marshallingContext = createMarshallingContext();
			marshallingContext.startDocument(this.encoding, this.standalone, writer);
			marshalDocument(marshallingContext, graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, true);
		}
	}

	private void marshalDocument(IMarshallingContext marshallingContext, Object graph) throws IOException, JiBXException {
		if (StringUtils.hasLength(this.docTypeRootElementName)) {
			IXMLWriter xmlWriter = marshallingContext.getXmlWriter();
			xmlWriter.writeDocType(this.docTypeRootElementName, this.docTypeSystemId,
					this.docTypePublicId, this.docTypeInternalSubset);
		}
		marshallingContext.marshalDocument(graph);
	}


	// Unsupported marshalling

	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		try {
			// JiBX 本身不支持DOM, 因此首先写入缓冲区, 然后将其转换为Node
			Result result = new DOMResult(node);
			transformAndMarshal(graph, result);
		}
		catch (IOException ex) {
			throw new MarshallingFailureException("JiBX marshalling exception", ex);
		}
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) {
		XMLStreamWriter streamWriter = StaxUtils.createEventStreamWriter(eventWriter);
		marshalXmlStreamWriter(graph, streamWriter);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		try {
			MarshallingContext marshallingContext = (MarshallingContext) createMarshallingContext();
			IXMLWriter xmlWriter = new StAXWriter(marshallingContext.getNamespaces(), streamWriter);
			marshallingContext.setXmlWriter(xmlWriter);
			marshallingContext.marshalDocument(graph);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException {
		try {
			// JiBX本身不支持SAX, 因此首先写入缓冲区, 然后将其转换为处理器
			SAXResult saxResult = new SAXResult(contentHandler);
			saxResult.setLexicalHandler(lexicalHandler);
			transformAndMarshal(graph, saxResult);
		}
		catch (IOException ex) {
			throw new MarshallingFailureException("JiBX marshalling exception", ex);
		}
	}

	private void transformAndMarshal(Object graph, Result result) throws IOException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			marshalOutputStream(graph, os);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			Transformer transformer = this.transformerFactory.newTransformer();
			transformer.transform(new StreamSource(is), result);
		}
		catch (TransformerException ex) {
			throw new MarshallingFailureException(
					"Could not transform to [" + ClassUtils.getShortName(result.getClass()) + "]", ex);
		}

	}


	// Unmarshalling

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) {
		try {
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			return unmarshalXmlStreamReader(streamReader);
		}
		catch (XMLStreamException ex) {
			return new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) {
		try {
			UnmarshallingContext unmarshallingContext = (UnmarshallingContext) createUnmarshallingContext();
			IXMLReader xmlReader = new StAXReaderWrapper(streamReader, null, true);
			unmarshallingContext.setDocument(xmlReader);
			return unmarshallingContext.unmarshalElement();
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		try {
			IUnmarshallingContext unmarshallingContext = createUnmarshallingContext();
			return unmarshallingContext.unmarshalDocument(inputStream, this.encoding);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}

	@Override
	protected Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		try {
			IUnmarshallingContext unmarshallingContext = createUnmarshallingContext();
			return unmarshallingContext.unmarshalDocument(reader);
		}
		catch (JiBXException ex) {
			throw convertJibxException(ex, false);
		}
	}


	// Unsupported Unmarshalling

	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		try {
			return transformAndUnmarshal(new DOMSource(node), null);
		}
		catch (IOException ex) {
			throw new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
		}
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		return transformAndUnmarshal(new SAXSource(xmlReader, inputSource), inputSource.getEncoding());
	}

	private Object transformAndUnmarshal(Source source, String encoding) throws IOException {
		try {
			Transformer transformer = this.transformerFactory.newTransformer();
			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
			}
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			transformer.transform(source, new StreamResult(os));
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			return unmarshalInputStream(is);
		}
		catch (TransformerException ex) {
			throw new MarshallingFailureException(
					"Could not transform from [" + ClassUtils.getShortName(source.getClass()) + "]", ex);
		}
	}


	/**
	 * 创建一个新的{@code IMarshallingContext}, 配置正确的缩进.
	 * 
	 * @return 创建的编组上下文
	 * @throws JiBXException 发生错误
	 */
	protected IMarshallingContext createMarshallingContext() throws JiBXException {
		IMarshallingContext marshallingContext = this.bindingFactory.createMarshallingContext();
		marshallingContext.setIndent(this.indent);
		return marshallingContext;
	}

	/**
	 * 创建新的{@code IUnmarshallingContext}.
	 * 
	 * @return 创建的解组上下文
	 * @throws JiBXException 发生错误
	 */
	protected IUnmarshallingContext createUnmarshallingContext() throws JiBXException {
		return this.bindingFactory.createUnmarshallingContext();
	}

	/**
	 * 将给定的{@code JiBXException}转换为{@code org.springframework.oxm}层次结构中的适当异常.
	 * <p>布尔标志用于指示在编组或解组期间是否发生此异常, 因为JiBX本身不会在其异常层次结构中进行此区分.
	 * 
	 * @param ex 发生的{@code JiBXException}
	 * @param marshalling 指示在编组({@code true}), 或解组({@code false})期间是否发生异常
	 * 
	 * @return 相应的{@code XmlMappingException}
	 */
	public XmlMappingException convertJibxException(JiBXException ex, boolean marshalling) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("JiBX validation exception", ex);
		}
		else {
			if (marshalling) {
				return new MarshallingFailureException("JiBX marshalling exception", ex);
			}
			else {
				return new UnmarshallingFailureException("JiBX unmarshalling exception", ex);
			}
		}
	}

}
