package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * 表示分配. 为表达式调用 setValue() 的替代方法是使用 assign.
 *
 * <p>Example: 'someNumberProperty=42'
 */
public class Assign extends SpelNodeImpl {

	public Assign(int pos,SpelNodeImpl... operands) {
		super(pos,operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue newValue = this.children[1].getValueInternal(state);
		getChild(0).setValue(state, newValue.getValue());
		return newValue;
	}

	@Override
	public String toStringAST() {
		return getChild(0).toStringAST() + "=" + getChild(1).toStringAST();
	}

}
