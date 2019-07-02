package org.springframework.test.util;

import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import static org.hamcrest.MatcherAssert.*;

/**
 * 用于XML内容断言的辅助类.
 */
public class XmlExpectationsHelper {

	/**
	 * 将内容解析为{@link Node}并应用{@link Matcher}.
	 */
	public void assertNode(String content, Matcher<? super Node> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", document, matcher);
	}

	private Document parseXmlString(String xml) throws Exception  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new StringReader(xml));
		return documentBuilder.parse(inputSource);
	}

	/**
	 * 将内容解析为{@link DOMSource}并应用{@link Matcher}.
	 */
	public void assertSource(String content, Matcher<? super Source> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", new DOMSource(document), matcher);
	}

	/**
	 * 将预期和实际内容字符串解析为XML, 并断言两者是"相似的" -- i.e. 它们包含相同的元素和属性, 而不管顺序如何.
	 * <p>使用此方法假定
	 * <a href="http://xmlunit.sourceforge.net/">XMLUnit<a/>库可用.
	 * 
	 * @param expected 预期的XML内容
	 * @param actual 实际的XML内容
	 */
	public void assertXmlEqual(String expected, String actual) throws Exception {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreAttributeOrder(true);

		Document control = XMLUnit.buildControlDocument(expected);
		Document test = XMLUnit.buildTestDocument(actual);
		Diff diff = new Diff(control, test);
		if (!diff.similar()) {
			AssertionErrors.fail("Body content " + diff.toString());
		}
	}

}
