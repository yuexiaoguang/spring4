package org.springframework.ui;

import java.util.Collection;
import java.util.Map;

/**
 * 实现{@link Model}接口的{@link ModelMap}的子类.
 * Java 5特定于{@code Model}接口本身.
 *
 * <p>这是Spring MVC向处理器方法公开的实现类, 通常是通过{@link org.springframework.ui.Model}接口的声明.
 * 无需在用户代码中构建它;
 * 普通的{@link org.springframework.ui.ModelMap}或者只是一个带有String键的常规{@link Map}就足以返回一个用户模型.
 */
@SuppressWarnings("serial")
public class ExtendedModelMap extends ModelMap implements Model {

	@Override
	public ExtendedModelMap addAttribute(String attributeName, Object attributeValue) {
		super.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(Map<String, ?> attributes) {
		super.addAllAttributes(attributes);
		return this;
	}

	@Override
	public ExtendedModelMap mergeAttributes(Map<String, ?> attributes) {
		super.mergeAttributes(attributes);
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}
