package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Helper类, 用于从MessageSource轻松访问消息, 提供各种重载的getMessage方法.
 *
 * <p>可从ApplicationObjectSupport获得, 但也可作为独立的帮助程序重新使用, 以委派给应用程序对象.
 */
public class MessageSourceAccessor {

	private final MessageSource messageSource;

	private final Locale defaultLocale;


	/**
	 * 使用LocaleContextHolder的区域设置作为默认区域设置.
	 * 
	 * @param messageSource 要包装的MessageSource
	 */
	public MessageSourceAccessor(MessageSource messageSource) {
		this.messageSource = messageSource;
		this.defaultLocale = null;
	}

	/**
	 * 使用给定的默认区域设置.
	 * 
	 * @param messageSource 要包装的MessageSource
	 * @param defaultLocale 用于消息访问的默认区域设置
	 */
	public MessageSourceAccessor(MessageSource messageSource, Locale defaultLocale) {
		this.messageSource = messageSource;
		this.defaultLocale = defaultLocale;
	}


	/**
	 * 如果未给出明确的区域设置, 则返回要使用的默认区域设置.
	 * <p>默认实现返回传递给相应构造函数的默认区域设置, 或者LocaleContextHolder的区域设置作为回退.
	 * 可以在子类中重写.
	 */
	protected Locale getDefaultLocale() {
		return (this.defaultLocale != null ? this.defaultLocale : LocaleContextHolder.getLocale());
	}

	/**
	 * 检索给定代码和默认区域设置的消息.
	 * 
	 * @param code 消息代码
	 * @param defaultMessage 如果查找失败, 要返回的字符串
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage) {
		return this.messageSource.getMessage(code, null, defaultMessage, getDefaultLocale());
	}

	/**
	 * 检索给定代码和给定Locale的消息.
	 * 
	 * @param code 消息代码
	 * @param defaultMessage 如果查找失败, 要返回的字符串
	 * @param locale 要在其中进行查找的区域设置
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage, Locale locale) {
		return this.messageSource.getMessage(code, null, defaultMessage, locale);
	}

	/**
	 * 检索给定代码和默认区域设置的消息.
	 * 
	 * @param code 消息代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 要返回的字符串
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, Object[] args, String defaultMessage) {
		return this.messageSource.getMessage(code, args, defaultMessage, getDefaultLocale());
	}

	/**
	 * 检索给定代码和给定Locale的消息.
	 * 
	 * @param code 消息代码
	 * @param args 消息的参数, 或{@code null}
	 * @param defaultMessage 如果查找失败, 要返回的字符串
	 * @param locale 要在其中进行查找的区域设置
	 * 
	 * @return 消息
	 */
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return this.messageSource.getMessage(code, args, defaultMessage, locale);
	}

	/**
	 * 检索给定代码和默认Locale的消息.
	 * 
	 * @param 消息代码
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, getDefaultLocale());
	}

	/**
	 * 检索给定代码和给定Locale的消息.
	 * 
	 * @param code 消息代码
	 * @param locale 要在其中进行查找的区域设置
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, locale);
	}

	/**
	 * 检索给定代码和默认Locale的消息.
	 * 
	 * @param code 消息代码
	 * @param args 消息的参数, 或{@code null}
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Object[] args) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, getDefaultLocale());
	}

	/**
	 * 检索给定代码和给定Locale的消息.
	 * 
	 * @param code 消息代码
	 * @param args 消息的参数, 或{@code null}
	 * @param locale 要在其中进行查找的区域设置
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	/**
	 * 在默认的Locale中检索给定的MessageSourceResolvable (e.g. ObjectError 实例).
	 * 
	 * @param resolvable the MessageSourceResolvable
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, getDefaultLocale());
	}

	/**
	 * 在给定的Locale中检索给定的 MessageSourceResolvable (e.g. ObjectError 实例).
	 * 
	 * @param resolvable the MessageSourceResolvable
	 * @param locale 要在其中进行查找的区域设置
	 * 
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}

}
