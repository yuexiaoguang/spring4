package org.springframework.beans.factory;

/**
 * 在目前不允许创建bean, 但仍然在请求bean的情况下抛出异常 (例如, 在bean工厂的关闭阶段).
 */
@SuppressWarnings("serial")
public class BeanCreationNotAllowedException extends BeanCreationException {

	/**
	 * @param beanName 请求的bean的名称
	 * @param msg the detail message
	 */
	public BeanCreationNotAllowedException(String beanName, String msg) {
		super(beanName, msg);
	}

}
