package org.springframework.scripting;

import org.springframework.core.NestedRuntimeException;

/**
 * 脚本编译失败抛出的异常.
 */
@SuppressWarnings("serial")
public class ScriptCompilationException extends NestedRuntimeException {

	private ScriptSource scriptSource;


	public ScriptCompilationException(String msg) {
		super(msg);
	}

	/**
	 * @param msg the detail message
	 * @param cause the root cause (通常来自使用底层脚本编译器API)
	 */
	public ScriptCompilationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param scriptSource 违规脚本的源
	 * @param msg the detail message
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg) {
		super("Could not compile " + scriptSource + ": " + msg);
		this.scriptSource = scriptSource;
	}

	/**
	 * @param scriptSource 违规脚本的源
	 * @param cause the root cause (通常来自使用底层脚本编译器API)
	 */
	public ScriptCompilationException(ScriptSource scriptSource, Throwable cause) {
		super("Could not compile " + scriptSource, cause);
		this.scriptSource = scriptSource;
	}

	/**
	 * @param scriptSource 违规脚本的源
	 * @param msg the detail message
	 * @param cause the root cause (通常来自使用底层脚本编译器API)
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg, Throwable cause) {
		super("Could not compile " + scriptSource + ": " + msg, cause);
		this.scriptSource = scriptSource;
	}


	/**
	 * 返回违规脚本的源.
	 * 
	 * @return 源, 或{@code null}
	 */
	public ScriptSource getScriptSource() {
		return this.scriptSource;
	}

}
