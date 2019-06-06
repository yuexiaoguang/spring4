package org.springframework.oxm.xstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * XStream的{@code Marshaller}接口实现.
 *
 * <p>默认情况下, XStream不需要任何进一步的配置, 并且可以编组/解组类路径上的任何类.
 * 因此, <b>不建议使用{@code XStreamMarshaller}来解析来自外部源的XML</b> (i.e. the Web), 因为这可能导致<b>全漏洞</b>.
 * 如果确实需要使用{@code XStreamMarshaller}来解组外部XML, 设置{@link #setSupportedClasses(Class[]) supportedClasses}
 * 和{@link #setConverters(ConverterMatcher[]) converters}属性 (可能使用{@link CatchAllConverter})
 * 或覆盖{@link #customizeXStream(XStream)}方法以确保它只接受希望它支持的类.
 *
 * <p>由于XStream的API, 需要设置用于写入OutputStreams的编码. 默认{@code UTF-8}.
 *
 * <p><b>NOTE:</b> XStream是一个XML序列化库, 而不是数据绑定库.
 * 因此, 它具有有限的命名空间支持. 因此, 它不适合在Web服务中使用.
 *
 * <p>从Spring 4.3开始, 这个编组器需要XStream 1.4.5或更高版本.
 * 请注意{@link XStream}构造已在4.0中重新编写, 流驱动器和类加载器现在已经传递到XStream本身.
 */
public class XStreamMarshaller extends AbstractMarshaller implements BeanClassLoaderAware, InitializingBean {

	/**
	 * 用于流访问的默认编码: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private ReflectionProvider reflectionProvider;

	private HierarchicalStreamDriver streamDriver;

	private HierarchicalStreamDriver defaultDriver;

	private Mapper mapper;

	private Class<? extends MapperWrapper>[] mapperWrappers;

	private ConverterLookup converterLookup = new DefaultConverterLookup();

	private ConverterRegistry converterRegistry = (ConverterRegistry) this.converterLookup;

	private ConverterMatcher[] converters;

	private MarshallingStrategy marshallingStrategy;

	private Integer mode;

	private Map<String, ?> aliases;

	private Map<String, ?> aliasesByType;

	private Map<String, String> fieldAliases;

	private Class<?>[] useAttributeForTypes;

	private Map<?, ?> useAttributeFor;

	private Map<Class<?>, String> implicitCollections;

	private Map<Class<?>, String> omittedFields;

	private Class<?>[] annotatedClasses;

	private boolean autodetectAnnotations;

	private String encoding = DEFAULT_ENCODING;

	private NameCoder nameCoder = new XmlFriendlyNameCoder();

	private Class<?>[] supportedClasses;

	private ClassLoader beanClassLoader = new CompositeClassLoader();

	private XStream xstream;


	/**
	 * 设置要使用的自定义XStream {@link ReflectionProvider}.
	 */
	public void setReflectionProvider(ReflectionProvider reflectionProvider) {
		this.reflectionProvider = reflectionProvider;
	}

	/**
	 * 设置用于读取器和写入器的XStream {@link HierarchicalStreamDriver}.
	 * <p>从Spring 4.0开始, 此流驱动程序也将传递给{@link XStream}构造函数, 因此由与流相关的本机API方法本身使用.
	 */
	public void setStreamDriver(HierarchicalStreamDriver streamDriver) {
		this.streamDriver = streamDriver;
		this.defaultDriver = streamDriver;
	}

	private HierarchicalStreamDriver getDefaultDriver() {
		if (this.defaultDriver == null) {
			this.defaultDriver = new XppDriver();
		}
		return this.defaultDriver;
	}

	/**
	 * 设置要使用的自定义XStream {@link Mapper}.
	 */
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 设置一个或多个自定义XStream {@link MapperWrapper}类.
	 * 每个类都需要一个构造函数, 其中包含{@link Mapper}或{@link MapperWrapper}类型的单个参数.
	 */
	@SuppressWarnings("unchecked")
	public void setMapperWrappers(Class<? extends MapperWrapper>... mapperWrappers) {
		this.mapperWrappers = mapperWrappers;
	}

	/**
	 * 设置要使用的自定义XStream {@link ConverterLookup}.
	 * 如果给定的引用也实现它, 也用作{@link ConverterRegistry}.
	 */
	public void setConverterLookup(ConverterLookup converterLookup) {
		this.converterLookup = converterLookup;
		if (converterLookup instanceof ConverterRegistry) {
			this.converterRegistry = (ConverterRegistry) converterLookup;
		}
	}

	/**
	 * 设置要使用的自定义XStream {@link ConverterRegistry}.
	 */
	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

	/**
	 * 设置{@code Converters}或{@code SingleValueConverters}, 以在{@code XStream}实例中注册.
	 */
	public void setConverters(ConverterMatcher... converters) {
		this.converters = converters;
	}

	/**
	 * 设置要使用的自定义XStream {@link MarshallingStrategy}.
	 */
	public void setMarshallingStrategy(MarshallingStrategy marshallingStrategy) {
		this.marshallingStrategy = marshallingStrategy;
	}

	/**
	 * 设置要使用的XStream模式.
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * 设置别名/类型映射, 由映射到类的字符串别名组成.
	 * <p>键是别名; 值可以是{@code Class}实例, 也可以是String类名.
	 */
	public void setAliases(Map<String, ?> aliases) {
		this.aliases = aliases;
	}

	/**
	 * 设置<em>类型到别名</em>的映射, 由映射到类的字符串组成.
	 * <p>任何可分配给此类型的类都将使用相同名称的别名.
	 * 键是别名; 值可以是{@code Class}实例, 也可以是String类名.
	 */
	public void setAliasesByType(Map<String, ?> aliasesByType) {
		this.aliasesByType = aliasesByType;
	}

	/**
	 * 设置字段别名/类型映射, 由字段名称组成.
	 */
	public void setFieldAliases(Map<String, String> fieldAliases) {
		this.fieldAliases = fieldAliases;
	}

	/**
	 * 设置要使用XML属性的类型.
	 */
	public void setUseAttributeForTypes(Class<?>... useAttributeForTypes) {
		this.useAttributeForTypes = useAttributeForTypes;
	}

	/**
	 * 设置要使用XML属性的类型.
	 * 给定的映射可以包含{@code <String, Class>}对, 在这种情况下, 调用{@link XStream#useAttributeFor(String, Class)}.
	 * 或者, 映射可以包含{@code <Class, String>}或{@code <Class, List<String>>}对,
	 * 这会导致{@link XStream#useAttributeFor(Class, String)}调用.
	 */
	public void setUseAttributeFor(Map<?, ?> useAttributeFor) {
		this.useAttributeFor = useAttributeFor;
	}

	/**
	 * 指定隐式集合字段, 作为映射到逗号分隔的集合字段名称的{@code Class}实例的Map.
	 */
	public void setImplicitCollections(Map<Class<?>, String> implicitCollections) {
		this.implicitCollections = implicitCollections;
	}

	/**
	 * 指定省略的字段, 作为映射到逗号分隔的字段名称的{@code Class}实例的Map.
	 */
	public void setOmittedFields(Map<Class<?>, String> omittedFields) {
		this.omittedFields = omittedFields;
	}

	/**
	 * 设置将从类级注解元数据中读取别名的带注解的类.
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * 激活XStream的自动检测模式.
	 * <p><b>Note</b>: 自动检测意味着在处理XML流时正在配置XStream实例, 因此引入了潜在的并发问题.
	 */
	public void setAutodetectAnnotations(boolean autodetectAnnotations) {
		this.autodetectAnnotations = autodetectAnnotations;
	}

	/**
	 * 设置用于流访问的编码.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	protected String getDefaultEncoding() {
		return this.encoding;
	}

	/**
	 * 设置要使用的自定义XStream {@link NameCoder}.
	 * 默认{@link XmlFriendlyNameCoder}.
	 */
	public void setNameCoder(NameCoder nameCoder) {
		this.nameCoder = nameCoder;
	}

	/**
	 * 设置此编组器支持的类.
	 * <p>如果此属性为空 (默认), 则支持所有类.
	 */
	public void setSupportedClasses(Class<?>... supportedClasses) {
		this.supportedClasses = supportedClasses;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() {
		this.xstream = buildXStream();
	}

	/**
	 * 构建此编组器使用的本机XStream委托, 委托给{@link #constructXStream()}, {@link #configureXStream}和{@link #customizeXStream}.
	 */
	protected XStream buildXStream() {
		XStream xstream = constructXStream();
		configureXStream(xstream);
		customizeXStream(xstream);
		return xstream;
	}

	/**
	 * 构造一个XStream实例, 使用其中一个标准构造函数或创建自定义子类.
	 * 
	 * @return {@code XStream}实例
	 */
	protected XStream constructXStream() {
		return new XStream(this.reflectionProvider, getDefaultDriver(), new ClassLoaderReference(this.beanClassLoader),
				this.mapper, this.converterLookup, this.converterRegistry) {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				MapperWrapper mapperToWrap = next;
				if (mapperWrappers != null) {
					for (Class<? extends MapperWrapper> mapperWrapper : mapperWrappers) {
						Constructor<? extends MapperWrapper> ctor;
						try {
							ctor = mapperWrapper.getConstructor(Mapper.class);
						}
						catch (NoSuchMethodException ex) {
							try {
								ctor = mapperWrapper.getConstructor(MapperWrapper.class);
							}
							catch (NoSuchMethodException ex2) {
								throw new IllegalStateException("No appropriate MapperWrapper constructor found: " + mapperWrapper);
							}
						}
						try {
							mapperToWrap = ctor.newInstance(mapperToWrap);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Failed to construct MapperWrapper: " + mapperWrapper);
						}
					}
				}
				return mapperToWrap;
			}
		};
	}

	/**
	 * 使用此编组器的bean属性配置XStream实例.
	 * 
	 * @param xstream {@code XStream}实例
	 */
	protected void configureXStream(XStream xstream) {
		if (this.converters != null) {
			for (int i = 0; i < this.converters.length; i++) {
				if (this.converters[i] instanceof Converter) {
					xstream.registerConverter((Converter) this.converters[i], i);
				}
				else if (this.converters[i] instanceof SingleValueConverter) {
					xstream.registerConverter((SingleValueConverter) this.converters[i], i);
				}
				else {
					throw new IllegalArgumentException("Invalid ConverterMatcher [" + this.converters[i] + "]");
				}
			}
		}

		if (this.marshallingStrategy != null) {
			xstream.setMarshallingStrategy(this.marshallingStrategy);
		}
		if (this.mode != null) {
			xstream.setMode(this.mode);
		}

		try {
			if (this.aliases != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliases);
				for (Map.Entry<String, Class<?>> entry : classMap.entrySet()) {
					xstream.alias(entry.getKey(), entry.getValue());
				}
			}
			if (this.aliasesByType != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliasesByType);
				for (Map.Entry<String, Class<?>> entry : classMap.entrySet()) {
					xstream.aliasType(entry.getKey(), entry.getValue());
				}
			}
			if (this.fieldAliases != null) {
				for (Map.Entry<String, String> entry : this.fieldAliases.entrySet()) {
					String alias = entry.getValue();
					String field = entry.getKey();
					int idx = field.lastIndexOf('.');
					if (idx != -1) {
						String className = field.substring(0, idx);
						Class<?> clazz = ClassUtils.forName(className, this.beanClassLoader);
						String fieldName = field.substring(idx + 1);
						xstream.aliasField(alias, clazz, fieldName);
					}
					else {
						throw new IllegalArgumentException("Field name [" + field + "] does not contain '.'");
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load specified alias class", ex);
		}

		if (this.useAttributeForTypes != null) {
			for (Class<?> type : this.useAttributeForTypes) {
				xstream.useAttributeFor(type);
			}
		}
		if (this.useAttributeFor != null) {
			for (Map.Entry<?, ?> entry : this.useAttributeFor.entrySet()) {
				if (entry.getKey() instanceof String) {
					if (entry.getValue() instanceof Class) {
						xstream.useAttributeFor((String) entry.getKey(), (Class<?>) entry.getValue());
					}
					else {
						throw new IllegalArgumentException(
								"'useAttributesFor' takes Map<String, Class> when using a map key of type String");
					}
				}
				else if (entry.getKey() instanceof Class) {
					Class<?> key = (Class<?>) entry.getKey();
					if (entry.getValue() instanceof String) {
						xstream.useAttributeFor(key, (String) entry.getValue());
					}
					else if (entry.getValue() instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> listValue = (List<Object>) entry.getValue();
						for (Object element : listValue) {
							if (element instanceof String) {
								xstream.useAttributeFor(key, (String) element);
							}
						}
					}
					else {
						throw new IllegalArgumentException("'useAttributesFor' property takes either Map<Class, String> " +
								"or Map<Class, List<String>> when using a map key of type Class");
					}
				}
				else {
					throw new IllegalArgumentException(
							"'useAttributesFor' property takes either a map key of type String or Class");
				}
			}
		}

		if (this.implicitCollections != null) {
			for (Map.Entry<Class<?>, String> entry : this.implicitCollections.entrySet()) {
				String[] collectionFields = StringUtils.commaDelimitedListToStringArray(entry.getValue());
				for (String collectionField : collectionFields) {
					xstream.addImplicitCollection(entry.getKey(), collectionField);
				}
			}
		}
		if (this.omittedFields != null) {
			for (Map.Entry<Class<?>, String> entry : this.omittedFields.entrySet()) {
				String[] fields = StringUtils.commaDelimitedListToStringArray(entry.getValue());
				for (String field : fields) {
					xstream.omitField(entry.getKey(), field);
				}
			}
		}

		if (this.annotatedClasses != null) {
			xstream.processAnnotations(this.annotatedClasses);
		}
		if (this.autodetectAnnotations) {
			xstream.autodetectAnnotations(true);
		}
	}

	private Map<String, Class<?>> toClassMap(Map<String, ?> map) throws ClassNotFoundException {
		Map<String, Class<?>> result = new LinkedHashMap<String, Class<?>>(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Class<?> type;
			if (value instanceof Class) {
				type = (Class<?>) value;
			}
			else if (value instanceof String) {
				String className = (String) value;
				type = ClassUtils.forName(className, this.beanClassLoader);
			}
			else {
				throw new IllegalArgumentException("Unknown value [" + value + "] - expected String or Class");
			}
			result.put(key, type);
		}
		return result;
	}

	/**
	 * 允许自定义给定{@link XStream}的模板.
	 * <p>默认实现为空.
	 * 
	 * @param xstream {@code XStream}实例
	 */
	protected void customizeXStream(XStream xstream) {
	}

	/**
	 * 返回此编组器使用的本机XStream委托.
	 * <p><b>NOTE: 从Spring 4.0开始, 此方法已标记为final.</b>
	 * 它可用于访问完全配置的XStream以进行编组, 但不再用于配置目的.
	 */
	public final XStream getXStream() {
		if (this.xstream == null) {
			this.xstream = buildXStream();
		}
		return this.xstream;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		if (ObjectUtils.isEmpty(this.supportedClasses)) {
			return true;
		}
		else {
			for (Class<?> supportedClass : this.supportedClasses) {
				if (supportedClass.isAssignableFrom(clazz)) {
					return true;
				}
			}
			return false;
		}
	}


	// Marshalling

	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		HierarchicalStreamWriter streamWriter;
		if (node instanceof Document) {
			streamWriter = new DomWriter((Document) node, this.nameCoder);
		}
		else if (node instanceof Element) {
			streamWriter = new DomWriter((Element) node, node.getOwnerDocument(), this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMResult contains neither Document nor Element");
		}
		doMarshal(graph, streamWriter, null);
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
		ContentHandler contentHandler = StaxUtils.createContentHandler(eventWriter);
		LexicalHandler lexicalHandler = null;
		if (contentHandler instanceof LexicalHandler) {
			lexicalHandler = (LexicalHandler) contentHandler;
		}
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		try {
			doMarshal(graph, new StaxWriter(new QNameMap(), streamWriter, this.nameCoder), null);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, true);
		}
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, LexicalHandler lexicalHandler)
			throws XmlMappingException {

		SaxWriter saxWriter = new SaxWriter(this.nameCoder);
		saxWriter.setContentHandler(contentHandler);
		doMarshal(graph, saxWriter, null);
	}

	@Override
	public void marshalOutputStream(Object graph, OutputStream outputStream) throws XmlMappingException, IOException {
		marshalOutputStream(graph, outputStream, null);
	}

	public void marshalOutputStream(Object graph, OutputStream outputStream, DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(outputStream), dataHolder);
		}
		else {
			marshalWriter(graph, new OutputStreamWriter(outputStream, this.encoding), dataHolder);
		}
	}

	@Override
	public void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		marshalWriter(graph, writer, null);
	}

	public void marshalWriter(Object graph, Writer writer, DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(writer), dataHolder);
		}
		else {
			doMarshal(graph, new CompactWriter(writer), dataHolder);
		}
	}

	/**
	 * 将给定图编组到给定的XStream HierarchicalStreamWriter.
	 * 使用{@link #convertXStreamException}转换异常.
	 */
	private void doMarshal(Object graph, HierarchicalStreamWriter streamWriter, DataHolder dataHolder) {
		try {
			getXStream().marshal(graph, streamWriter, dataHolder);
		}
		catch (Exception ex) {
			throw convertXStreamException(ex, true);
		}
		finally {
			try {
				streamWriter.flush();
			}
			catch (Exception ex) {
				logger.debug("Could not flush HierarchicalStreamWriter", ex);
			}
		}
	}


	// Unmarshalling

	@Override
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			return unmarshalInputStream(streamSource.getInputStream());
		}
		else if (streamSource.getReader() != null) {
			return unmarshalReader(streamSource.getReader());
		}
		else {
			throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
		}
	}

	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		HierarchicalStreamReader streamReader;
		if (node instanceof Document) {
			streamReader = new DomReader((Document) node, this.nameCoder);
		}
		else if (node instanceof Element) {
			streamReader = new DomReader((Element) node, this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMSource contains neither Document nor Element");
		}
        return doUnmarshal(streamReader, null);
	}

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
		try {
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			return unmarshalXmlStreamReader(streamReader);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, false);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
        return doUnmarshal(new StaxReader(new QNameMap(), streamReader, this.nameCoder), null);
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		throw new UnsupportedOperationException(
				"XStreamMarshaller does not support unmarshalling using SAX XMLReaders");
	}

	@Override
	public Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		return unmarshalInputStream(inputStream, null);
	}

	public Object unmarshalInputStream(InputStream inputStream, DataHolder dataHolder) throws XmlMappingException, IOException {
        if (this.streamDriver != null) {
            return doUnmarshal(this.streamDriver.createReader(inputStream), dataHolder);
        }
        else {
		    return unmarshalReader(new InputStreamReader(inputStream, this.encoding), dataHolder);
        }
	}

	@Override
	public Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		return unmarshalReader(reader, null);
	}

	public Object unmarshalReader(Reader reader, DataHolder dataHolder) throws XmlMappingException, IOException {
		return doUnmarshal(getDefaultDriver().createReader(reader), dataHolder);
	}

    /**
     * 将给定的图解组到给定的XStream HierarchicalStreamWriter.
     * 使用{@link #convertXStreamException}转换异常.
     */
    private Object doUnmarshal(HierarchicalStreamReader streamReader, DataHolder dataHolder) {
        try {
            return getXStream().unmarshal(streamReader, null, dataHolder);
        }
        catch (Exception ex) {
            throw convertXStreamException(ex, false);
        }
    }


    /**
     * 将给定的XStream异常转换为{@code org.springframework.oxm}层次结构中的适当异常.
     * <p>boolean标志用于指示在编组或解组期间是否发生此异常, 因为XStream本身不会在其异常层次结构中进行此区分.
     * 
     * @param ex 发生的XStream异常
     * @param marshalling 指示在编组({@code true}), 或解组({@code false})期间是否发生异常
     * 
     * @return 相应的{@code XmlMappingException}
     */
	protected XmlMappingException convertXStreamException(Exception ex, boolean marshalling) {
		if (ex instanceof StreamException || ex instanceof CannotResolveClassException ||
				ex instanceof ConversionException) {
			if (marshalling) {
				return new MarshallingFailureException("XStream marshalling exception",  ex);
			}
			else {
				return new UnmarshallingFailureException("XStream unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XStream exception", ex);
		}
	}
}
