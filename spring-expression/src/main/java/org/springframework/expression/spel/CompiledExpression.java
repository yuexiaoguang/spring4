package org.springframework.expression.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;

/**
 * 编译表达式的基础超类.
 * 每个生成的编译表达式类都将扩展此类并实现{@link #getValue}方法.
 * 它不打算由用户代码子类化.
 */
public abstract class CompiledExpression {

	/**
	 * SpelCompiler生成的CompiledExpression子类将提供此方法的实现.
	 */
	public abstract Object getValue(Object target, EvaluationContext context) throws EvaluationException;

}
