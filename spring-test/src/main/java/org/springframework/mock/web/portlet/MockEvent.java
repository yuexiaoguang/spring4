package org.springframework.mock.web.portlet;

import java.io.Serializable;
import javax.portlet.Event;
import javax.xml.namespace.QName;

/**
 * Mock implementation of the {@link javax.portlet.Event} interface.
 */
public class MockEvent implements Event {

	private final QName name;

	private final Serializable value;


	/**
	 * Create a new MockEvent with the given name.
	 * @param name the name of the event
	 */
	public MockEvent(QName name) {
		this.name = name;
		this.value = null;
	}

	/**
	 * Create a new MockEvent with the given name and value.
	 * @param name the name of the event
	 * @param value the associated payload of the event
	 */
	public MockEvent(QName name, Serializable value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Create a new MockEvent with the given name.
	 * @param name the name of the event
	 */
	public MockEvent(String name) {
		this.name = new QName(name);
		this.value = null;
	}

	/**
	 * Create a new MockEvent with the given name and value.
	 * @param name the name of the event
	 * @param value the associated payload of the event
	 */
	public MockEvent(String name, Serializable value) {
		this.name = new QName(name);
		this.value = value;
	}


	@Override
	public QName getQName() {
		return this.name;
	}

	@Override
	public String getName() {
		return this.name.getLocalPart();
	}

	@Override
	public Serializable getValue() {
		return this.value;
	}

}
