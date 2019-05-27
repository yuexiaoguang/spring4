package org.springframework.beans.factory.support;

import org.springframework.util.Assert;

/**
 * 用于保存托管数组元素的标签集合类, 可能包含运行时bean引用(要解析为bean对象).
 */
@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

	/** 用于运行时创建目标数组的已解析的元素类型 */
	volatile Class<?> resolvedElementType;


	/**
	 * 创建一个新的托管数组占位符.
	 * 
	 * @param elementTypeName 目标元素类型作为类名
	 * @param size 数组大小
	 */
	public ManagedArray(String elementTypeName, int size) {
		super(size);
		Assert.notNull(elementTypeName, "elementTypeName must not be null");
		setElementTypeName(elementTypeName);
	}

}
