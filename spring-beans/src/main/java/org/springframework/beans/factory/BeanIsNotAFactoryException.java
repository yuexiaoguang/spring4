package org.springframework.beans.factory;

/**
 * 当bean不是工厂, 但是用户试图在工厂获取给定的bean名称时抛出异常.
 * bean是否是工厂, 是由它是否实现FactoryBean接口决定的.
 */
@SuppressWarnings("serial")
public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

	/**
	 * @param name 请求的bean的名称
	 * @param actualType 返回的实际类型, 与预期类型不匹配
	 */
	public BeanIsNotAFactoryException(String name, Class<?> actualType) {
		super(name, FactoryBean.class, actualType);
	}

}
