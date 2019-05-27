package org.springframework.beans.factory.parsing;

import org.springframework.util.Assert;

/**
 * 表示bean定义配置的问题.
 * 主要作为传递给{@link ProblemReporter}的共同论点.
 *
 * <p>表示可能致命的问题(错误)或只是警告.
 */
public class Problem {

	private final String message;

	private final Location location;

	private final ParseState parseState;

	private final Throwable rootCause;


	/**
	 * @param message 详细说明问题的信息
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location) {
		this(message, location, null, null);
	}

	/**
	 * @param message 详细说明问题的信息
	 * @param parseState 错误发生时的{@link ParseState}
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location, ParseState parseState) {
		this(message, location, parseState, null);
	}

	/**
	 * @param message 详细说明问题的信息
	 * @param rootCause 导致错误的基础异常 (may be {@code null})
	 * @param parseState 错误发生时的{@link ParseState}
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location, ParseState parseState, Throwable rootCause) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(location, "Location must not be null");
		this.message = message;
		this.location = location;
		this.parseState = parseState;
		this.rootCause = rootCause;
	}


	/**
	 * 获取详细说明问题的消息.
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * 获取触发错误的bean配置源中的位置.
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * 获取此Problem的Location对象中包含的触发错误的bean配置源的描述.
	 */
	public String getResourceDescription() {
		return getLocation().getResource().getDescription();
	}

	/**
	 * 在出错时获取{@link ParseState} (may be {@code null}).
	 */
	public ParseState getParseState() {
		return this.parseState;
	}

	/**
	 * 获取导致错误的基础异常 (may be {@code null}).
	 */
	public Throwable getRootCause() {
		return this.rootCause;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Configuration problem: ");
		sb.append(getMessage());
		sb.append("\nOffending resource: ").append(getResourceDescription());
		if (getParseState() != null) {
			sb.append('\n').append(getParseState());
		}
		return sb.toString();
	}

}
