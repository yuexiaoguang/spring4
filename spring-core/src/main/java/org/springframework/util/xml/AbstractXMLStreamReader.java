package org.springframework.util.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.util.Assert;

/**
 * {@code XMLStreamReader}的抽象基类.
 */
abstract class AbstractXMLStreamReader implements XMLStreamReader {

	@Override
	public String getElementText() throws XMLStreamException {
		if (getEventType() != XMLStreamConstants.START_ELEMENT) {
			throw new XMLStreamException("parser must be on START_ELEMENT to read next text", getLocation());
		}
		int eventType = next();
		StringBuilder builder = new StringBuilder();
		while (eventType != XMLStreamConstants.END_ELEMENT) {
			if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA ||
					eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
				builder.append(getText());
			}
			else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION ||
					eventType == XMLStreamConstants.COMMENT) {
				// skipping
			}
			else if (eventType == XMLStreamConstants.END_DOCUMENT) {
				throw new XMLStreamException("unexpected end of document when reading element text content",
						getLocation());
			}
			else if (eventType == XMLStreamConstants.START_ELEMENT) {
				throw new XMLStreamException("element text content may not contain START_ELEMENT", getLocation());
			}
			else {
				throw new XMLStreamException("Unexpected event type " + eventType, getLocation());
			}
			eventType = next();
		}
		return builder.toString();
	}

	@Override
	public String getAttributeLocalName(int index) {
		return getAttributeName(index).getLocalPart();
	}

	@Override
	public String getAttributeNamespace(int index) {
		return getAttributeName(index).getNamespaceURI();
	}

	@Override
	public String getAttributePrefix(int index) {
		return getAttributeName(index).getPrefix();
	}

	@Override
	public String getNamespaceURI() {
		int eventType = getEventType();
		if (eventType == XMLStreamConstants.START_ELEMENT || eventType == XMLStreamConstants.END_ELEMENT) {
			return getName().getNamespaceURI();
		}
		else {
			throw new IllegalStateException("parser must be on START_ELEMENT or END_ELEMENT state");
		}
	}

	@Override
	public String getNamespaceURI(String prefix) {
		Assert.notNull(prefix, "No prefix given");
		return getNamespaceContext().getNamespaceURI(prefix);
	}

	@Override
	public boolean hasText() {
		int eventType = getEventType();
		return eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.CHARACTERS ||
				eventType == XMLStreamConstants.COMMENT || eventType == XMLStreamConstants.CDATA ||
				eventType == XMLStreamConstants.ENTITY_REFERENCE;
	}

	@Override
	public String getPrefix() {
		int eventType = getEventType();
		if (eventType == XMLStreamConstants.START_ELEMENT || eventType == XMLStreamConstants.END_ELEMENT) {
			return getName().getPrefix();
		}
		else {
			throw new IllegalStateException("parser must be on START_ELEMENT or END_ELEMENT state");
		}
	}

	@Override
	public boolean hasName() {
		int eventType = getEventType();
		return eventType == XMLStreamConstants.START_ELEMENT || eventType == XMLStreamConstants.END_ELEMENT;
	}

	@Override
	public boolean isWhiteSpace() {
		return getEventType() == XMLStreamConstants.SPACE;
	}

	@Override
	public boolean isStartElement() {
		return getEventType() == XMLStreamConstants.START_ELEMENT;
	}

	@Override
	public boolean isEndElement() {
		return getEventType() == XMLStreamConstants.END_ELEMENT;
	}

	@Override
	public boolean isCharacters() {
		return getEventType() == XMLStreamConstants.CHARACTERS;
	}

	@Override
	public int nextTag() throws XMLStreamException {
		int eventType = next();
		while (eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace() ||
				eventType == XMLStreamConstants.CDATA && isWhiteSpace() || eventType == XMLStreamConstants.SPACE ||
				eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType == XMLStreamConstants.COMMENT) {
			eventType = next();
		}
		if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
			throw new XMLStreamException("expected start or end tag", getLocation());
		}
		return eventType;
	}

	@Override
	public void require(int expectedType, String namespaceURI, String localName) throws XMLStreamException {
		int eventType = getEventType();
		if (eventType != expectedType) {
			throw new XMLStreamException("Expected [" + expectedType + "] but read [" + eventType + "]");
		}
	}

	@Override
	public String getAttributeValue(String namespaceURI, String localName) {
		for (int i = 0; i < getAttributeCount(); i++) {
			QName name = getAttributeName(i);
			if (name.getLocalPart().equals(localName) &&
					(namespaceURI == null || name.getNamespaceURI().equals(namespaceURI))) {
				return getAttributeValue(i);
			}
		}
		return null;
	}

	@Override
	public boolean hasNext() throws XMLStreamException {
		return getEventType() != END_DOCUMENT;
	}

	@Override
	public String getLocalName() {
		return getName().getLocalPart();
	}

	@Override
	public char[] getTextCharacters() {
		return getText().toCharArray();
	}

	@Override
	public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
			throws XMLStreamException {
		char[] source = getTextCharacters();
		length = Math.min(length, source.length);
		System.arraycopy(source, sourceStart, target, targetStart, length);
		return length;
	}

	@Override
	public int getTextLength() {
		return getText().length();
	}
}
