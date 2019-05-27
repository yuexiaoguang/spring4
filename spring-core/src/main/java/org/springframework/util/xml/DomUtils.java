package org.springframework.util.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;

import org.springframework.util.Assert;

/**
 * 使用DOM API的便捷方法, 特别是用于处理DOM节点和DOM元素.
 */
public abstract class DomUtils {

	/**
	 * 检索给定DOM元素中与任何给定元素名称匹配的所有子元素.
	 * 仅查看给定元素的直接子级别; 不要进一步深入 (与DOM API的{@code getElementsByTagName}方法相比).
	 * 
	 * @param ele 要分析的DOM元素
	 * @param childEleNames 要查找的子元素名称
	 * 
	 * @return 子{@code org.w3c.dom.Element}实例列表
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String... childEleNames) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleNames, "Element names collection must not be null");
		List<String> childEleNameList = Arrays.asList(childEleNames);
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleNameList)) {
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 检索给定DOM元素的与给定元素名称匹配的所有子元素.
	 * 只查看给定元素的直接子级别; 不要进一步深入 (与DOM API的{@code getElementsByTagName}方法相比).
	 * 
	 * @param ele 要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * 
	 * @return 子{@code org.w3c.dom.Element}实例列表
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String childEleName) {
		return getChildElementsByTagName(ele, new String[] {childEleName});
	}

	/**
	 * 返回由其名称标识的第一个子元素的实用程序方法.
	 * 
	 * @param ele 要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * 
	 * @return {@code org.w3c.dom.Element}实例, 或{@code null}
	 */
	public static Element getChildElementByTagName(Element ele, String childEleName) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleName, "Element name must not be null");
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleName)) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * 返回由其名称标识的第一个子元素值的实用程序方法.
	 * 
	 * @param ele 要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * 
	 * @return 提取的文本值, 或{@code null} 如果找不到子元素
	 */
	public static String getChildElementValueByTagName(Element ele, String childEleName) {
		Element child = getChildElementByTagName(ele, childEleName);
		return (child != null ? getTextValue(child) : null);
	}

	/**
	 * 检索给定DOM元素的所有子元素
	 * 
	 * @param ele 要分析的DOM元素
	 * 
	 * @return 子{@code org.w3c.dom.Element}实例列表
	 */
	public static List<Element> getChildElements(Element ele) {
		Assert.notNull(ele, "Element must not be null");
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<Element>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 从给定的DOM元素中提取文本值, 忽略XML注释.
	 * <p>将所有CharacterData节点和EntityReference节点追加到单个String值中, 不包括Comment节点.
	 * 仅公开实际的用户指定文本, 没有任何类型的默认值.
	 */
	public static String getTextValue(Element valueEle) {
		Assert.notNull(valueEle, "Element must not be null");
		StringBuilder sb = new StringBuilder();
		NodeList nl = valueEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
				sb.append(item.getNodeValue());
			}
		}
		return sb.toString();
	}

	/**
	 * 命名空间感知相等比较.
	 * 如果{@link Node#getLocalName}或{@link Node#getNodeName}等于{@code desiredName}, 则返回{@code true}, 否则返回{@code false}.
	 */
	public static boolean nodeNameEquals(Node node, String desiredName) {
		Assert.notNull(node, "Node must not be null");
		Assert.notNull(desiredName, "Desired name must not be null");
		return nodeNameMatch(node, desiredName);
	}

	/**
	 * 返回一个SAX {@code ContentHandler}, 它将回调调用转换为DOM {@code Node}.
	 * 
	 * @param node 要将事件发布到的节点
	 * 
	 * @return 内容处理器
	 */
	public static ContentHandler createContentHandler(Node node) {
		return new DomContentHandler(node);
	}

	/**
	 * 根据给定的所需名称匹配给定节点的名称和本地名称.
	 */
	private static boolean nodeNameMatch(Node node, String desiredName) {
		return (desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName()));
	}

	/**
	 * 根据给定的所需名称匹配给定节点的名称和本地名称.
	 */
	private static boolean nodeNameMatch(Node node, Collection<?> desiredNames) {
		return (desiredNames.contains(node.getNodeName()) || desiredNames.contains(node.getLocalName()));
	}

}
