package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * 简单实现{@link org.springframework.context.MessageSource}, 允许以编程方式注册消息.
 * 支持基本国际化的MessageSource.
 *
 * <p>用于测试而不是用于生产系统.
 */
public class StaticMessageSource extends AbstractMessageSource {

	/** Map from 'code + locale' keys to message Strings */
	private final Map<String, String> messages = new HashMap<String, String>();

	private final Map<String, MessageFormat> cachedMessageFormats = new HashMap<String, MessageFormat>();


	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		return this.messages.get(code + '_' + locale.toString());
	}

	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		String key = code + '_' + locale.toString();
		String msg = this.messages.get(key);
		if (msg == null) {
			return null;
		}
		synchronized (this.cachedMessageFormats) {
			MessageFormat messageFormat = this.cachedMessageFormats.get(key);
			if (messageFormat == null) {
				messageFormat = createMessageFormat(msg, locale);
				this.cachedMessageFormats.put(key, messageFormat);
			}
			return messageFormat;
		}
	}

	/**
	 * 将给定消息与给定代码相关联.
	 * 
	 * @param code 要查找的代码
	 * @param locale 应该在其中找到消息的区域设置
	 * @param msg 与此查找代码关联的消息
	 */
	public void addMessage(String code, Locale locale, String msg) {
		Assert.notNull(code, "Code must not be null");
		Assert.notNull(locale, "Locale must not be null");
		Assert.notNull(msg, "Message must not be null");
		this.messages.put(code + '_' + locale.toString(), msg);
		if (logger.isDebugEnabled()) {
			logger.debug("Added message [" + msg + "] for code [" + code + "] and Locale [" + locale + "]");
		}
	}

	/**
	 * 关联给定的消息值, 给定的Key作为代码.
	 * 
	 * @param messages 要注册的消息, 消息代码为Key, 消息文本为值
	 * @param locale 应该在其中查找消息的区域设置
	 */
	public void addMessages(Map<String, String> messages, Locale locale) {
		Assert.notNull(messages, "Messages Map must not be null");
		for (Map.Entry<String, String> entry : messages.entrySet()) {
			addMessage(entry.getKey(), locale, entry.getValue());
		}
	}


	@Override
	public String toString() {
		return getClass().getName() + ": " + this.messages;
	}

}
