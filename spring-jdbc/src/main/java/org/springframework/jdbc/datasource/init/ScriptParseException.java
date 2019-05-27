package org.springframework.jdbc.datasource.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * 如果无法正确解析SQL脚本, 则由{@link ScriptUtils}抛出.
 */
@SuppressWarnings("serial")
public class ScriptParseException extends ScriptException {

	/**
	 * @param message 详细信息
	 * @param resource 从中读取SQL脚本的资源
	 */
	public ScriptParseException(String message, EncodedResource resource) {
		super(buildMessage(message, resource));
	}

	/**
	 * @param message 详细信息
	 * @param resource 从中读取SQL脚本的资源
	 * @param cause 失败的底层异常
	 */
	public ScriptParseException(String message, EncodedResource resource, Throwable cause) {
		super(buildMessage(message, resource), cause);
	}

	private static String buildMessage(String message, EncodedResource resource) {
		return String.format("Failed to parse SQL script from resource [%s]: %s", (resource == null ? "<unknown>"
				: resource), message);
	}

}
