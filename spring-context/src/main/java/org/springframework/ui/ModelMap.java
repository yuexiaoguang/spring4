package org.springframework.ui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Conventions;
import org.springframework.util.Assert;

/**
 * {@link java.util.Map}的实现, 在构建用于UI工具的模型数据时使用.
 * 支持链式调用和模型属性名称的生成.
 *
 * <p>此类充当Servlet和Portlet MVC的通用模型持有者, 但不依赖于其中任何一个.
 * 查看{@link Model}接口, 了解基于Java-5的接口变体, 它具有相同的用途.
 */
@SuppressWarnings("serial")
public class ModelMap extends LinkedHashMap<String, Object> {

	public ModelMap() {
	}

	/**
	 * 构造一个新的{@code ModelMap}.
	 */
	public ModelMap(String attributeName, Object attributeValue) {
		addAttribute(attributeName, attributeValue);
	}

	/**
	 * 使用属性名称生成为提供的模型对象生成键.
	 */
	public ModelMap(Object attributeValue) {
		addAttribute(attributeValue);
	}


	/**
	 * @param attributeName 模型属性的名称 (never {@code null})
	 * @param attributeValue 模型属性值 (can be {@code null})
	 */
	public ModelMap addAttribute(String attributeName, Object attributeValue) {
		Assert.notNull(attributeName, "Model attribute name must not be null");
		put(attributeName, attributeValue);
		return this;
	}

	/**
	 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}将提供的属性添加到此{@code Map}.
	 * <p><emphasis>Note: 使用此方法时, 不会将空{@link Collection Collections}添加到模型中, 因为无法正确确定真正的约定名称.
	 * 视图代码应该检查{@code null}, 而不是JSTL标记已经完成的空集合.</emphasis>
	 * 
	 * @param attributeValue 模型属性值 (never {@code null})
	 */
	public ModelMap addAttribute(Object attributeValue) {
		Assert.notNull(attributeValue, "Model object must not be null");
		if (attributeValue instanceof Collection && ((Collection<?>) attributeValue).isEmpty()) {
			return this;
		}
		return addAttribute(Conventions.getVariableName(attributeValue), attributeValue);
	}

	/**
	 * 使用每个元素的属性名称生成, 将提供的{@code Collection}中的所有属性复制到此{@code Map}中.
	 */
	public ModelMap addAllAttributes(Collection<?> attributeValues) {
		if (attributeValues != null) {
			for (Object attributeValue : attributeValues) {
				addAttribute(attributeValue);
			}
		}
		return this;
	}

	/**
	 * 将提供的{@code Map}中的所有属性复制到此{@code Map}.
	 */
	public ModelMap addAllAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			putAll(attributes);
		}
		return this;
	}

	/**
	 * 将提供的{@code Map}中的所有属性复制到此{@code Map}中, 同名的现有对象优先 (i.e. 不被替换).
	 */
	public ModelMap mergeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (Map.Entry<String, ?> entry : attributes.entrySet()) {
				String key = entry.getKey();
				if (!containsKey(key)) {
					put(key, entry.getValue());
				}
			}
		}
		return this;
	}

	/**
	 * 此模型是否包含给定名称的属性?
	 * 
	 * @param attributeName 模型属性的名称 (never {@code null})
	 * 
	 * @return 此模型是否包含相应的属性
	 */
	public boolean containsAttribute(String attributeName) {
		return containsKey(attributeName);
	}

}
