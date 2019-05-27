package org.springframework.ui;

import java.util.Collection;
import java.util.Map;

/**
 * 特定于Java-5的接口, 用于定义模型属性的持有者.
 * 主要用于向模型添加属性.
 * 允许以{@code java.util.Map}的形式访问整个模型.
 */
public interface Model {

	/**
	 * 在提供的名称下添加提供的属性.
	 * 
	 * @param attributeName 模型属性的名称 (never {@code null})
	 * @param attributeValue 模型属性值 (can be {@code null})
	 */
	Model addAttribute(String attributeName, Object attributeValue);

	/**
	 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}将提供的属性添加到此{@code Map}.
	 * <p><emphasis>Note: 使用此方法时, 不会将空{@link java.util.Collection Collections}添加到模型中, 因为无法正确确定真实的约定名称.
	 * 视图代码应该检查{@code null}, 而不是检查JSTL标签已经完成的空集合.</emphasis>
	 * 
	 * @param attributeValue 模型属性值 (never {@code null})
	 */
	Model addAttribute(Object attributeValue);

	/**
	 * 使用每个元素的属性名称生成, 将提供的{@code Collection}中的所有属性复制到此{@code Map}中.
	 */
	Model addAllAttributes(Collection<?> attributeValues);

	/**
	 * 将提供的{@code Map}中的所有属性复制到此{@code Map}.
	 */
	Model addAllAttributes(Map<String, ?> attributes);

	/**
	 * 将提供的{@code Map}中的所有属性复制到此{@code Map}中, 同名的现有对象优先 (i.e. 即不被替换).
	 */
	Model mergeAttributes(Map<String, ?> attributes);

	/**
	 * 此模型是否包含给定名称的属性?
	 * 
	 * @param attributeName 模型属性的名称 (never {@code null})
	 * 
	 * @return 此模型是否包含相应的属性
	 */
	boolean containsAttribute(String attributeName);

	/**
	 * 返回当前模型属性集合.
	 */
	Map<String, Object> asMap();

}
