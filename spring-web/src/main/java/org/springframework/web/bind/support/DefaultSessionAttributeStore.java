package org.springframework.web.bind.support;

import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;

/**
 * {@link SessionAttributeStore}接口的默认实现, 将属性存储在WebRequest会话中 (i.e. HttpSession 或 PortletSession).
 */
public class DefaultSessionAttributeStore implements SessionAttributeStore {

	private String attributeNamePrefix = "";


	/**
	 * 指定用于后端会话中的属性名称的前缀.
	 * <p>默认是不使用前缀, 存储与模型中名称相同的会话属性.
	 */
	public void setAttributeNamePrefix(String attributeNamePrefix) {
		this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
	}


	@Override
	public void storeAttribute(WebRequest request, String attributeName, Object attributeValue) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		Assert.notNull(attributeValue, "Attribute value must not be null");
		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		request.setAttribute(storeAttributeName, attributeValue, WebRequest.SCOPE_SESSION);
	}

	@Override
	public Object retrieveAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		return request.getAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}

	@Override
	public void cleanupAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		String storeAttributeName = getAttributeNameInSession(request, attributeName);
		request.removeAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}


	/**
	 * 计算后端会话中的属性名称.
	 * <p>默认实现只是预先配置了{@link #setAttributeNamePrefix "attributeNamePrefix"}.
	 * 
	 * @param request 当前请求
	 * @param attributeName 属性名称
	 * 
	 * @return 后端会话中的属性名称
	 */
	protected String getAttributeNameInSession(WebRequest request, String attributeName) {
		return this.attributeNamePrefix + attributeName;
	}

}
