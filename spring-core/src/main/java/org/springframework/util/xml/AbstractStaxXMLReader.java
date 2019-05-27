package org.springframework.util.xml;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

import org.springframework.util.StringUtils;

/**
 * 使用StAX作为基础的SAX {@code XMLReader}实现的抽象基类.
 */
abstract class AbstractStaxXMLReader extends AbstractXMLReader {

	private static final String NAMESPACES_FEATURE_NAME = "http://xml.org/sax/features/namespaces";

	private static final String NAMESPACE_PREFIXES_FEATURE_NAME = "http://xml.org/sax/features/namespace-prefixes";

	private static final String IS_STANDALONE_FEATURE_NAME = "http://xml.org/sax/features/is-standalone";


	private boolean namespacesFeature = true;

	private boolean namespacePrefixesFeature = false;

	private Boolean isStandalone;

	private final Map<String, String> namespaces = new LinkedHashMap<String, String>();


	@Override
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (NAMESPACES_FEATURE_NAME.equals(name)) {
			return this.namespacesFeature;
		}
		else if (NAMESPACE_PREFIXES_FEATURE_NAME.equals(name)) {
			return this.namespacePrefixesFeature;
		}
		else if (IS_STANDALONE_FEATURE_NAME.equals(name)) {
			if (this.isStandalone != null) {
				return this.isStandalone;
			}
			else {
				throw new SAXNotSupportedException("startDocument() callback not completed yet");
			}
		}
		else {
			return super.getFeature(name);
		}
	}

	@Override
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (NAMESPACES_FEATURE_NAME.equals(name)) {
			this.namespacesFeature = value;
		}
		else if (NAMESPACE_PREFIXES_FEATURE_NAME.equals(name)) {
			this.namespacePrefixesFeature = value;
		}
		else {
			super.setFeature(name, value);
		}
	}

	protected void setStandalone(boolean standalone) {
		this.isStandalone = standalone;
	}

	/**
	 * 指示是否已打开SAX功能{@code http://xml.org/sax/features/namespaces}.
	 */
	protected boolean hasNamespacesFeature() {
		return this.namespacesFeature;
	}

	/**
	 * 指示是否已打开SAX功能{@code http://xml.org/sax/features/namespaces-prefixes}.
	 */
	protected boolean hasNamespacePrefixesFeature() {
		return this.namespacePrefixesFeature;
	}

	/**
	 * 将{@code QName}转换为DOM和SAX使用的限定名称.
	 * 如果设置了前缀, 则返回的字符串格式为{@code prefix:localName}, 否则返回{@code localName}.
	 * 
	 * @param qName {@code QName}
	 * 
	 * @return 限定名称
	 */
	protected String toQualifiedName(QName qName) {
		String prefix = qName.getPrefix();
		if (!StringUtils.hasLength(prefix)) {
			return qName.getLocalPart();
		}
		else {
			return prefix + ":" + qName.getLocalPart();
		}
	}


	/**
	 * 解析在构造时传递的StAX XML读取器.
	 * <p><b>NOTE:</b>: 给定的{@code InputSource}不会被读取, 但会被忽略.
	 * 
	 * @param ignored is ignored
	 * 
	 * @throws SAXException SAX异常, 可能包装{@code XMLStreamException}
	 */
	@Override
	public final void parse(InputSource ignored) throws SAXException {
		parse();
	}

	/**
	 * 解析在构造时传递的StAX XML读取器.
	 * <p><b>NOTE:</b>: 不读取给定的系统标识符, 但忽略该标识符.
	 * 
	 * @param ignored is ignored
	 * 
	 * @throws SAXException SAX异常, 可能包装{@code XMLStreamException}
	 */
	@Override
	public final void parse(String ignored) throws SAXException {
		parse();
	}

	private void parse() throws SAXException {
		try {
			parseInternal();
		}
		catch (XMLStreamException ex) {
			Locator locator = null;
			if (ex.getLocation() != null) {
				locator = new StaxLocator(ex.getLocation());
			}
			SAXParseException saxException = new SAXParseException(ex.getMessage(), locator, ex);
			if (getErrorHandler() != null) {
				getErrorHandler().fatalError(saxException);
			}
			else {
				throw saxException;
			}
		}
	}

	/**
	 * 解析在构造时传递的StAX读取器的模板方法.
	 */
	protected abstract void parseInternal() throws SAXException, XMLStreamException;


	/**
	 * 启动给定前缀的前缀映射.
	 */
	protected void startPrefixMapping(String prefix, String namespace) throws SAXException {
		if (getContentHandler() != null) {
			if (prefix == null) {
				prefix = "";
			}
			if (!StringUtils.hasLength(namespace)) {
				return;
			}
			if (!namespace.equals(this.namespaces.get(prefix))) {
				getContentHandler().startPrefixMapping(prefix, namespace);
				this.namespaces.put(prefix, namespace);
			}
		}
	}

	/**
	 * 结束给定前缀的前缀映射.
	 */
	protected void endPrefixMapping(String prefix) throws SAXException {
		if (getContentHandler() != null) {
			if (this.namespaces.containsKey(prefix)) {
				getContentHandler().endPrefixMapping(prefix);
				this.namespaces.remove(prefix);
			}
		}
	}


	/**
	 * 基于给定的StAX {@code Location}的{@code Locator}接口的实现.
	 */
	private static class StaxLocator implements Locator {

		private final Location location;

		public StaxLocator(Location location) {
			this.location = location;
		}

		@Override
		public String getPublicId() {
			return this.location.getPublicId();
		}

		@Override
		public String getSystemId() {
			return this.location.getSystemId();
		}

		@Override
		public int getLineNumber() {
			return this.location.getLineNumber();
		}

		@Override
		public int getColumnNumber() {
			return this.location.getColumnNumber();
		}
	}
}
