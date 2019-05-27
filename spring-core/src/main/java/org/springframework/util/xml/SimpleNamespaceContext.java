package org.springframework.util.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.springframework.util.Assert;

/**
 * 简单的{@code javax.xml.namespace.NamespaceContext}实现.
 * 遵循标准{@code NamespaceContext}约定, 可通过{@code java.util.Map}或{@code java.util.Properties}对象加载
 */
public class SimpleNamespaceContext implements NamespaceContext {

	private final Map<String, String> prefixToNamespaceUri = new HashMap<String, String>();

	private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap<String, Set<String>>();

	private String defaultNamespaceUri = "";


	@Override
	public String getNamespaceURI(String prefix) {
		Assert.notNull(prefix, "No prefix given");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		}
		else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			return this.defaultNamespaceUri;
		}
		else if (this.prefixToNamespaceUri.containsKey(prefix)) {
			return this.prefixToNamespaceUri.get(prefix);
		}
		return "";
	}

	@Override
	public String getPrefix(String namespaceUri) {
		Set<String> prefixes = getPrefixesSet(namespaceUri);
		return (!prefixes.isEmpty() ? prefixes.iterator().next() : null);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceUri) {
		return getPrefixesSet(namespaceUri).iterator();
	}

	private Set<String> getPrefixesSet(String namespaceUri) {
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (this.defaultNamespaceUri.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX);
		}
		else if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XML_NS_PREFIX);
		}
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
			return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE);
		}
		else {
			Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
			return (prefixes != null ?  Collections.unmodifiableSet(prefixes) : Collections.<String>emptySet());
		}
	}


	/**
	 * 设置此命名空间上下文的绑定.
	 * 提供的映射必须由字符串键值对组成.
	 */
	public void setBindings(Map<String, String> bindings) {
		for (Map.Entry<String, String> entry : bindings.entrySet()) {
			bindNamespaceUri(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * 将给定的命名空间绑定为默认命名空间.
	 * 
	 * @param namespaceUri 命名空间uri
	 */
	public void bindDefaultNamespaceUri(String namespaceUri) {
		bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
	}

	/**
	 * 将给定的前缀绑定到给定的命名空间.
	 * 
	 * @param prefix 命名空间前缀
	 * @param namespaceUri 命名空间uri
	 */
	public void bindNamespaceUri(String prefix, String namespaceUri) {
		Assert.notNull(prefix, "No prefix given");
		Assert.notNull(namespaceUri, "No namespaceUri given");
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = namespaceUri;
		}
		else {
			this.prefixToNamespaceUri.put(prefix, namespaceUri);
			Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
			if (prefixes == null) {
				prefixes = new LinkedHashSet<String>();
				this.namespaceUriToPrefixes.put(namespaceUri, prefixes);
			}
			prefixes.add(prefix);
		}
	}

	/**
	 * 从此上下文中删除给定的前缀.
	 * 
	 * @param prefix 要删除的前缀
	 */
	public void removeBinding(String prefix) {
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
			this.defaultNamespaceUri = "";
		}
		else if (prefix != null) {
			String namespaceUri = this.prefixToNamespaceUri.remove(prefix);
			if (namespaceUri != null) {
				Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
				if (prefixes != null) {
					prefixes.remove(prefix);
					if (prefixes.isEmpty()) {
						this.namespaceUriToPrefixes.remove(namespaceUri);
					}
				}
			}
		}
	}

	/**
	 * 删除所有已声明的前缀.
	 */
	public void clear() {
		this.prefixToNamespaceUri.clear();
		this.namespaceUriToPrefixes.clear();
	}

	/**
	 * 返回所有已声明的前缀.
	 */
	public Iterator<String> getBoundPrefixes() {
		return this.prefixToNamespaceUri.keySet().iterator();
	}

}
