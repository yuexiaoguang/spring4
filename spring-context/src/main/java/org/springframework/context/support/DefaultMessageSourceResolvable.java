package org.springframework.context.support;

import java.io.Serializable;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MessageSourceResolvable}接口的默认实现.
 * 提供一种简单的方法, 来存储通过{@link org.springframework.context.MessageSource}解析消息所需的所有必要值.
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

	private final String[] codes;

	private final Object[] arguments;

	private final String defaultMessage;


	/**
	 * @param code 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String code) {
		this(new String[] {code}, null, null);
	}

	/**
	 * @param codes 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String[] codes) {
		this(codes, null, null);
	}

	/**
	 * @param codes 用于解析此消息的代码
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
		this(codes, null, defaultMessage);
	}

	/**
	 * @param codes 用于解析此消息的代码
	 * @param arguments 用于解析此消息的参数数组
	 */
	public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
		this(codes, arguments, null);
	}

	/**
	 * @param codes 用于解析此消息的代码
	 * @param arguments 用于解析此消息的参数数组
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(String[] codes, Object[] arguments, String defaultMessage) {
		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * 克隆: 从另一个已解析的实例创建一个新实例.
	 * 
	 * @param resolvable 要复制的实例
	 */
	public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
		this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
	}


	/**
	 * 返回此实例的默认代码, 即代码数组中的最后一个.
	 */
	public String getCode() {
		return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
	}

	@Override
	public String[] getCodes() {
		return this.codes;
	}

	@Override
	public Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public String getDefaultMessage() {
		return this.defaultMessage;
	}


	/**
	 * 为此MessageSourceResolvable构建默认的String表示形式:
	 * 包括代码, 参数和默认消息.
	 */
	protected final String resolvableToString() {
		StringBuilder result = new StringBuilder();
		result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
		result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
		result.append("]; default message [").append(this.defaultMessage).append(']');
		return result.toString();
	}

	/**
	 * 默认实现暴露此MessageSourceResolvable的属性.
	 * 要在更具体的子类中重写, 可能包括通过 {@code resolvableToString()}的可解析内容.
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": " + resolvableToString();
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MessageSourceResolvable)) {
			return false;
		}
		MessageSourceResolvable otherResolvable = (MessageSourceResolvable) other;
		return (ObjectUtils.nullSafeEquals(getCodes(), otherResolvable.getCodes()) &&
				ObjectUtils.nullSafeEquals(getArguments(), otherResolvable.getArguments()) &&
				ObjectUtils.nullSafeEquals(getDefaultMessage(), otherResolvable.getDefaultMessage()));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getCodes());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArguments());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getDefaultMessage());
		return hashCode;
	}

}
