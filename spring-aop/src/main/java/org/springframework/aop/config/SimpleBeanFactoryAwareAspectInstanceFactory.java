package org.springframework.aop.config;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link AspectInstanceFactory}实现, 使用配置的bean名称从{@link org.springframework.beans.factory.BeanFactory}定位切面.
 */
public class SimpleBeanFactoryAwareAspectInstanceFactory implements AspectInstanceFactory, BeanFactoryAware {

	private String aspectBeanName;

	private BeanFactory beanFactory;


	/**
	 * 设置切面bean的名称. 这是调用{@link #getAspectInstance()}时返回的bean.
	 */
	public void setAspectBeanName(String aspectBeanName) {
		this.aspectBeanName = aspectBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		Assert.notNull(this.aspectBeanName, "'aspectBeanName' is required");
	}


	/**
	 * 从{@link BeanFactory}中查找切面 bean并返回它.
	 */
	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.aspectBeanName);
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
		}
		else {
			return ClassUtils.getDefaultClassLoader();
		}
	}

	@Override
	public int getOrder() {
		if (this.beanFactory.isSingleton(this.aspectBeanName) &&
				this.beanFactory.isTypeMatch(this.aspectBeanName, Ordered.class)) {
			return ((Ordered) this.beanFactory.getBean(this.aspectBeanName)).getOrder();
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

}
