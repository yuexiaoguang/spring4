package org.springframework.beans.factory.parsing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;

/**
 * ComponentDefinition基于标准BeanDefinition, 公开给定的bean定义, 以及给定bean的内部bean定义和bean引用.
 */
public class BeanComponentDefinition extends BeanDefinitionHolder implements ComponentDefinition {

	private BeanDefinition[] innerBeanDefinitions;

	private BeanReference[] beanReferences;


	/**
	 * @param beanDefinition BeanDefinition
	 * @param beanName bean的名称
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName) {
		super(beanDefinition, beanName);
		findInnerBeanDefinitionsAndBeanReferences(beanDefinition);
	}

	/**
	 * @param beanDefinition BeanDefinition
	 * @param beanName bean的名称
	 * @param aliases bean的别名, 或{@code null}
	 */
	public BeanComponentDefinition(BeanDefinition beanDefinition, String beanName, String[] aliases) {
		super(beanDefinition, beanName, aliases);
		findInnerBeanDefinitionsAndBeanReferences(beanDefinition);
	}

	/**
	 * @param holder 封装bean定义的BeanDefinitionHolder, 以及bean的名称
	 */
	public BeanComponentDefinition(BeanDefinitionHolder holder) {
		super(holder);
		findInnerBeanDefinitionsAndBeanReferences(holder.getBeanDefinition());
	}


	private void findInnerBeanDefinitionsAndBeanReferences(BeanDefinition beanDefinition) {
		List<BeanDefinition> innerBeans = new ArrayList<BeanDefinition>();
		List<BeanReference> references = new ArrayList<BeanReference>();
		PropertyValues propertyValues = beanDefinition.getPropertyValues();
		for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
			Object value = propertyValue.getValue();
			if (value instanceof BeanDefinitionHolder) {
				innerBeans.add(((BeanDefinitionHolder) value).getBeanDefinition());
			}
			else if (value instanceof BeanDefinition) {
				innerBeans.add((BeanDefinition) value);
			}
			else if (value instanceof BeanReference) {
				references.add((BeanReference) value);
			}
		}
		this.innerBeanDefinitions = innerBeans.toArray(new BeanDefinition[innerBeans.size()]);
		this.beanReferences = references.toArray(new BeanReference[references.size()]);
	}


	@Override
	public String getName() {
		return getBeanName();
	}

	@Override
	public String getDescription() {
		return getShortDescription();
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return new BeanDefinition[] {getBeanDefinition()};
	}

	@Override
	public BeanDefinition[] getInnerBeanDefinitions() {
		return this.innerBeanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}


	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * 除了超类的相等要求之外, 此实现还期望其他对象也是BeanComponentDefinition类型.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof BeanComponentDefinition && super.equals(other)));
	}

}
