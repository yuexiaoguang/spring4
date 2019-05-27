package org.springframework.context;

import java.util.Locale;

/**
 * 无法解析消息时抛出异常.
 */
@SuppressWarnings("serial")
public class NoSuchMessageException extends RuntimeException {

	/**
	 * @param code 对于给定的区域设置无法解析的代码
	 * @param locale 用于搜索其中代码的区域
	 */
	public NoSuchMessageException(String code, Locale locale) {
		super("No message found under code '" + code + "' for locale '" + locale + "'.");
	}

	/**
	 * @param code 对于给定的区域设置无法解析的代码
	 */
	public NoSuchMessageException(String code) {
		super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
	}

}

