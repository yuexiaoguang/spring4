package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.ObjectUtils;

/**
 * {@link HierarchicalMessageSource}接口的抽象实现, 实现消息变体的常见处理, 使得为具体的MessageSource实现特定策略变得容易.
 *
 * <p>子类必须实现抽象的{@link #resolveCode}方法.
 * 为了有效地解析没有参数的消息, 还应该重写{@link #resolveCodeWithoutArguments}方法, 解析消息而不涉及MessageFormat.
 *
 * <p><b>Note:</b> 默认情况下, 如果已为消息传入参数, 则仅通过MessageFormat解析消息文本.
 * 如果没有参数, 将按原样返回消息文本.
 * 因此, 您应该仅对具有实际参数的消息使用MessageFormat转义, 并保持所有其他消息不转义.
 * 如果您希望转义所有消息, 请将 "alwaysUseMessageFormat"标志设置为 "true".
 *
 * <p>不仅支持 MessageSourceResolvables 作为主要消息, 还支持解析 MessageSourceResolvables本身的消息参数.
 *
 * <p>此类不实现每个代码的消息缓存, 因此子类可以随时间动态更改消息.
 * 鼓励子类以修改感知的方式缓存其消息, 允许已更新的消息的热部署.
 */
public abstract class AbstractMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

	private MessageSource parentMessageSource;

	private Properties commonMessages;

	private boolean useCodeAsDefaultMessage = false;


	@Override
	public void setParentMessageSource(MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}

	/**
	 * 指定与区域设置无关的公共消息, 消息代码为Key, 完整消息String (可能包含参数占位符) 作为值.
	 * <p>也可以链接到外部定义的Properties对象,
	 * e.g. 通过{@link org.springframework.beans.factory.config.PropertiesFactoryBean}定义.
	 */
	public void setCommonMessages(Properties commonMessages) {
		this.commonMessages = commonMessages;
	}

	/**
	 * 返回定义与区域设置无关的公共消息的Properties对象.
	 */
	protected Properties getCommonMessages() {
		return this.commonMessages;
	}

	/**
	 * 设置是否将消息代码用作默认消息, 而不是抛出NoSuchMessageException. 对开发和调试很有用.
	 * 默认 "false".
	 * <p>Note: 如果MessageSourceResolvable具有多个代码 (如 FieldError) 和MessageSource具有父MessageSource,
	 * 不要激活<i>父级</i>中的 "useCodeAsDefaultMessage" :
	 * 否则, 您将获得父级返回的第一个代码, 而不会尝试检查更多代码.
	 * <p>为了能够在父级中启用 "useCodeAsDefaultMessage", 
	 * AbstractMessageSource 和 AbstractApplicationContext包含特殊检查以委托给内部 {@link #getMessageInternal}方法.
	 * 一般来说, 建议在开发过程中只使用 "useCodeAsDefaultMessage", 而不是首先在生产中依赖它.
	 */
	public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}

	/**
	 * 返回是否使用消息代码作为默认消息, 而不是抛出NoSuchMessageException.
	 * 对开发和调试很有用.
	 * 默认 "false".
	 * <p>或者, 考虑重写 {@link #getDefaultMessage} 方法以返回无法解析的代码的自定义回退消息.
	 */
	protected boolean isUseCodeAsDefaultMessage() {
		return this.useCodeAsDefaultMessage;
	}


	@Override
	public final String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		String msg = getMessageInternal(code, args, locale);
		if (msg != null) {
			return msg;
		}
		if (defaultMessage == null) {
			String fallback = getDefaultMessage(code);
			if (fallback != null) {
				return fallback;
			}
		}
		return renderDefaultMessage(defaultMessage, args, locale);
	}

	@Override
	public final String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		String msg = getMessageInternal(code, args, locale);
		if (msg != null) {
			return msg;
		}
		String fallback = getDefaultMessage(code);
		if (fallback != null) {
			return fallback;
		}
		throw new NoSuchMessageException(code, locale);
	}

	@Override
	public final String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		String[] codes = resolvable.getCodes();
		if (codes != null) {
			for (String code : codes) {
				String message = getMessageInternal(code, resolvable.getArguments(), locale);
				if (message != null) {
					return message;
				}
			}
		}
		String defaultMessage = getDefaultMessage(resolvable, locale);
		if (defaultMessage != null) {
			return defaultMessage;
		}
		throw new NoSuchMessageException(!ObjectUtils.isEmpty(codes) ? codes[codes.length - 1] : null, locale);
	}


	/**
	 * 将给定的代码和参数解析为给定Locale中的消息, 如果未找到则返回{@code null}.
	 * 不会作为默认消息回退到代码. 由{@code getMessage}方法调用.
	 * 
	 * @param code 要查找的代码, 例如 'calculator.noRateSet'
	 * @param args 将填充消息中的参数的参数数组
	 * @param locale 要在其中执行查找的区域设置
	 * 
	 * @return 已解析的消息; 如果未找到, 则为{@code null}
	 */
	protected String getMessageInternal(String code, Object[] args, Locale locale) {
		if (code == null) {
			return null;
		}
		if (locale == null) {
			locale = Locale.getDefault();
		}
		Object[] argsToUse = args;

		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			// 优化的解析: 没有要应用的参数, 因此不需要涉及MessageFormat.
			// 请注意, 默认实现仍使用MessageFormat; 可以在特定的子类中重写.
			String message = resolveCodeWithoutArguments(code, locale);
			if (message != null) {
				return message;
			}
		}

		else {
			// 对于在父级MessageSource中定义消息, 但在子级MessageSource中定义可解析参数的情况, 实时地解析参数.
			argsToUse = resolveArguments(args, locale);

			MessageFormat messageFormat = resolveCode(code, locale);
			if (messageFormat != null) {
				synchronized (messageFormat) {
					return messageFormat.format(argsToUse);
				}
			}
		}

		// 检查与区域设置无关的给定消息代码的公共消息.
		Properties commonMessages = getCommonMessages();
		if (commonMessages != null) {
			String commonMessage = commonMessages.getProperty(code);
			if (commonMessage != null) {
				return formatMessage(commonMessage, args, locale);
			}
		}

		// Not found -> check parent, if any.
		return getMessageFromParent(code, argsToUse, locale);
	}

	/**
	 * 尝试从父级{@code MessageSource}检索给定的消息.
	 * 
	 * @param code 要查找的代码, 例如 'calculator.noRateSet'
	 * @param args 将填充消息中的参数的参数数组
	 * @param locale 要在其中执行查找的区域设置
	 * 
	 * @return 已解析的消息; 如果未找到, 则为{@code null}
	 */
	protected String getMessageFromParent(String code, Object[] args, Locale locale) {
		MessageSource parent = getParentMessageSource();
		if (parent != null) {
			if (parent instanceof AbstractMessageSource) {
				// 调用内部方法以避免在激活 "useCodeAsDefaultMessage" 的情况下, 返回默认代码.
				return ((AbstractMessageSource) parent).getMessageInternal(code, args, locale);
			}
			else {
				// 检查父级MessageSource, 如果未找到返回 null.
				return parent.getMessage(code, args, null, locale);
			}
		}
		// 在父级中也找不到.
		return null;
	}

	/**
	 * 获取给定{@code MessageSourceResolvable}的默认消息.
	 * <p>此实现完全呈现默认消息, 或者仅在主要消息代码用作默认消息时, 返回普通默认消息{@code String}.
	 * 
	 * @param resolvable 要为其解析默认消息的值对象
	 * @param locale 当前区域设置
	 * 
	 * @return 默认消息, 或{@code null}
	 */
	protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
		String defaultMessage = resolvable.getDefaultMessage();
		String[] codes = resolvable.getCodes();
		if (defaultMessage != null) {
			if (!ObjectUtils.isEmpty(codes) && defaultMessage.equals(codes[0])) {
				// 永远不要格式化 code-as-default-message, 即使是 alwaysUseMessageFormat=true
				return defaultMessage;
			}
			return renderDefaultMessage(defaultMessage, resolvable.getArguments(), locale);
		}
		return (!ObjectUtils.isEmpty(codes) ? getDefaultMessage(codes[0]) : null);
	}

	/**
	 * 返回给定代码的回退默认消息.
	 * <p>如果激活 "useCodeAsDefaultMessage", 则默认返回代码本身, 否则不返回回退.
	 * 如果没有回退, 调用者通常会从{@code getMessage}收到{@code NoSuchMessageException}.
	 * 
	 * @param code 无法解决并且没有收到明确的默认消息的消息代码
	 * 
	 * @return 要使用的默认消息, 或{@code null}
	 */
	protected String getDefaultMessage(String code) {
		if (isUseCodeAsDefaultMessage()) {
			return code;
		}
		return null;
	}


	/**
	 * 搜索给定的对象数组, 查找MessageSourceResolvable对象并解析它们.
	 * <p>允许消息将MessageSourceResolvable作为参数.
	 * 
	 * @param args 消息的参数数组
	 * @param locale 要解析的区域设置
	 * 
	 * @return 已解析的MessageSourceResolvable的参数数组
	 */
	@Override
	protected Object[] resolveArguments(Object[] args, Locale locale) {
		if (args == null) {
			return new Object[0];
		}
		List<Object> resolvedArgs = new ArrayList<Object>(args.length);
		for (Object arg : args) {
			if (arg instanceof MessageSourceResolvable) {
				resolvedArgs.add(getMessage((MessageSourceResolvable) arg, locale));
			}
			else {
				resolvedArgs.add(arg);
			}
		}
		return resolvedArgs.toArray();
	}

	/**
	 * 子类可以重写此方法, 以优化的方式解析没有参数的消息, i.e. 解析而不涉及MessageFormat.
	 * <p>默认实现使用 MessageFormat, 通过委托给 {@link #resolveCode} 方法.
	 * 鼓励子类用优化的解析替换它.
	 * <p>不幸的是, {@code java.text.MessageFormat} 没有以有效的方式实现.
	 * 特别是, 它不会检测消息模式首先不包含参数占位符.
	 * 因此, 建议对没有参数的消息规避MessageFormat.
	 * 
	 * @param code 要解析的消息的代码
	 * @param locale 解析代码的区域设置 (鼓励子类支持国际化)
	 * 
	 * @return 消息String, 或{@code null}如果未找到
	 */
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		MessageFormat messageFormat = resolveCode(code, locale);
		if (messageFormat != null) {
			synchronized (messageFormat) {
				return messageFormat.format(new Object[0]);
			}
		}
		return null;
	}

	/**
	 * 子类必须实现此方法来解析消息.
	 * <p>返回MessageFormat实例而不是消息String, 以允许在子类中适当缓存MessageFormats.
	 * <p><b>鼓励子类为没有参数的消息提供优化的解析, 不涉及MessageFormat.</b>
	 * See the {@link #resolveCodeWithoutArguments} javadoc for details.
	 * 
	 * @param code 要解析的消息的代码
	 * @param locale 解析代码的区域设置 (鼓励子类支持国际化)
	 * 
	 * @return 消息的MessageFormat, 或{@code null}如果未找到
	 */
	protected abstract MessageFormat resolveCode(String code, Locale locale);

}
