package org.springframework.mock.web.portlet;

import java.io.Serializable;
import javax.portlet.Event;
import javax.xml.namespace.QName;

/**
 * {@link javax.portlet.Event}接口的模拟实现.
 */
public class MockEvent implements Event {

	private final QName name;

	private final Serializable value;


	/**
	 * @param name 事件的名称
	 */
	public MockEvent(QName name) {
		this.name = name;
		this.value = null;
	}

	/**
	 * @param name 事件的名称
	 * @param value 事件的相关有效负载
	 */
	public MockEvent(QName name, Serializable value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @param name 事件的名称
	 */
	public MockEvent(String name) {
		this.name = new QName(name);
		this.value = null;
	}

	/**
	 * @param name 事件的名称
	 * @param value 事件的相关有效负载
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
