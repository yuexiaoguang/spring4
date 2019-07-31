package org.springframework.web.bind.support;

import org.springframework.web.context.request.WebRequest;

/**
 * 用于在后端会话中存储模型属性的策略接口.
 */
public interface SessionAttributeStore {

	/**
	 * 将提供的属性存储在后端会话中.
	 * <p>可以调用新属性以及现有属性.
	 * 在后一种情况下, 这表示属性值可能已被修改.
	 * 
	 * @param request 当前请求
	 * @param attributeName 属性名称
	 * @param attributeValue 要存储的属性值
	 */
	void storeAttribute(WebRequest request, String attributeName, Object attributeValue);

	/**
	 * 从后端会话中检索指定的属性.
	 * <p>通常期望该属性已存在, 如果此方法返回{@code null}, 则抛出异常.
	 * 
	 * @param request 当前请求
	 * @param attributeName 属性名称
	 * 
	 * @return 当前属性值, 或{@code null}
	 */
	Object retrieveAttribute(WebRequest request, String attributeName);

	/**
	 * 清除后端会话中的指定属性.
	 * <p>表示将不再使用属性名称.
	 * 
	 * @param request 当前请求
	 * @param attributeName 属性名称
	 */
	void cleanupAttribute(WebRequest request, String attributeName);

}
