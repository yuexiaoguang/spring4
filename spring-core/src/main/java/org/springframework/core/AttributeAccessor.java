package org.springframework.core;

/**
 * 定义用于向/从任意对象附加和访问元数据的通用契约的接口.
 */
public interface AttributeAccessor {

	/**
	 * 设置{@code name}定义的属性的{@code value}.
	 * 如果{@code value}是{@code null}, 则属性将被{@link #removeAttribute removed}.
	 * <p>通常, 用户应注意通过使用完全限定名称来防止与其他元数据属性重叠, 使用类或包名称作为前缀.
	 * 
	 * @param name 唯一的属性键
	 * @param value 要附加的属性值
	 */
	void setAttribute(String name, Object value);

	/**
	 * 获取{@code name}标识的属性的值.
	 * 如果该属性不存在, 则返回{@code null}.
	 * 
	 * @param name 唯一的属性键
	 * 
	 * @return 属性的当前值
	 */
	Object getAttribute(String name);

	/**
	 * 删除{@code name}标识的属性, 并返回其值.
	 * 如果找不到{@code name}下的属性, 则返回{@code null}.
	 * 
	 * @param name 唯一的属性键
	 * 
	 * @return 属性的最后一个值
	 */
	Object removeAttribute(String name);

	/**
	 * 如果{@code name}标识的属性存在, 则返回{@code true}.
	 * 否则返回{@code false}.
	 * 
	 * @param name 唯一的属性键
	 */
	boolean hasAttribute(String name);

	/**
	 * 返回所有属性的名称.
	 */
	String[] attributeNames();

}
