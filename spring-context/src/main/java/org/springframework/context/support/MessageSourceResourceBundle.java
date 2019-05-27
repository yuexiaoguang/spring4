package org.springframework.context.support;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.Assert;

/**
 * 允许以{@link java.util.ResourceBundle}访问Spring {@link org.springframework.context.MessageSource}的Helper类.
 * 例如, 用于将Spring MessageSource公开给JSTL Web视图.
 */
public class MessageSourceResourceBundle extends ResourceBundle {

	private final MessageSource messageSource;

	private final Locale locale;


	/**
	 * @param source 要从中检索消息的MessageSource
	 * @param locale 要检索消息的区域设置
	 */
	public MessageSourceResourceBundle(MessageSource source, Locale locale) {
		Assert.notNull(source, "MessageSource must not be null");
		this.messageSource = source;
		this.locale = locale;
	}

	/**
	 * @param source 要从中检索消息的MessageSource
	 * @param locale 要检索消息的区域设置
	 * @param parent 如果没有找到本地消息, 则委托给父级ResourceBundle
	 */
	public MessageSourceResourceBundle(MessageSource source, Locale locale, ResourceBundle parent) {
		this(source, locale);
		setParent(parent);
	}


	/**
	 * 此实现解析MessageSource中的代码.
	 * 如果无法解析消息, 则返回{@code null}.
	 */
	@Override
	protected Object handleGetObject(String key) {
		try {
			return this.messageSource.getMessage(key, null, this.locale);
		}
		catch (NoSuchMessageException ex) {
			return null;
		}
	}

	/**
	 * 此实现检查目标MessageSource是否可以解析给定Key的消息, 相应地转换 {@code NoSuchMessageException}.
	 * 与ResourceBundle在JDK 1.6中的默认实现相反, 这不依赖于枚举消息Key的能力.
	 */
	@Override
	public boolean containsKey(String key) {
		try {
			this.messageSource.getMessage(key, null, this.locale);
			return true;
		}
		catch (NoSuchMessageException ex) {
			return false;
		}
	}

	/**
	 * 此实现抛出{@code UnsupportedOperationException}, 因为MessageSource不允许枚举定义的消息代码.
	 */
	@Override
	public Enumeration<String> getKeys() {
		throw new UnsupportedOperationException("MessageSourceResourceBundle does not support enumerating its keys");
	}

	/**
	 * 此实现通过标准 {@code ResourceBundle.getLocale()}方法公开指定的Locale以进行内省.
	 */
	@Override
	public Locale getLocale() {
		return this.locale;
	}

}
