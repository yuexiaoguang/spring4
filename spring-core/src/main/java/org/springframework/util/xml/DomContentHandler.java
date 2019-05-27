package org.springframework.util.xml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.springframework.util.Assert;

/**
 * SAX {@code ContentHandler}将回调调用转换为 DOM {@code Node}.
 */
class DomContentHandler implements ContentHandler {

	private final Document document;

	private final List<Element> elements = new ArrayList<Element>();

	private final Node node;

	/**
	 * @param node 要将事件发布到的节点
	 */
	DomContentHandler(Node node) {
		Assert.notNull(node, "node must not be null");
		this.node = node;
		if (node instanceof Document) {
			document = (Document) node;
		}
		else {
			document = node.getOwnerDocument();
		}
		Assert.notNull(document, "document must not be null");
	}

	private Node getParent() {
		if (!elements.isEmpty()) {
			return elements.get(elements.size() - 1);
		}
		else {
			return node;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Node parent = getParent();
		Element element = document.createElementNS(uri, qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			String attrUri = attributes.getURI(i);
			String attrQname = attributes.getQName(i);
			String value = attributes.getValue(i);
			if (!attrQname.startsWith("xmlns")) {
				element.setAttributeNS(attrUri, attrQname, value);
			}
		}
		element = (Element) parent.appendChild(element);
		elements.add(element);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		elements.remove(elements.size() - 1);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String data = new String(ch, start, length);
		Node parent = getParent();
		Node lastChild = parent.getLastChild();
		if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
			((Text) lastChild).appendData(data);
		}
		else {
			Text text = document.createTextNode(data);
			parent.appendChild(text);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		Node parent = getParent();
		ProcessingInstruction pi = document.createProcessingInstruction(target, data);
		parent.appendChild(pi);
	}

	/*
	 * Unsupported
	 */

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}
}
