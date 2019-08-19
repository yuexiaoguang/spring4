package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.PageContext;

/**
 * 用于为JSP标记生成'{@code id}'属性值的工具类.
 * 给定标记的名称 (在大多数情况下为数据绑定路径), 在当前{@link PageContext}中返回该名称的唯一ID.
 * 每个对给定名称的ID的请求都会附加一个不断增加的计数器到名称本身.
 * 例如, 如果名称为'{@code person.name}', 则第一个请求将提供'{@code person.name1}', 第二个请求将提供'{@code person.name2}'.
 * 这支持常见用例, 其中为同一数据字段生成一组radio或check按钮, 每个按钮是一个不同的标记实例.
 */
abstract class TagIdGenerator {

	/**
	 * 此标记创建的所有{@link PageContext}属性的前缀.
	 */
	private static final String PAGE_CONTEXT_ATTRIBUTE_PREFIX = TagIdGenerator.class.getName() + ".";

	/**
	 * 获取所提供名称的下一个唯一ID (在给定的{@link PageContext}内).
	 */
	public static String nextId(String name, PageContext pageContext) {
		String attributeName = PAGE_CONTEXT_ATTRIBUTE_PREFIX + name;
		Integer currentCount = (Integer) pageContext.getAttribute(attributeName);
		currentCount = (currentCount != null ? currentCount + 1 : 1);
		pageContext.setAttribute(attributeName, currentCount);
		return (name + currentCount);
	}

}
