package org.springframework.scripting.support;

import javax.script.ScriptException;

/**
 * 装饰来自JSR-223脚本评估的{@link javax.script.ScriptException}的异常,
 * i.e. {@link javax.script.ScriptEngine#eval}调用
 * 或{@link javax.script.Invocable#invokeMethod} / {@link javax.script.Invocable#invokeFunction}调用.
 *
 * <p>此异常不会打印Java堆栈跟踪, 因为JSR-223 {@link ScriptException}会导致相当复杂的文本输出.
 * 从这个角度来看, 这个异常主要是传递给外部异常的{@link ScriptException}根本原因的装饰器.
 */
@SuppressWarnings("serial")
public class StandardScriptEvalException extends RuntimeException {

	private final ScriptException scriptException;


	public StandardScriptEvalException(ScriptException ex) {
		super(ex.getMessage());
		this.scriptException = ex;
	}


	public final ScriptException getScriptException() {
		return this.scriptException;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
