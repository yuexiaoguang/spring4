package org.springframework.cache.interceptor;

import org.springframework.expression.EvaluationException;

/**
 * 一个特定的{@link EvaluationException}, 提示表达式中使用的给定变量在上下文中不可用.
 */
@SuppressWarnings("serial")
class VariableNotAvailableException extends EvaluationException {

	private final String name;


	public VariableNotAvailableException(String name) {
		super("Variable not available");
		this.name = name;
	}


	public final String getName() {
		return this.name;
	}

}
