package org.springframework.jdbc.datasource.init;

/**
 * 当无法确定比"处理SQL脚本时出现问题"更具体的内容时抛出:
 * 例如, 来自JDBC的{@link java.sql.SQLException}, 无法更准确地指出.
 */
@SuppressWarnings("serial")
public class UncategorizedScriptException extends ScriptException {

	public UncategorizedScriptException(String message) {
		super(message);
	}

	public UncategorizedScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
