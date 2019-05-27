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
 * A helper class for assertions on XML content.
 */
public class XmlExpectationsHelper {

	/**
	 * Parse the content as {@link Node} and apply a {@link Matcher}.
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
	 * Parse the content as {@link DOMSource} and apply a {@link Matcher}.
	 */
	public void assertSource(String content, Matcher<? super Source> matcher) throws Exception {
		Document document = parseXmlString(content);
		assertThat("Body content", new DOMSource(document), matcher);
	}

	/**
	 * Parse the expected and actual content strings as XML and assert that the
	 * two are "similar" -- i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this method assumes the
	 * <a href="http://xmlunit.sourceforge.net/">XMLUnit<a/> library is available.
	 * @param expected the expected XML content
	 * @param actual the actual XML content
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Object...)
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers#xpath(String, Map, Object...)
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
