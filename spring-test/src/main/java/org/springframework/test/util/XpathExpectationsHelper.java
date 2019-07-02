package org.springframework.test.util;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleNamespaceContext;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 用于通过XPath表达式应用断言的辅助类.
 */
public class XpathExpectationsHelper {

	private final String expression;

	private final XPathExpression xpathExpression;

	private final boolean hasNamespaces;


	/**
	 * @param expression XPath表达式
	 * @param namespaces XPath表达式中引用的XML名称空间, 或{@code null}
	 * @param args 使用{@link String#format(String, Object...)}中定义的格式说明符参数化XPath表达式的参数
	 * 
	 * @throws XPathExpressionException 如果表达式编译失败
	 */
	public XpathExpectationsHelper(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.expression = String.format(expression, args);
		this.xpathExpression = compileXpathExpression(this.expression, namespaces);
		this.hasNamespaces = !CollectionUtils.isEmpty(namespaces);
	}


	private XPathExpression compileXpathExpression(String expression, Map<String, String> namespaces)
			throws XPathExpressionException {

		SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
		namespaceContext.setBindings(namespaces != null ? namespaces : Collections.<String, String> emptyMap());
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(namespaceContext);
		return xpath.compile(expression);
	}

	/**
	 * 返回已编译的XPath表达式.
	 */
	protected XPathExpression getXpathExpression() {
		return this.xpathExpression;
	}

	/**
	 * 解析内容, 将XPath表达式计算为{@link Node}, 并使用给定的{@code Matcher<Node>}断言它.
	 */
	public void assertNode(byte[] content, String encoding, final Matcher<? super Node> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertThat("XPath " + this.expression, node, matcher);
	}

	/**
	 * 将给定的XML内容解析为{@link Document}.
	 * 
	 * @param xml 要解析的内容
	 * @param encoding 可选内容编码, 如果作为元数据提供 (e.g. 在HTTP header中)
	 * 
	 * @return 解析的文件
	 */
	protected Document parseXmlByteArray(byte[] xml, String encoding) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(this.hasNamespaces);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new ByteArrayInputStream(xml));
		if (StringUtils.hasText(encoding)) {
			inputSource.setEncoding(encoding);
		}
		return documentBuilder.parse(inputSource);
	}

	/**
	 * 将XPath表达式应用于给定文档.
	 * 
	 * @throws XPathExpressionException 如果表达式评估失败
	 */
	@SuppressWarnings("unchecked")
	protected <T> T evaluateXpath(Document document, QName evaluationType, Class<T> expectedClass)
			throws XPathExpressionException {

		return (T) getXpathExpression().evaluate(document, evaluationType);
	}

	/**
	 * 应用XPath表达式并断言结果内容存在.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void exists(byte[] content, String encoding) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertTrue("XPath " + this.expression + " does not exist", node != null);
	}

	/**
	 * 应用XPath表达式并断言结果内容不存在.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void doesNotExist(byte[] content, String encoding) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertTrue("XPath " + this.expression + " exists", node == null);
	}

	/**
	 * 应用XPath表达式, 并使用给定的Hamcrest匹配器断言生成的内容.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertNodeCount(byte[] content, String encoding, Matcher<Integer> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		assertThat("nodeCount for XPath " + this.expression, nodeList.getLength(), matcher);
	}

	/**
	 * 应用XPath表达式并将结果内容断言为整数.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertNodeCount(byte[] content, String encoding, int expectedCount) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		assertEquals("nodeCount for XPath " + this.expression, expectedCount, nodeList.getLength());
	}

	/**
	 * 应用XPath表达式并使用给定的Hamcrest匹配器断言生成的内容.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertString(byte[] content, String encoding, Matcher<? super String> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String result = evaluateXpath(document,  XPathConstants.STRING, String.class);
		assertThat("XPath " + this.expression, result, matcher);
	}

	/**
	 * 应用XPath表达式并将结果内容断言为String.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertString(byte[] content, String encoding, String expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String actual = evaluateXpath(document,  XPathConstants.STRING, String.class);
		assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * 应用XPath表达式, 并使用给定的Hamcrest匹配器断言生成的内容.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertNumber(byte[] content, String encoding, Matcher<? super Double> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Double result = evaluateXpath(document, XPathConstants.NUMBER, Double.class);
		assertThat("XPath " + this.expression, result, matcher);
	}

	/**
	 * 应用XPath表达式并将结果内容断言为Double.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertNumber(byte[] content, String encoding, Double expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Double actual = evaluateXpath(document, XPathConstants.NUMBER, Double.class);
		assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * 应用XPath表达式, 并将结果内容断言为布尔值.
	 * 
	 * @throws Exception 如果内容解析或表达式评估失败
	 */
	public void assertBoolean(byte[] content, String encoding, boolean expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String actual = evaluateXpath(document, XPathConstants.STRING, String.class);
		assertEquals("XPath " + this.expression, expectedValue, Boolean.parseBoolean(actual));
	}

}
