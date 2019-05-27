package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * Spring的低级JavaBeans基础结构的中央接口.
 *
 * <p>通常不直接使用, 而是通过{@link org.springframework.beans.factory.BeanFactory}或{@link org.springframework.validation.DataBinder}隐式使用.
 *
 * <p>提供分析和操作标准JavaBean的操作:
 * 获取和设置属性值的能力 (单独或一起), 获取属性描述符, 并查询属性的可读性/可写性.
 *
 * <p>这个接口支持<b>嵌套的属性</b> 使子属性的属性设置为无限深度.
 *
 * <p>BeanWrapper的“extractOldValueForEditor”设置的默认值为“false”, 避免由getter方法调用引起的副作用.
 * 将其设置为“true”以将当前属性值公开给自定义编辑器.
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

	/**
	 * 指定数组和集合自动增长的限制.
	 * <p>普通BeanWrapper的默认值是无限制的.
	 * @since 4.1
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * 返回数组和集合自动增长的限制.
	 * @since 4.1
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * 返回此对象包装的bean实例.
	 * 
	 * @return bean实例, 或{@code null}
	 */
	Object getWrappedInstance();

	/**
	 * 返回包装的JavaBean对象的类型.
	 * 
	 * @return 包装bean实例的类型, 或{@code null}
	 */
	Class<?> getWrappedClass();

	/**
	 * 获取包装对象的PropertyDescriptors (由标准JavaBeans反射确定).
	 * 
	 * @return 包装对象的PropertyDescriptors
	 */
	PropertyDescriptor[] getPropertyDescriptors();

	/**
	 * 获取包装对象的特定属性的属性描述符.
	 * 
	 * @param propertyName 要获取描述符的属性 (可能是嵌套路径, 但没有索引/映射属性)
	 * 
	 * @return 指定属性的属性描述符
	 * @throws InvalidPropertyException 如果没有这样的属性
	 */
	PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

}
