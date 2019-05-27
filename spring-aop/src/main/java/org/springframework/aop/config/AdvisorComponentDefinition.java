package org.springframework.aop.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.parsing.AbstractComponentDefinition;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.parsing.ComponentDefinition}
 * 填补了由{@code <aop:advisor>}标记配置的切面bean定义与组件定义基础结构之间的差距.
 */
public class AdvisorComponentDefinition extends AbstractComponentDefinition {

	private final String advisorBeanName;

	private final BeanDefinition advisorDefinition;

	private String description;

	private BeanReference[] beanReferences;

	private BeanDefinition[] beanDefinitions;


	public AdvisorComponentDefinition(String advisorBeanName, BeanDefinition advisorDefinition) {
		 this(advisorBeanName, advisorDefinition, null);
	}

	public AdvisorComponentDefinition(
			String advisorBeanName, BeanDefinition advisorDefinition, BeanDefinition pointcutDefinition) {

		Assert.notNull(advisorBeanName, "'advisorBeanName' must not be null");
		Assert.notNull(advisorDefinition, "'advisorDefinition' must not be null");
		this.advisorBeanName = advisorBeanName;
		this.advisorDefinition = advisorDefinition;
		unwrapDefinitions(advisorDefinition, pointcutDefinition);
	}


	private void unwrapDefinitions(BeanDefinition advisorDefinition, BeanDefinition pointcutDefinition) {
		MutablePropertyValues pvs = advisorDefinition.getPropertyValues();
		BeanReference adviceReference = (BeanReference) pvs.getPropertyValue("adviceBeanName").getValue();

		if (pointcutDefinition != null) {
			this.beanReferences = new BeanReference[] {adviceReference};
			this.beanDefinitions = new BeanDefinition[] {advisorDefinition, pointcutDefinition};
			this.description = buildDescription(adviceReference, pointcutDefinition);
		}
		else {
			BeanReference pointcutReference = (BeanReference) pvs.getPropertyValue("pointcut").getValue();
			this.beanReferences = new BeanReference[] {adviceReference, pointcutReference};
			this.beanDefinitions = new BeanDefinition[] {advisorDefinition};
			this.description = buildDescription(adviceReference, pointcutReference);
		}
	}

	private String buildDescription(BeanReference adviceReference, BeanDefinition pointcutDefinition) {
		return new StringBuilder("Advisor <advice(ref)='").
				append(adviceReference.getBeanName()).append("', pointcut(expression)=[").
				append(pointcutDefinition.getPropertyValues().getPropertyValue("expression").getValue()).
				append("]>").toString();
	}

	private String buildDescription(BeanReference adviceReference, BeanReference pointcutReference) {
		return new StringBuilder("Advisor <advice(ref)='").
				append(adviceReference.getBeanName()).append("', pointcut(ref)='").
				append(pointcutReference.getBeanName()).append("'>").toString();
	}


	@Override
	public String getName() {
		return this.advisorBeanName;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public BeanDefinition[] getBeanDefinitions() {
		return this.beanDefinitions;
	}

	@Override
	public BeanReference[] getBeanReferences() {
		return this.beanReferences;
	}

	@Override
	public Object getSource() {
		return this.advisorDefinition.getSource();
	}

}
