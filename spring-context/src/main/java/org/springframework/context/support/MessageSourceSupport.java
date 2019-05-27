package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ObjectUtils;

/**
 * 消息源实现的基类, 提供支持基础结构, 例如 {@link java.text.MessageFormat}处理,
 * 但不实现{@link org.springframework.context.MessageSource}中定义的具体方法.
 *
 * <p>{@link AbstractMessageSource} 派生自此类, 提供具体的{@code getMessage}实现,
 * 委托给用于消息代码解析的中央模板方法.
 */
public abstract class MessageSourceSupport {

	private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean alwaysUseMessageFormat = false;

	/**
	 * 保存每条消息已生成的MessageFormat的缓存.
	 * 用于传入的默认消息. 已解析的代码的MessageFormat在子类中以特定方式缓存.
	 */
	private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage =
			new HashMap<String, Map<Locale, MessageFormat>>();


	/**
	 * 设置是否始终应用{@code MessageFormat}规则, 甚至解析不带参数的消息.
	 * <p>默认 "false": 默认情况下, 不带参数的消息按原样返回, 而不通过MessageFormat解析它们.
	 * 设置为"true" 以对所有消息强制执行MessageFormat, 期望使用MessageFormat转义来编写所有消息文本.
	 * <p>例如, MessageFormat期望将单引号转义为 "''".
	 * 如果你的消息文本都是用这样的转义写的, 即使没有定义参数占位符, 也需要将这个标志设置为 "true".
	 * 否则, 只有具有实际参数的消息文本才应该使用MessageFormat转义来编写.
	 */
	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	/**
	 * 返回是否始终应用MessageFormat规则, 甚至解析不带参数的消息.
	 */
	protected boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}


	/**
	 * 呈现给定的默认消息.
	 * 默认消息按调用者指定的方式传入, 并可呈现为向用户显示的完全格式化的默认消息.
	 * <p>默认实现将String传递给 {@code formatMessage}, 解析在其中找到的任何参数占位符.
	 * 子类可以重写此方法, 以插入默认消息的自定义处理.
	 * 
	 * @param defaultMessage 传入的默认消息
	 * @param args 将填充消息中的params的参数数组; 如果没有, 则为{@code null}.
	 * @param locale 用于格式化的Locale
	 * 
	 * @return 呈现的默认消息 (带有已解析的参数)
	 */
	protected String renderDefaultMessage(String defaultMessage, Object[] args, Locale locale) {
		return formatMessage(defaultMessage, args, locale);
	}

	/**
	 * 使用缓存的MessageFormat格式化给定的消息.
	 * 默认情况下, 为传入的默认消息调用, 以解析在其中找到的任何参数占位符.
	 * 
	 * @param msg 要格式化的消息
	 * @param args 将填充消息中的params的参数数组; 如果没有, 则为{@code null}.
	 * @param locale 用于格式化的Locale
	 * 
	 * @return 已格式化的消息 (带有已解析的参数)
	 */
	protected String formatMessage(String msg, Object[] args, Locale locale) {
		if (msg == null || (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args))) {
			return msg;
		}
		MessageFormat messageFormat = null;
		synchronized (this.messageFormatsPerMessage) {
			Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
			if (messageFormatsPerLocale != null) {
				messageFormat = messageFormatsPerLocale.get(locale);
			}
			else {
				messageFormatsPerLocale = new HashMap<Locale, MessageFormat>();
				this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
			}
			if (messageFormat == null) {
				try {
					messageFormat = createMessageFormat(msg, locale);
				}
				catch (IllegalArgumentException ex) {
					// 消息格式无效 - 可能不是用于格式化, 而是使用不涉及参数的消息结构...
					if (isAlwaysUseMessageFormat()) {
						throw ex;
					}
					// 如果格式未强制, 请静默处理原始消息...
					messageFormat = INVALID_MESSAGE_FORMAT;
				}
				messageFormatsPerLocale.put(locale, messageFormat);
			}
		}
		if (messageFormat == INVALID_MESSAGE_FORMAT) {
			return msg;
		}
		synchronized (messageFormat) {
			return messageFormat.format(resolveArguments(args, locale));
		}
	}

	/**
	 * 为给定的消息和Locale创建MessageFormat.
	 * 
	 * @param msg 用于创建MessageFormat的消息
	 * @param locale 用于创建MessageFormat的Locale
	 * 
	 * @return MessageFormat实例
	 */
	protected MessageFormat createMessageFormat(String msg, Locale locale) {
		return new MessageFormat((msg != null ? msg : ""), locale);
	}

	/**
	 * 用于解析参数对象的模板方法.
	 * <p>默认实现只是按原样返回给定的参数数组.
	 * 可以在子类中重写以解析特殊参数类型.
	 * 
	 * @param args 原始的参数数组
	 * @param locale 要解析的区域设置
	 * 
	 * @return 已解析的参数数组
	 */
	protected Object[] resolveArguments(Object[] args, Locale locale) {
		return args;
	}

}
