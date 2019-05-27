package org.springframework.beans;

import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 可以访问命名属性的类的公共接口 (例如对象的bean属性或对象中的字段).
 * 作为{@link BeanWrapper}的基础接口.
 */
public interface PropertyAccessor {

	/**
	 * 嵌套属性的路径分隔符.
	 * 遵循常规Java约定: getFoo().getBar() -> "foo.bar".
	 */
	String NESTED_PROPERTY_SEPARATOR = ".";
	char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

	/**
	 * 标记, 指示索引或映射属性的属性Key的开头: "person.addresses[0]".
	 */
	String PROPERTY_KEY_PREFIX = "[";
	char PROPERTY_KEY_PREFIX_CHAR = '[';

	/**
	 * 标记, 指示索引或映射属性的属性Key的结尾: "person.addresses[0]".
	 */
	String PROPERTY_KEY_SUFFIX = "]";
	char PROPERTY_KEY_SUFFIX_CHAR = ']';


	/**
	 * 确定指定的属性是否可读.
	 * <p>如果属性不存在, 则返回{@code false}.
	 * 
	 * @param propertyName 要检查的属性 (可以是嵌套路径和/或索引/映射属性)
	 * 
	 * @return 属性是否可读
	 */
	boolean isReadableProperty(String propertyName);

	/**
	 * 确定指定的属性是否可写.
	 * <p>如果属性不存在, 则返回{@code false}.
	 * 
	 * @param propertyName 要检查的属性 (可以是嵌套路径和/或索引/映射属性)
	 * 
	 * @return 属性是否可写
	 */
	boolean isWritableProperty(String propertyName);

	/**
	 * 确定指定属性的属性类型, 检查属性描述符, 或在索引/映射元素的情况下检查值.
	 * 
	 * @param propertyName 要检查的属性 (可以是嵌套路径和/或索引/映射属性)
	 * 
	 * @return 特定属性的属性类型, 或 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问方法失败
	 */
	Class<?> getPropertyType(String propertyName) throws BeansException;

	/**
	 * 返回指定属性的类型描述符: 优选地, 从读取方法, 回到写方法.
	 * 
	 * @param propertyName 要检查的属性 (可以是嵌套路径和/或索引/映射属性)
	 * 
	 * @return 特定属性的属性类型, 或 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问方法失败
	 */
	TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

	/**
	 * 获取指定属性的当前值.
	 * 
	 * @param propertyName 要获取值的属性的名称(可以是嵌套路径和/或索引/映射属性)
	 * 
	 * @return 属性值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可读
	 * @throws PropertyAccessException 如果属性有效但访问方法失败
	 */
	Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 将指定值设置为当前属性值.
	 * 
	 * @param propertyName 要设置值的属性的名称 (可以是嵌套路径和/或索引/映射属性)
	 * @param value 新值
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问方法失败
	 */
	void setPropertyValue(String propertyName, Object value) throws BeansException;

	/**
	 * 将指定值设置为当前属性值.
	 * 
	 * @param pv 包含新属性值的对象
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问方法失败或类型不匹配
	 */
	void setPropertyValue(PropertyValue pv) throws BeansException;

	/**
	 * 从Map执行批量更新.
	 * <p>PropertyValues的批量更新功能更强大:
	 * 提供该方法是为了方便起见. 行为将与{@link #setPropertyValues(PropertyValues)}方法的行为相同.
	 * 
	 * @param map 要获取属性的Map. 属性名作为Key
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果批量更新期间特定属性发生一个或多个PropertyAccessException.
	 * 此异常捆绑了所有单个PropertyAccessException. 所有其他属性都已成功更新.
	 */
	void setPropertyValues(Map<?, ?> map) throws BeansException;

	/**
	 * 执行批量更新的首选方法.
	 * <p>请注意, 执行批量更新与执行单个更新不同, 因为这个类的实现将继续更新属性,
	 * 如果遇到可恢复的错误(例如类型不匹配, 但不是无效的字段名称等), 抛出包含所有个别错误的{@link PropertyBatchUpdateException}.
	 * 稍后可以检查此异常以查看所有绑定错误. 已成功更新的属性仍会更改.
	 * <p>不允许使用未知字段或无效字段.
	 * 
	 * @param pvs 要设置在目标对象上的PropertyValues
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果批量更新期间特定属性发生一个或多个PropertyAccessException.
	 * 此异常捆绑了所有单个PropertyAccessException. 所有其他属性都已成功更新.
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	/**
	 * 通过更多控制行为执行批量更新.
	 * <p>请注意, 执行批量更新与执行单个更新不同, 因为这个类的实现将继续更新属性,
	 * 如果遇到可恢复的错误(例如类型不匹配, 但不是无效的字段名称等), 抛出包含所有个别错误的{@link PropertyBatchUpdateException}.
	 * 稍后可以检查此异常以查看所有绑定错误. 已成功更新的属性仍会更改.
	 * 
	 * @param pvs 要设置在目标对象上的PropertyValues
	 * @param ignoreUnknown 是否应该忽略未知属性 (在bean中找不到)
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果批量更新期间特定属性发生一个或多个PropertyAccessException.
	 * 此异常捆绑了所有单个PropertyAccessException. 所有其他属性都已成功更新.
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
			throws BeansException;

	/**
	 * 通过完全控制行为执行批量更新.
	 * <p>请注意, 执行批量更新与执行单个更新不同, 因为这个类的实现将继续更新属性,
	 * 如果遇到可恢复的错误(例如类型不匹配, 但不是无效的字段名称等), 抛出包含所有个别错误的{@link PropertyBatchUpdateException}.
	 * 稍后可以检查此异常以查看所有绑定错误. 已成功更新的属性仍会更改.
	 * 
	 * @param pvs 要设置在目标对象上的PropertyValues
	 * @param ignoreUnknown 是否应该忽略未知属性 (在bean中找不到)
	 * @param ignoreInvalid 是否应该忽略无效的属性 (找到了, 但无法访问)
	 * 
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果批量更新期间特定属性发生一个或多个PropertyAccessException.
	 * 此异常捆绑了所有单个PropertyAccessException. 所有其他属性都已成功更新.
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException;

}
