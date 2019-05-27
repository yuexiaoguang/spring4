package org.springframework.beans;

import java.beans.PropertyEditor;

/**
 * 封装注册JavaBeans {@link PropertyEditor PropertyEditors}的方法.
 * 这是{@link PropertyEditorRegistrar}操作的中心接口.
 *
 * <p>由{@link BeanWrapper}扩展; 由{@link BeanWrapperImpl} 和 {@link org.springframework.validation.DataBinder}实现.
 */
public interface PropertyEditorRegistry {

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器.
	 * 
	 * @param requiredType 属性的类型
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * 为给定的类型和属性, 或者给定类型的所有属性, 注册给定的自定义属性编辑器.
	 * <p>如果属性路径表示数组或Collection属性, 编辑器将应用于数组/集合本身 ({@link PropertyEditor} 必须创建一个数组或Collection值),
	 * 或应用于每个元素 ({@code PropertyEditor}必须创建元素类型), 取决于指定的所需类型.
	 * <p>Note: 每个属性路径仅支持一个注册的自定义编辑器.
	 * 在Collection/array的情况下, 不要为同一属性上的Collection/array和每个元素注册编辑器.
	 * <p>例如, 如果想为 "items[n].quantity" (所有元素 n)注册一个编辑器, 需要将 "items.quantity" 作为这个方法的'propertyPath'参数的值.
	 * 
	 * @param requiredType 属性的类型.
	 * 如果给出了属性, 这可能是{@code null}, 但在任何情况下都应该指定,  特别是在Collection的情况下
	 *  - 明确编辑器是应该应用于整个Collection本身还是应用于每个条目.
	 * 所以作为一般规则:
	 * <b>不要在Collection/array的情况下指定 {@code null}!</b>
	 * 
	 * @param propertyPath 属性的路径 (名称或嵌套路径), 或{@code null} 如果为给定类型的所有属性注册编辑器
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor);

	/**
	 * 查找给定类型和属性的自定义属性编辑器.
	 * 
	 * @param requiredType 属性的类型 (可以是 {@code null}, 如果给出了属性但在任何情况下都应指定用于一致性检查)
	 * @param propertyPath 属性的路径 (名称或嵌套路径), 或{@code null} 如果为给定类型的所有属性注册编辑器
	 * 
	 * @return the registered editor, or {@code null} if none
	 */
	PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath);

}
