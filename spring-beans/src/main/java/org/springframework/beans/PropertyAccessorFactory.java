package org.springframework.beans;

/**
 * 用于获取{@link PropertyAccessor}实例的简单工厂外观, 特别是对于{@link BeanWrapper}实例.
 * 隐藏实际的目标实现类及其扩展的公共签名.
 */
public abstract class PropertyAccessorFactory {

	/**
	 * 获取给定目标对象的BeanWrapper, 以JavaBeans样式访问属性.]
	 * 
	 * @param target 要包装的目标对象
	 * 
	 * @return 属性访问器
	 */
	public static BeanWrapper forBeanPropertyAccess(Object target) {
		return new BeanWrapperImpl(target);
	}

	/**
	 * 获取给定目标对象的PropertyAccessor, 以直接字段样式访问属性.
	 * 
	 * @param target 要包装的目标对象
	 * 
	 * @return 属性访问器
	 */
	public static ConfigurablePropertyAccessor forDirectFieldAccess(Object target) {
		return new DirectFieldAccessor(target);
	}
}
