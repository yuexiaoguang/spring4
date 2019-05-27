package org.springframework.beans.factory.xml;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * 包含{@link org.xml.sax.SAXException}的特定于XML的BeanDefinitionStoreException子类,
 * 通常是{@link org.xml.sax.SAXParseException}, 其中包含有关错误位置的信息.
 */
@SuppressWarnings("serial")
public class XmlBeanDefinitionStoreException extends BeanDefinitionStoreException {

	/**
	 * @param resourceDescription bean定义来自的资源的描述
	 * @param msg 详细信息 (用作异常消息的原样)
	 * @param cause the SAXException (typically a SAXParseException) root cause
	 */
	public XmlBeanDefinitionStoreException(String resourceDescription, String msg, SAXException cause) {
		super(resourceDescription, msg, cause);
	}

	/**
	 * 返回失败的XML资源中的行号.
	 * 
	 * @return 行号 (in case of a SAXParseException); -1 else
	 */
	public int getLineNumber() {
		Throwable cause = getCause();
		if (cause instanceof SAXParseException) {
			return ((SAXParseException) cause).getLineNumber();
		}
		return -1;
	}
}
