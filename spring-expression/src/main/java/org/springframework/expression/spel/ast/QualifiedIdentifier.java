package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

/**
 * 表示以点分隔的字符串序列, 表示包限定类型引用.
 *
 * <p>Example: "java.lang.String" 在表达式中"new java.lang.String('hello')"
 */
public class QualifiedIdentifier extends SpelNodeImpl {

	// TODO safe to cache? dont think so
	private TypedValue value;


	public QualifiedIdentifier(int pos, SpelNodeImpl... operands) {
		super(pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		// 缓存子标识符的串联
		if (this.value == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < getChildCount(); i++) {
				Object value = this.children[i].getValueInternal(state).getValue();
				if (i > 0 && !value.toString().startsWith("$")) {
					sb.append(".");
				}
				sb.append(value);
			}
			this.value = new TypedValue(sb.toString());
		}
		return this.value;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		if (this.value != null) {
			sb.append(this.value.getValue());
		}
		else {
			for (int i = 0; i < getChildCount(); i++) {
				if (i > 0) {
					sb.append(".");
				}
				sb.append(getChild(i).toStringAST());
			}
		}
		return sb.toString();
	}

}
