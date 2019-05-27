package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * 用于XML bean定义的Bean定义读取器.
 * 将实际的XML文档读取委托给{@link BeanDefinitionDocumentReader}接口的实现.
 *
 * <p>通常应用于
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * 或{@link org.springframework.context.support.GenericApplicationContext}.
 *
 * <p>此类加载DOM文档, 并将BeanDefinitionDocumentReader应用于它.
 * 文档读取器将使用给定的bean工厂注册每个bean定义,
 * 采取后者的{@link org.springframework.beans.factory.support.BeanDefinitionRegistry}接口的实现.
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 禁用验证.
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * 自动检测验证模式.
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * 使用DTD验证.
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * 使用XSD验证.
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


	/** 此类的常量实例 */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	private int validationMode = VALIDATION_AUTO;

	private boolean namespaceAware = false;

	private Class<?> documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	private NamespaceHandlerResolver namespaceHandlerResolver;

	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	private EntityResolver entityResolver;

	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded");


	/**
	 * 为给定的bean工厂创建新的XmlBeanDefinitionReader.
	 * 
	 * @param registry BeanFactory以BeanDefinitionRegistry的形式加载bean定义
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 设置是否使用XML验证. 默认 {@code true}.
	 * <p>此方法在关闭验证时切换命名空间感知, 以便在这种情况下仍然正确处理模式命名空间.
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * 将验证模式设置为按名称使用. 默认{@link #VALIDATION_AUTO}.
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * 设置要使用的验证模式. 默认{@link #VALIDATION_AUTO}.
	 * <p>请注意, 这仅激活或停用验证本身.
	 * 如果要关闭模式文件的验证, 则可能需要显式激活模式命名空间支持: see {@link #setNamespaceAware}.
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 返回要使用的验证模式.
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * 设置XML解析器是否应该支持XML命名空间.
	 * 默认 "false".
	 * <p>当模式验证处于活动状态时, 通常不需要这样做.
	 * 但是, 如果没有验证, 必须将其切换为“true”才能正确处理模式命名空间.
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * 返回XML解析器是否应该支持XML命名空间.
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * 指定要使用的 {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 * <p>默认实现是 {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}, 表现出快速失败的行为.
	 * 外部工具可以提供另一种实现, 用于整理错误和警告, 以便在工具UI中显示.
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * 指定要使用的{@link ReaderEventListener}.
	 * <p>默认实现是EmptyReaderEventListener, 它会丢弃每个事件通知.
	 * 外部工具可以提供另一种实现来监视BeanFactory中注册的组件.
	 */
	public void setEventListener(ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
	}

	/**
	 * 指定要使用的{@link SourceExtractor}.
	 * <p>默认实现是{@link NullSourceExtractor}, 它只返回{@code null}作为源对象.
	 * 这意味着 - 在正常运行执行期间 - 没有其他源元数据附加到bean配置元数据.
	 */
	public void setSourceExtractor(SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
	}

	/**
	 * 指定要使用的{@link NamespaceHandlerResolver}.
	 * <p>如果未指定, 则将通过 {@link #createDefaultNamespaceHandlerResolver()}创建默认实例.
	 */
	public void setNamespaceHandlerResolver(NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * 指定要使用的{@link DocumentLoader}.
	 * <p>默认实现是{@link DefaultDocumentLoader}, 它使用JAXP加载{@link Document}实例.
	 */
	public void setDocumentLoader(DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
	}

	/**
	 * 设置要用于解析的SAX实体解析器.
	 * <p>默认情况下, 将使用{@link ResourceEntityResolver}.
	 * 可以覆盖自定义实体解析, 例如相对于某些特定的基本路径.
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * 返回要使用的EntityResolver, 如果没有指定, 则构建默认解析器.
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// Determine default EntityResolver to use.
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			}
			else {
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * 设置{@code org.xml.sax.ErrorHandler}接口的实现, 以自定义处理XML解析错误和警告.
	 * <p>如果未设置, 则使用默认的SimpleSaxErrorHandler, 它只使用视图类的记录器实例记录警告, 并重新抛出错误以停止XML转换.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 指定要使用的{@link BeanDefinitionDocumentReader}实现, 负责实际读取XML bean定义文档.
	 * <p>默认是 {@link DefaultBeanDefinitionDocumentReader}.
	 * 
	 * @param documentReaderClass 所需的BeanDefinitionDocumentReader实现类
	 */
	public void setDocumentReaderClass(Class<?> documentReaderClass) {
		if (documentReaderClass == null || !BeanDefinitionDocumentReader.class.isAssignableFrom(documentReaderClass)) {
			throw new IllegalArgumentException(
					"documentReaderClass must be an implementation of the BeanDefinitionDocumentReader interface");
		}
		this.documentReaderClass = documentReaderClass;
	}


	/**
	 * 从指定的XML文件加载bean定义.
	 * 
	 * @param resource XML文件的资源描述符
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的XML文件加载bean定义.
	 * 
	 * @param encodedResource XML文件的资源描述符, 允许指定用于解析文件的编码
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isInfoEnabled()) {
			logger.info("Loading XML bean definitions from " + encodedResource);
		}

		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
		if (currentResources == null) {
			currentResources = new HashSet<EncodedResource>(4);
			this.resourcesCurrentlyBeingLoaded.set(currentResources);
		}
		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}
		try {
			InputStream inputStream = encodedResource.getResource().getInputStream();
			try {
				InputSource inputSource = new InputSource(inputStream);
				if (encodedResource.getEncoding() != null) {
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * 从指定的XML文件加载bean定义.
	 * 
	 * @param inputSource 要读取的SAX InputSource
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * 从指定的XML文件加载bean定义.
	 * 
	 * @param inputSource 要读取的SAX InputSource
	 * @param resourceDescription 资源的描述 (can be {@code null} or empty)
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	public int loadBeanDefinitions(InputSource inputSource, String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * 实际从指定的XML文件加载bean定义.
	 * 
	 * @param inputSource 要读取的SAX InputSource
	 * @param resource XML文件的资源描述符
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {
		try {
			Document doc = doLoadDocument(inputSource, resource);
			return registerBeanDefinitions(doc, resource);
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * 实际上使用配置的DocumentLoader加载指定的文档.
	 * 
	 * @param inputSource 要读取的SAX InputSource
	 * @param resource XML文件的资源描述符
	 * 
	 * @return the DOM Document
	 * @throws Exception 从DocumentLoader抛出时
	 */
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}

	/**
	 * 确定指定的{@link Resource}的验证模式.
	 * 如果未配置显式验证模式, 则验证模式将从给定资源中获取{@link #detectValidationMode}.
	 * <p>如果您希望完全控制验证模式, 请覆盖此方法, 即使设置了{@link #VALIDATION_AUTO}以外的其他内容.
	 */
	protected int getValidationModeForResource(Resource resource) {
		int validationModeToUse = getValidationMode();
		if (validationModeToUse != VALIDATION_AUTO) {
			return validationModeToUse;
		}
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}
		// Hmm, we didn't get a clear indication... Let's assume XSD,
		// 因为在检测停止之前显然没有找到DTD声明 (在找到文档的根标记之前).
		return VALIDATION_XSD;
	}

	/**
	 * 检测对提供的{@link Resource}标识的XML文件执行哪种验证.
	 * 如果文件具有{@code DOCTYPE}定义, 则使用DTD验证, 否则假定XSD验证.
	 * <p>如果要自定义{@link #VALIDATION_AUTO}模式的解析, 请覆盖此方法.
	 */
	protected int detectValidationMode(Resource resource) {
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"Passed-in Resource [" + resource + "] contains an open stream: " +
					"cannot determine validation mode automatically. Either pass in a Resource " +
					"that is able to create fresh streams, or explicitly specify the validationMode " +
					"on your XmlBeanDefinitionReader instance.");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
					"Did you attempt to load directly from a SAX InputSource without specifying the " +
					"validationMode on your XmlBeanDefinitionReader instance?", ex);
		}

		try {
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
					resource + "]: an error occurred whilst reading from the InputStream.", ex);
		}
	}

	/**
	 * 注册给定DOM文档中包含的bean定义.
	 * 由{@code loadBeanDefinitions}调用.
	 * <p>创建解析器类的新实例, 并在其上调用{@code registerBeanDefinitions}.
	 * 
	 * @param doc DOM文档
	 * @param resource 资源描述符 (用于上下文信息)
	 * 
	 * @return 找到的bean定义数量
	 * @throws BeanDefinitionStoreException 解析错误
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		int countBefore = getRegistry().getBeanDefinitionCount();
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * 创建{@link BeanDefinitionDocumentReader}, 以用于从XML文档中实际读取bean定义.
	 * <p>默认实现实例化指定的 "documentReaderClass".
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanDefinitionDocumentReader.class.cast(BeanUtils.instantiateClass(this.documentReaderClass));
	}

	/**
	 * 创建{@link XmlReaderContext}, 以传递给文档读取器.
	 */
	public XmlReaderContext createReaderContext(Resource resource) {
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, getNamespaceHandlerResolver());
	}

	/**
	 * 延迟创建一个默认的NamespaceHandlerResolver, 如果之前没有设置的话.
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * 如果未指定, 则创建使用的{@link NamespaceHandlerResolver}的默认实现.
	 * <p>默认实现返回{@link DefaultNamespaceHandlerResolver}实例.
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
	}

}
