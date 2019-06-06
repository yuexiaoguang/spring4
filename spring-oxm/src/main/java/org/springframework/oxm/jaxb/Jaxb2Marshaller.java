package org.springframework.oxm.jaxb;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.oxm.mime.MimeMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.oxm.support.SaxResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * {@code GenericMarshaller}接口的实现, 用于JAXB 2.1/2.2, 包含在JDK 6 update 4+ 和 Java 7/8.
 *
 * <p>典型用法是在此bean上设置"contextPath"或"classesToBeBound"属性,
 * 可以通过设置属性, 模式, 适配器和侦听器来自定义编组器和解组器, 并引用它.
 */
public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller,
		BeanClassLoaderAware, InitializingBean {

	private static final String CID = "cid:";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private String contextPath;

	private Class<?>[] classesToBeBound;

	private String[] packagesToScan;

	private Map<String, ?> jaxbContextProperties;

	private Map<String, ?> marshallerProperties;

	private Map<String, ?> unmarshallerProperties;

	private Marshaller.Listener marshallerListener;

	private Unmarshaller.Listener unmarshallerListener;

	private ValidationEventHandler validationEventHandler;

	private XmlAdapter<?, ?>[] adapters;

	private Resource[] schemaResources;

	private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

	private LSResourceResolver schemaResourceResolver;

	private boolean lazyInit = false;

	private boolean mtomEnabled = false;

	private boolean supportJaxbElementClass = false;

	private boolean checkForXmlRootElement = true;

	private Class<?> mappedClass;

	private ClassLoader beanClassLoader;

	private final Object jaxbContextMonitor = new Object();

	private volatile JAXBContext jaxbContext;

	private Schema schema;

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;


	/**
	 * 设置多个JAXB上下文路径.
	 * 给定的上下文路径数组将转换为冒号分隔的字符串, 由JAXB支持.
	 */
	public void setContextPaths(String... contextPaths) {
		Assert.notEmpty(contextPaths, "'contextPaths' must not be empty");
		this.contextPath = StringUtils.arrayToDelimitedString(contextPaths, ":");
	}

	/**
	 * 设置JAXB上下文路径.
	 * <p>设置此属性, {@link #setClassesToBeBound "classesToBeBound"} 或 {@link #setPackagesToScan "packagesToScan"}是必需的.
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * 返回JAXB上下文路径.
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * 设置要由新创建的JAXBContext识别的Java类列表.
	 * <p>设置此属性, {@link #setContextPath "contextPath"} 或 {@link #setPackagesToScan "packagesToScan"}是必需的.
	 */
	public void setClassesToBeBound(Class<?>... classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	/**
	 * 返回要由新创建的JAXBContext识别的Java类列表.
	 */
	public Class<?>[] getClassesToBeBound() {
		return this.classesToBeBound;
	}

	/**
	 * 设置包, 以在类路径中搜索具有JAXB2注解的类.
	 * 这是使用基于Spring的搜索, 因此类似于Spring的组件扫描功能
	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
	 * <p>设置此属性, {@link #setContextPath "contextPath"} 或 {@link #setClassesToBeBound "classesToBeBound"}是必需的.
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 返回要搜索JAXB2注解的包.
	 */
	public String[] getPackagesToScan() {
		return this.packagesToScan;
	}

	/**
	 * 设置{@code JAXBContext}属性.
	 * 这些特定于实现的属性将在底层{@code JAXBContext}上设置.
	 */
	public void setJaxbContextProperties(Map<String, ?> jaxbContextProperties) {
		this.jaxbContextProperties = jaxbContextProperties;
	}

	/**
	 * 设置JAXB {@code Marshaller}属性.
	 * 这些属性将在底层JAXB {@code Marshaller}上设置, 并允许缩进等功能.
	 * 
	 * @param properties 属性
	 */
	public void setMarshallerProperties(Map<String, ?> properties) {
		this.marshallerProperties = properties;
	}

	/**
	 * 设置JAXB {@code Unmarshaller}属性.
	 * 这些属性将在底层JAXB {@code Unmarshaller}上设置.
	 * 
	 * @param properties 属性
	 */
	public void setUnmarshallerProperties(Map<String, ?> properties) {
		this.unmarshallerProperties = properties;
	}

	/**
	 * 指定要在JAXB {@code Marshaller}注册的{@code Marshaller.Listener}.
	 */
	public void setMarshallerListener(Marshaller.Listener marshallerListener) {
		this.marshallerListener = marshallerListener;
	}

	/**
	 * 设置要在JAXB {@code Unmarshaller}注册的{@code Unmarshaller.Listener}.
	 */
	public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
		this.unmarshallerListener = unmarshallerListener;
	}

	/**
	 * 设置JAXB验证事件处理器.
	 * 如果在调用任何编组API期间遇到任何验证错误, 则JAXB将调用此事件处理器.
	 */
	public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
		this.validationEventHandler = validationEventHandler;
	}

	/**
	 * 指定要在JAXB {@code Marshaller}和{@code Unmarshaller}注册的{@code XmlAdapter}
	 */
	public void setAdapters(XmlAdapter<?, ?>... adapters) {
		this.adapters = adapters;
	}

	/**
	 * 设置要用于验证的模式资源.
	 */
	public void setSchema(Resource schemaResource) {
		this.schemaResources = new Resource[] {schemaResource};
	}

	/**
	 * 设置要用于验证的模式资源.
	 */
	public void setSchemas(Resource... schemaResources) {
		this.schemaResources = schemaResources;
	}

	/**
	 * 设置模式语言.
	 * 默认是W3C XML Schema: {@code http://www.w3.org/2001/XMLSchema"}.
	 */
	public void setSchemaLanguage(String schemaLanguage) {
		this.schemaLanguage = schemaLanguage;
	}

	/**
	 * 设置资源解析器, 用于加载模式资源.
	 */
	public void setSchemaResourceResolver(LSResourceResolver schemaResourceResolver) {
		this.schemaResourceResolver = schemaResourceResolver;
	}

	/**
	 * 设置是否延迟初始化此marshaller的{@link JAXBContext}.
	 * 默认为{@code false}以在启动时初始化; 可以切换到{@code true}.
	 * <p>如果调用{@link #afterPropertiesSet()}, 则只需要进行实时初始化.
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 指定是否应启用MTOM支持.
	 * 默认{@code false}: 使用XOP/MTOM的编组未启用.
	 */
	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}

	/**
	 * 指定{@link #supports(Class)}是否为{@link JAXBElement}类返回{@code true}.
	 * <p>默认{@code false}, 意味着{@code supports(Class)}始终为{@code JAXBElement}类返回{@code false}
	 * (尽管{@link #supports(Type)}可以返回{@code true}, 因为它可以获得{@code JAXBElement}的类型参数).
	 * <p>此属性通常与
	 * {@link org.springframework.web.servlet.view.xml.MarshallingView MarshallingView}等类的使用一起启用,
	 * 因为{@code ModelAndView}在运行时不提供类型参数信息.
	 */
	public void setSupportJaxbElementClass(boolean supportJaxbElementClass) {
		this.supportJaxbElementClass = supportJaxbElementClass;
	}

	/**
	 * 指定{@link #supports(Class)}是否应检查{@link XmlRootElement @XmlRootElement}注解.
	 * <p>默认{@code true}, 意味着{@code supports(Class)}将检查此注解.
	 * 但是, 一些JAXB实现 (i.e. EclipseLink MOXy) 允许在外部定义文件中定义绑定, 从而保持类注解自由.
	 * 将此属性设置为{@code false}支持这些JAXB实现.
	 */
	public void setCheckForXmlRootElement(boolean checkForXmlRootElement) {
		this.checkForXmlRootElement = checkForXmlRootElement;
	}

	/**
	 * 为部分解组指定JAXB映射类.
	 */
	public void setMappedClass(Class<?> mappedClass) {
		this.mappedClass = mappedClass;
	}

	/**
	 * 指示是否应支持DTD解析.
	 * <p>默认{@code false}, 表示DTD已禁用.
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
	 * 请注意，只有在传递给{@link #unmarshal(Source)} 的{@code Source}为{@link SAXSource}或{@link StreamSource}时,
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


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		boolean hasContextPath = StringUtils.hasLength(this.contextPath);
		boolean hasClassesToBeBound = !ObjectUtils.isEmpty(this.classesToBeBound);
		boolean hasPackagesToScan = !ObjectUtils.isEmpty(this.packagesToScan);

		if (hasContextPath && (hasClassesToBeBound || hasPackagesToScan) ||
				(hasClassesToBeBound && hasPackagesToScan)) {
			throw new IllegalArgumentException("Specify either 'contextPath', 'classesToBeBound', " +
					"or 'packagesToScan'");
		}
		if (!hasContextPath && !hasClassesToBeBound && !hasPackagesToScan) {
			throw new IllegalArgumentException(
					"Setting either 'contextPath', 'classesToBeBound', " + "or 'packagesToScan' is required");
		}
		if (!this.lazyInit) {
			getJaxbContext();
		}
		if (!ObjectUtils.isEmpty(this.schemaResources)) {
			this.schema = loadSchema(this.schemaResources, this.schemaLanguage);
		}
	}

	/**
	 * 返回此编组器使用的JAXBContext, 如有必要, 可以延迟地构建它.
	 */
	public JAXBContext getJaxbContext() {
		if (this.jaxbContext != null) {
			return this.jaxbContext;
		}
		synchronized (this.jaxbContextMonitor) {
			if (this.jaxbContext == null) {
				try {
					if (StringUtils.hasLength(this.contextPath)) {
						this.jaxbContext = createJaxbContextFromContextPath();
					}
					else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
						this.jaxbContext = createJaxbContextFromClasses();
					}
					else if (!ObjectUtils.isEmpty(this.packagesToScan)) {
						this.jaxbContext = createJaxbContextFromPackages();
					}
				}
				catch (JAXBException ex) {
					throw convertJaxbException(ex);
				}
			}
			return this.jaxbContext;
		}
	}

	private JAXBContext createJaxbContextFromContextPath() throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with context path [" + this.contextPath + "]");
		}
		if (this.jaxbContextProperties != null) {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(this.contextPath, this.beanClassLoader, this.jaxbContextProperties);
			}
			else {
				// 类似于JAXBContext.newInstance(String)实现
				return JAXBContext.newInstance(this.contextPath, Thread.currentThread().getContextClassLoader(),
						this.jaxbContextProperties);
			}
		}
		else {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(this.contextPath, this.beanClassLoader);
			}
			else {
				return JAXBContext.newInstance(this.contextPath);
			}
		}
	}

	private JAXBContext createJaxbContextFromClasses() throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext with classes to be bound [" +
					StringUtils.arrayToCommaDelimitedString(this.classesToBeBound) + "]");
		}
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(this.classesToBeBound, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(this.classesToBeBound);
		}
	}

	private JAXBContext createJaxbContextFromPackages() throws JAXBException {
		if (logger.isInfoEnabled()) {
			logger.info("Creating JAXBContext by scanning packages [" +
					StringUtils.arrayToCommaDelimitedString(this.packagesToScan) + "]");
		}
		ClassPathJaxb2TypeScanner scanner = new ClassPathJaxb2TypeScanner(this.beanClassLoader, this.packagesToScan);
		Class<?>[] jaxb2Classes = scanner.scanPackages();
		if (logger.isDebugEnabled()) {
			logger.debug("Found JAXB2 classes: [" + StringUtils.arrayToCommaDelimitedString(jaxb2Classes) + "]");
		}
		this.classesToBeBound = jaxb2Classes;
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(jaxb2Classes, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(jaxb2Classes);
		}
	}

	private Schema loadSchema(Resource[] resources, String schemaLanguage) throws IOException, SAXException {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting validation schema to " +
					StringUtils.arrayToCommaDelimitedString(this.schemaResources));
		}
		Assert.notEmpty(resources, "No resources given");
		Assert.hasLength(schemaLanguage, "No schema language provided");
		Source[] schemaSources = new Source[resources.length];
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			if (resource == null || !resource.exists()) {
				throw new IllegalArgumentException("Resource does not exist: " + resource);
			}
			InputSource inputSource = SaxResourceUtils.createInputSource(resource);
			schemaSources[i] = new SAXSource(xmlReader, inputSource);
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance(schemaLanguage);
		if (this.schemaResourceResolver != null) {
			schemaFactory.setResourceResolver(this.schemaResourceResolver);
		}
		return schemaFactory.newSchema(schemaSources);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return (this.supportJaxbElementClass && JAXBElement.class.isAssignableFrom(clazz)) ||
				supportsInternal(clazz, this.checkForXmlRootElement);
	}

	@Override
	public boolean supports(Type genericType) {
		if (genericType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericType;
			if (JAXBElement.class == parameterizedType.getRawType() &&
					parameterizedType.getActualTypeArguments().length == 1) {
				Type typeArgument = parameterizedType.getActualTypeArguments()[0];
				if (typeArgument instanceof Class) {
					Class<?> classArgument = (Class<?>) typeArgument;
					return ((classArgument.isArray() && Byte.TYPE == classArgument.getComponentType()) ||
							isPrimitiveWrapper(classArgument) || isStandardClass(classArgument) ||
							supportsInternal(classArgument, false));
				}
				else if (typeArgument instanceof GenericArrayType) {
					// Only on JDK 6 - see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5041784
					GenericArrayType arrayType = (GenericArrayType) typeArgument;
					return (Byte.TYPE == arrayType.getGenericComponentType());
				}
			}
		}
		else if (genericType instanceof Class) {
			Class<?> clazz = (Class<?>) genericType;
			return supportsInternal(clazz, this.checkForXmlRootElement);
		}
		return false;
	}

	private boolean supportsInternal(Class<?> clazz, boolean checkForXmlRootElement) {
		if (checkForXmlRootElement && AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) == null) {
			return false;
		}
		if (StringUtils.hasLength(this.contextPath)) {
			String packageName = ClassUtils.getPackageName(clazz);
			String[] contextPaths = StringUtils.tokenizeToStringArray(this.contextPath, ":");
			for (String contextPath : contextPaths) {
				if (contextPath.equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
			return Arrays.asList(this.classesToBeBound).contains(clazz);
		}
		return false;
	}

	/**
	 * 检查给定类型是否为原始包装类型.
	 * 比较JAXB2规范的8.5.1节.
	 */
	private boolean isPrimitiveWrapper(Class<?> clazz) {
		return (Boolean.class == clazz ||
				Byte.class == clazz ||
				Short.class == clazz ||
				Integer.class == clazz ||
				Long.class == clazz ||
				Float.class == clazz ||
				Double.class == clazz);
	}

	/**
	 * 检查给定类型是否为标准类.
	 * 比较JAXB2规范的8.5.2节.
	 */
	private boolean isStandardClass(Class<?> clazz) {
		return (String.class == clazz ||
				BigInteger.class.isAssignableFrom(clazz) ||
				BigDecimal.class.isAssignableFrom(clazz) ||
				Calendar.class.isAssignableFrom(clazz) ||
				Date.class.isAssignableFrom(clazz) ||
				QName.class.isAssignableFrom(clazz) ||
				URI.class == clazz ||
				XMLGregorianCalendar.class.isAssignableFrom(clazz) ||
				Duration.class.isAssignableFrom(clazz) ||
				Image.class == clazz ||
				DataHandler.class == clazz ||
				// 应根据JAXB2规范支持源和子类, 但不在RI中
				// Source.class.isAssignableFrom(clazz) ||
				UUID.class == clazz);

	}


	// Marshalling

	@Override
	public void marshal(Object graph, Result result) throws XmlMappingException {
		marshal(graph, result, null);
	}

	@Override
	public void marshal(Object graph, Result result, MimeContainer mimeContainer) throws XmlMappingException {
		try {
			Marshaller marshaller = createMarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
				marshaller.setAttachmentMarshaller(new Jaxb2AttachmentMarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxResult(result)) {
				marshalStaxResult(marshaller, graph, result);
			}
			else {
				marshaller.marshal(graph, result);
			}
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	private void marshalStaxResult(Marshaller jaxbMarshaller, Object graph, Result staxResult) throws JAXBException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			jaxbMarshaller.marshal(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				jaxbMarshaller.marshal(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StAX Result contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/**
	 * 返回一个新创建的JAXB编组器. JAXB 编组器不一定是线程安全的.
	 */
	protected Marshaller createMarshaller() {
		try {
			Marshaller marshaller = getJaxbContext().createMarshaller();
			initJaxbMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * 可以由具体JAXB 编组器覆盖的模板方法, 用于自定义初始化行为.
	 * 在创建JAXB {@code Marshaller}之后, 并在设置了相应的属性之后调用.
	 * <p>默认实现设置{@link #setMarshallerProperties(Map) 定义的属性},
	 * {@link #setValidationEventHandler(ValidationEventHandler) 验证事件处理器},
	 * {@link #setSchemas(Resource[]) 模式}, {@link #setMarshallerListener(javax.xml.bind.Marshaller.Listener) 监听器},
	 * 和{@link #setAdapters(XmlAdapter[]) 适配器}.
	 */
	protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
		if (this.marshallerProperties != null) {
			for (String name : this.marshallerProperties.keySet()) {
				marshaller.setProperty(name, this.marshallerProperties.get(name));
			}
		}
		if (this.marshallerListener != null) {
			marshaller.setListener(this.marshallerListener);
		}
		if (this.validationEventHandler != null) {
			marshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				marshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			marshaller.setSchema(this.schema);
		}
	}


	// Unmarshalling

	@Override
	public Object unmarshal(Source source) throws XmlMappingException {
		return unmarshal(source, null);
	}

	@Override
	public Object unmarshal(Source source, MimeContainer mimeContainer) throws XmlMappingException {
		source = processSource(source);

		try {
			Unmarshaller unmarshaller = createUnmarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
				unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxSource(source)) {
				return unmarshalStaxSource(unmarshaller, source);
			}
			else if (this.mappedClass != null) {
				return unmarshaller.unmarshal(source, this.mappedClass).getValue();
			}
			else {
				return unmarshaller.unmarshal(source);
			}
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.", ex);
			}
			throw ex;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	protected Object unmarshalStaxSource(Unmarshaller jaxbUnmarshaller, Source staxSource) throws JAXBException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return (this.mappedClass != null ?
					jaxbUnmarshaller.unmarshal(streamReader, this.mappedClass).getValue() :
					jaxbUnmarshaller.unmarshal(streamReader));
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return (this.mappedClass != null ?
						jaxbUnmarshaller.unmarshal(eventReader, this.mappedClass).getValue() :
						jaxbUnmarshaller.unmarshal(eventReader));
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	private Source processSource(Source source) {
		if (StaxUtils.isStaxSource(source) || source instanceof DOMSource) {
			return source;
		}

		XMLReader xmlReader = null;
		InputSource inputSource = null;

		if (source instanceof SAXSource) {
			SAXSource saxSource = (SAXSource) source;
			xmlReader = saxSource.getXMLReader();
			inputSource = saxSource.getInputSource();
		}
		else if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getInputStream() != null) {
				inputSource = new InputSource(streamSource.getInputStream());
			}
			else if (streamSource.getReader() != null) {
				inputSource = new InputSource(streamSource.getReader());
			}
			else {
				inputSource = new InputSource(streamSource.getSystemId());
			}
		}

		try {
			if (xmlReader == null) {
				xmlReader = XMLReaderFactory.createXMLReader();
			}
			xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			String name = "http://xml.org/sax/features/external-general-entities";
			xmlReader.setFeature(name, isProcessExternalEntities());
			if (!isProcessExternalEntities()) {
				xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			return new SAXSource(xmlReader, inputSource);
		}
		catch (SAXException ex) {
			logger.warn("Processing of external entities could not be disabled", ex);
			return source;
		}
	}

	/**
	 * 返回一个新创建的JAXB 解组器.
	 * Note: JAXB 解组器不一定是线程安全的.
	 */
	protected Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
			initJaxbUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * 可以由具体JAXB 解组器覆盖的模板方法, 用于自定义初始化行为.
	 * 在创建JAXB {@code Unmarshaller}之后, 并在设置了相应的属性之后调用.
	 * <p>默认实现设置{@link #setUnmarshallerProperties(Map) 定义的属性},
	 * {@link #setValidationEventHandler(ValidationEventHandler) 验证事件处理器},
	 * {@link #setSchemas(Resource[]) 模式}, {@link #setUnmarshallerListener(javax.xml.bind.Unmarshaller.Listener) 监听器},
	 * 和{@link #setAdapters(XmlAdapter[]) 适配器}.
	 */
	protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
		if (this.unmarshallerProperties != null) {
			for (String name : this.unmarshallerProperties.keySet()) {
				unmarshaller.setProperty(name, this.unmarshallerProperties.get(name));
			}
		}
		if (this.unmarshallerListener != null) {
			unmarshaller.setListener(this.unmarshallerListener);
		}
		if (this.validationEventHandler != null) {
			unmarshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				unmarshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			unmarshaller.setSchema(this.schema);
		}
	}

	/**
	 * 将给定的{@code JAXBException}转换为{@code org.springframework.oxm}层次结构中的适当异常.
	 * 
	 * @param ex 发生的{@code JAXBException}
	 * 
	 * @return 相应的{@code XmlMappingException}
	 */
	protected XmlMappingException convertJaxbException(JAXBException ex) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("JAXB validation exception", ex);
		}
		else if (ex instanceof MarshalException) {
			return new MarshallingFailureException("JAXB marshalling exception", ex);
		}
		else if (ex instanceof UnmarshalException) {
			return new UnmarshallingFailureException("JAXB unmarshalling exception", ex);
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown JAXB exception", ex);
		}
	}


	private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentMarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public String addMtomAttachment(byte[] data, int offset, int length, String mimeType,
				String elementNamespace, String elementLocalName) {
			ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
			return addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
		}

		@Override
		public String addMtomAttachment(DataHandler dataHandler, String elementNamespace, String elementLocalName) {
			String host = getHost(elementNamespace, dataHandler);
			String contentId = UUID.randomUUID() + "@" + host;
			this.mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
			try {
				contentId = URLEncoder.encode(contentId, "UTF-8");
			}
			catch (UnsupportedEncodingException ex) {
				// ignore
			}
			return CID + contentId;
		}

		private String getHost(String elementNamespace, DataHandler dataHandler) {
			try {
				URI uri = new URI(elementNamespace);
				return uri.getHost();
			}
			catch (URISyntaxException ex) {
				// ignore
			}
			return dataHandler.getName();
		}

		@Override
		public String addSwaRefAttachment(DataHandler dataHandler) {
			String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
			this.mimeContainer.addAttachment(contentId, dataHandler);
			return contentId;
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.convertToXopPackage();
		}
	}


	private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentUnmarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public byte[] getAttachmentAsByteArray(String cid) {
			try {
				DataHandler dataHandler = getAttachmentAsDataHandler(cid);
				return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
			}
			catch (IOException ex) {
				throw new UnmarshallingFailureException("Couldn't read attachment", ex);
			}
		}

		@Override
		public DataHandler getAttachmentAsDataHandler(String contentId) {
			if (contentId.startsWith(CID)) {
				contentId = contentId.substring(CID.length());
				try {
					contentId = URLDecoder.decode(contentId, "UTF-8");
				}
				catch (UnsupportedEncodingException ex) {
					// ignore
				}
				contentId = '<' + contentId + '>';
			}
			return this.mimeContainer.getAttachment(contentId);
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.isXopPackage();
		}
	}


	/**
	 * 包装字节数组的DataSource.
	 */
	private static class ByteArrayDataSource implements DataSource {

		private final byte[] data;

		private final String contentType;

		private final int offset;

		private final int length;

		public ByteArrayDataSource(String contentType, byte[] data, int offset, int length) {
			this.contentType = contentType;
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(this.data, this.offset, this.length);
		}

		@Override
		public OutputStream getOutputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		@Override
		public String getName() {
			return "ByteArrayDataSource";
		}
	}


	private static final EntityResolver NO_OP_ENTITY_RESOLVER = new EntityResolver() {
		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(""));
		}
	};
}
