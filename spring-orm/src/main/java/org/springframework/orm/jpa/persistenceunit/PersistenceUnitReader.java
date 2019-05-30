package org.springframework.orm.jpa.persistenceunit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.util.xml.SimpleSaxErrorHandler;

/**
 * 内部帮助类， 用于读取符合JPA的{@code persistence.xml}文件.
 */
final class PersistenceUnitReader {

	private static final String PERSISTENCE_VERSION = "version";

	private static final String PERSISTENCE_UNIT = "persistence-unit";

	private static final String UNIT_NAME = "name";

	private static final String MAPPING_FILE_NAME = "mapping-file";

	private static final String JAR_FILE_URL = "jar-file";

	private static final String MANAGED_CLASS_NAME = "class";

	private static final String PROPERTIES = "properties";

	private static final String PROVIDER = "provider";

	private static final String TRANSACTION_TYPE = "transaction-type";

	private static final String JTA_DATA_SOURCE = "jta-data-source";

	private static final String NON_JTA_DATA_SOURCE = "non-jta-data-source";

	private static final String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";

	private static final String SHARED_CACHE_MODE = "shared-cache-mode";

	private static final String VALIDATION_MODE = "validation-mode";

	private static final String META_INF = "META-INF";


	private static final Log logger = LogFactory.getLog(PersistenceUnitReader.class);

	private final ResourcePatternResolver resourcePatternResolver;

	private final DataSourceLookup dataSourceLookup;


	/**
	 * @param resourcePatternResolver 用于加载资源的ResourcePatternResolver
	 * @param dataSourceLookup 用于解析{@code persistence.xml}文件中的DataSource名称的DataSourceLookup
	 */
	public PersistenceUnitReader(ResourcePatternResolver resourcePatternResolver, DataSourceLookup dataSourceLookup) {
		Assert.notNull(resourcePatternResolver, "ResourceLoader must not be null");
		Assert.notNull(dataSourceLookup, "DataSourceLookup must not be null");
		this.resourcePatternResolver = resourcePatternResolver;
		this.dataSourceLookup = dataSourceLookup;
	}


	/**
	 * 解析并构建在指定的XML文件中定义的所有持久化单元信息.
	 * 
	 * @param persistenceXmlLocation 资源位置 (可以是模式)
	 * 
	 * @return 生成的PersistenceUnitInfo实例
	 */
	public SpringPersistenceUnitInfo[] readPersistenceUnitInfos(String persistenceXmlLocation) {
		return readPersistenceUnitInfos(new String[] {persistenceXmlLocation});
	}

	/**
	 * 解析并构建给定XML文件中定义的所有持久化单元信息.
	 * 
	 * @param persistenceXmlLocations 资源位置 (可以是模式)
	 * 
	 * @return 生成的PersistenceUnitInfo实例
	 */
	public SpringPersistenceUnitInfo[] readPersistenceUnitInfos(String[] persistenceXmlLocations) {
		ErrorHandler handler = new SimpleSaxErrorHandler(logger);
		List<SpringPersistenceUnitInfo> infos = new LinkedList<SpringPersistenceUnitInfo>();
		String resourceLocation = null;
		try {
			for (String location : persistenceXmlLocations) {
				Resource[] resources = this.resourcePatternResolver.getResources(location);
				for (Resource resource : resources) {
					resourceLocation = resource.toString();
					InputStream stream = resource.getInputStream();
					try {
						Document document = buildDocument(handler, stream);
						parseDocument(resource, document, infos);
					}
					finally {
						stream.close();
					}
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Cannot parse persistence unit from " + resourceLocation, ex);
		}
		catch (SAXException ex) {
			throw new IllegalArgumentException("Invalid XML in persistence unit from " + resourceLocation, ex);
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalArgumentException("Internal error parsing persistence unit from " + resourceLocation);
		}

		return infos.toArray(new SpringPersistenceUnitInfo[infos.size()]);
	}


	/**
	 * 验证给定的流并返回有效的DOM文档以进行解析.
	 */
	protected Document buildDocument(ErrorHandler handler, InputStream stream)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder parser = dbf.newDocumentBuilder();
		parser.setErrorHandler(handler);
		return parser.parse(stream);
	}


	/**
	 * 解析经过验证的文档, 并将条目添加到给定的单元信息列表中.
	 */
	protected List<SpringPersistenceUnitInfo> parseDocument(
			Resource resource, Document document, List<SpringPersistenceUnitInfo> infos) throws IOException {

		Element persistence = document.getDocumentElement();
		String version = persistence.getAttribute(PERSISTENCE_VERSION);
		URL rootUrl = determinePersistenceUnitRootUrl(resource);

		List<Element> units = DomUtils.getChildElementsByTagName(persistence, PERSISTENCE_UNIT);
		for (Element unit : units) {
			infos.add(parsePersistenceUnitInfo(unit, version, rootUrl));
		}

		return infos;
	}

	/**
	 * 解析单元信息DOM元素.
	 */
	protected SpringPersistenceUnitInfo parsePersistenceUnitInfo(Element persistenceUnit, String version, URL rootUrl)
			throws IOException {

		SpringPersistenceUnitInfo unitInfo = new SpringPersistenceUnitInfo();

		// set JPA version (1.0 or 2.0)
		unitInfo.setPersistenceXMLSchemaVersion(version);

		// set persistence unit root URL
		unitInfo.setPersistenceUnitRootUrl(rootUrl);

		// set unit name
		unitInfo.setPersistenceUnitName(persistenceUnit.getAttribute(UNIT_NAME).trim());

		// set transaction type
		String txType = persistenceUnit.getAttribute(TRANSACTION_TYPE).trim();
		if (StringUtils.hasText(txType)) {
			unitInfo.setTransactionType(PersistenceUnitTransactionType.valueOf(txType));
		}

		// evaluate data sources
		String jtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, JTA_DATA_SOURCE);
		if (StringUtils.hasText(jtaDataSource)) {
			unitInfo.setJtaDataSource(this.dataSourceLookup.getDataSource(jtaDataSource.trim()));
		}

		String nonJtaDataSource = DomUtils.getChildElementValueByTagName(persistenceUnit, NON_JTA_DATA_SOURCE);
		if (StringUtils.hasText(nonJtaDataSource)) {
			unitInfo.setNonJtaDataSource(this.dataSourceLookup.getDataSource(nonJtaDataSource.trim()));
		}

		// provider
		String provider = DomUtils.getChildElementValueByTagName(persistenceUnit, PROVIDER);
		if (StringUtils.hasText(provider)) {
			unitInfo.setPersistenceProviderClassName(provider.trim());
		}

		// exclude unlisted classes
		Element excludeUnlistedClasses = DomUtils.getChildElementByTagName(persistenceUnit, EXCLUDE_UNLISTED_CLASSES);
		if (excludeUnlistedClasses != null) {
			String excludeText = DomUtils.getTextValue(excludeUnlistedClasses);
			unitInfo.setExcludeUnlistedClasses(!StringUtils.hasText(excludeText) || Boolean.valueOf(excludeText));
		}

		// set JPA 2.0 shared cache mode
		String cacheMode = DomUtils.getChildElementValueByTagName(persistenceUnit, SHARED_CACHE_MODE);
		if (StringUtils.hasText(cacheMode)) {
			unitInfo.setSharedCacheMode(SharedCacheMode.valueOf(cacheMode));
		}

		// set JPA 2.0 validation mode
		String validationMode = DomUtils.getChildElementValueByTagName(persistenceUnit, VALIDATION_MODE);
		if (StringUtils.hasText(validationMode)) {
			unitInfo.setValidationMode(ValidationMode.valueOf(validationMode));
		}

		parseProperties(persistenceUnit, unitInfo);
		parseManagedClasses(persistenceUnit, unitInfo);
		parseMappingFiles(persistenceUnit, unitInfo);
		parseJarFiles(persistenceUnit, unitInfo);

		return unitInfo;
	}

	/**
	 * 解析{@code property} XML元素.
	 */
	protected void parseProperties(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		Element propRoot = DomUtils.getChildElementByTagName(persistenceUnit, PROPERTIES);
		if (propRoot == null) {
			return;
		}
		List<Element> properties = DomUtils.getChildElementsByTagName(propRoot, "property");
		for (Element property : properties) {
			String name = property.getAttribute("name");
			String value = property.getAttribute("value");
			unitInfo.addProperty(name, value);
		}
	}

	/**
	 * 解析{@code class} XML元素.
	 */
	protected void parseManagedClasses(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		List<Element> classes = DomUtils.getChildElementsByTagName(persistenceUnit, MANAGED_CLASS_NAME);
		for (Element element : classes) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value))
				unitInfo.addManagedClassName(value);
		}
	}

	/**
	 * 解析{@code mapping-file} XML元素.
	 */
	protected void parseMappingFiles(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) {
		List<Element> files = DomUtils.getChildElementsByTagName(persistenceUnit, MAPPING_FILE_NAME);
		for (Element element : files) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value)) {
				unitInfo.addMappingFileName(value);
			}
		}
	}

	/**
	 * 解析{@code jar-file} XML元素.
	 */
	protected void parseJarFiles(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) throws IOException {
		List<Element> jars = DomUtils.getChildElementsByTagName(persistenceUnit, JAR_FILE_URL);
		for (Element element : jars) {
			String value = DomUtils.getTextValue(element).trim();
			if (StringUtils.hasText(value)) {
				Resource[] resources = this.resourcePatternResolver.getResources(value);
				boolean found = false;
				for (Resource resource : resources) {
					if (resource.exists()) {
						found = true;
						unitInfo.addJarFileUrl(resource.getURL());
					}
				}
				if (!found) {
					// 相对于持久化单位根, 根据JPA规范
					URL rootUrl = unitInfo.getPersistenceUnitRootUrl();
					if (rootUrl != null) {
						unitInfo.addJarFileUrl(new URL(rootUrl, value));
					}
					else {
						logger.warn("Cannot resolve jar-file entry [" + value + "] in persistence unit '" +
								unitInfo.getPersistenceUnitName() + "' without root URL");
					}
				}
			}
		}
	}


	/**
	 * 根据给定资源确定持久化单元根URL (指向正在读取的{@code persistence.xml}文件).
	 * 
	 * @param resource 要检查的资源
	 * 
	 * @return 相应的持久化单元根URL
	 * @throws IOException 如果检查失败
	 */
	static URL determinePersistenceUnitRootUrl(Resource resource) throws IOException {
		URL originalURL = resource.getURL();

		// 如果获得存档, 只需返回jar URL (JPA规范中的第6.2节)
		if (ResourceUtils.isJarURL(originalURL)) {
			return ResourceUtils.extractJarFileURL(originalURL);
		}

		// Check META-INF folder
		String urlToString = originalURL.toExternalForm();
		if (!urlToString.contains(META_INF)) {
			if (logger.isInfoEnabled()) {
				logger.info(resource.getFilename() +
						" should be located inside META-INF directory; cannot determine persistence unit root URL for " +
						resource);
			}
			return null;
		}
		if (urlToString.lastIndexOf(META_INF) == urlToString.lastIndexOf('/') - (1 + META_INF.length())) {
			if (logger.isInfoEnabled()) {
				logger.info(resource.getFilename() +
						" is not located in the root of META-INF directory; cannot determine persistence unit root URL for " +
						resource);
			}
			return null;
		}

		String persistenceUnitRoot = urlToString.substring(0, urlToString.lastIndexOf(META_INF));
		if (persistenceUnitRoot.endsWith("/")) {
			persistenceUnitRoot = persistenceUnitRoot.substring(0, persistenceUnitRoot.length() - 1);
		}
		return new URL(persistenceUnitRoot);
	}

}
