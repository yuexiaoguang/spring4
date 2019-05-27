package org.springframework.oxm.xmlbeans;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;

import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} that configures an XMLBeans {@code XmlOptions} object
 * and provides it as a bean reference.
 *
 * <p>Typical usage will be to set XMLBeans options on this bean, and refer to it
 * in the {@link XmlBeansMarshaller}.
 */
public class XmlOptionsFactoryBean implements FactoryBean<XmlOptions> {

	private XmlOptions xmlOptions = new XmlOptions();


	/**
	 * Set options on the underlying {@code XmlOptions} object.
	 * <p>The keys of the supplied map should be one of the String constants
	 * defined in {@code XmlOptions}, the values vary per option.
	 */
	public void setOptions(Map<String, ?> optionsMap) {
		this.xmlOptions = new XmlOptions();
		if (optionsMap != null) {
			for (Map.Entry<String, ?> option : optionsMap.entrySet()) {
				this.xmlOptions.put(option.getKey(), option.getValue());
			}
		}
	}


	@Override
	public XmlOptions getObject() {
		return this.xmlOptions;
	}

	@Override
	public Class<? extends XmlOptions> getObjectType() {
		return XmlOptions.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
