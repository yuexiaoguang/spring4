package org.springframework.beans;

/**
 * 包含一个或多个{@link PropertyValue}对象的持有者, 通常包括特定目标bean的一个更新.
 */
public interface PropertyValues {

	/**
	 * 返回此对象中保存的PropertyValue对象的数组.
	 */
	PropertyValue[] getPropertyValues();

	/**
	 * 返回具有给定名称的属性值.
	 * 
	 * @param propertyName 要搜索的名称
	 * 
	 * @return 属性值, 或{@code null}
	 */
	PropertyValue getPropertyValue(String propertyName);

	/**
	 * 返回自上一个PropertyValues以来的更改.
	 * 子类也应该覆盖{@code equals}.
	 * 
	 * @param old 旧的属性值
	 * 
	 * @return 已更新的PropertyValues或新属性. 如果没有更改, 则返回空PropertyValues.
	 */
	PropertyValues changesSince(PropertyValues old);

	/**
	 * 是否有此属性的属性值（或其他处理条目）?
	 * 
	 * @param propertyName 感兴趣的属性的名称
	 * 
	 * @return 是否有此属性的属性值
	 */
	boolean contains(String propertyName);

	/**
	 * 此持有者是否完全不包含PropertyValue对象?
	 */
	boolean isEmpty();

}
