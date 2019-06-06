package org.springframework.oxm.xmlbeans;

import java.util.Map;

import org.apache.xmlbeans.XmlOptions;

import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean}, 用于配置XMLBeans {@code XmlOptions}对象并将其作为bean引用提供.
 *
 * <p>典型的用法是在这个bean上设置XMLBeans选项, 并在{@link XmlBeansMarshaller}中引用它.
 */
public class XmlOptionsFactoryBean implements FactoryBean<XmlOptions> {

	private XmlOptions xmlOptions = new XmlOptions();


	/**
	 * 在底层{@code XmlOptions}对象上设置选项.
	 * <p>提供的映射的键应该是{@code XmlOptions}中定义的字符串常量之一, 每个选项的值不同.
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
