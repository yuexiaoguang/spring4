package org.springframework.aop.config;

import java.lang.reflect.Method;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean}实现, 在指定的bean上定位一个{@link Method}.
 */
public class MethodLocatingFactoryBean implements FactoryBean<Method>, BeanFactoryAware {

	private String targetBeanName;

	private String methodName;

	private Method method;


	/**
	 * 设置要定位{@link Method}的 bean名称.
	 * <p>此属性是必需的.
	 * 
	 * @param targetBeanName 用于定位{@link Method}的bean的名称
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * 设置要定位的{@link Method}的名称.
	 * <p>此属性是必需的.
	 * 
	 * @param methodName 要定位的{@link Method}的名称.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!StringUtils.hasText(this.targetBeanName)) {
			throw new IllegalArgumentException("Property 'targetBeanName' is required");
		}
		if (!StringUtils.hasText(this.methodName)) {
			throw new IllegalArgumentException("Property 'methodName' is required");
		}

		Class<?> beanClass = beanFactory.getType(this.targetBeanName);
		if (beanClass == null) {
			throw new IllegalArgumentException("Can't determine type of bean with name '" + this.targetBeanName + "'");
		}
		this.method = BeanUtils.resolveSignature(this.methodName, beanClass);

		if (this.method == null) {
			throw new IllegalArgumentException("Unable to locate method [" + this.methodName +
					"] on bean [" + this.targetBeanName + "]");
		}
	}


	@Override
	public Method getObject() throws Exception {
		return this.method;
	}

	@Override
	public Class<Method> getObjectType() {
		return Method.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
