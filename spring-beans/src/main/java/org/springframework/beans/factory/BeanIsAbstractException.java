package org.springframework.beans.factory;

/**
 * 当请求已定义为抽象的bean定义的实例时抛出异常.
 */
@SuppressWarnings("serial")
public class BeanIsAbstractException extends BeanCreationException {

	/**
	 * @param beanName 请求的bean的名称
	 */
	public BeanIsAbstractException(String beanName) {
		super(beanName, "Bean definition is abstract");
	}

}
