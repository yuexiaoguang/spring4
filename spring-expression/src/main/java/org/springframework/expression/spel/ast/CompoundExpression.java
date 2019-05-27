package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;

/**
 * 表示DOT分隔的表达式序列, 例如'property1.property2.methodOne()'
 */
public class CompoundExpression extends SpelNodeImpl {

	public CompoundExpression(int pos, SpelNodeImpl... expressionComponents) {
		super(pos, expressionComponents);
		if (expressionComponents.length < 2) {
			throw new IllegalStateException("Do not build compound expressions with less than two entries: " +
					expressionComponents.length);
		}
	}


	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		if (getChildCount() == 1) {
			return this.children[0].getValueRef(state);
		}

		SpelNodeImpl nextNode = this.children[0];
		try {
			TypedValue result = nextNode.getValueInternal(state);
			int cc = getChildCount();
			for (int i = 1; i < cc - 1; i++) {
				try {
					state.pushActiveContextObject(result);
					nextNode = this.children[i];
					result = nextNode.getValueInternal(state);
				}
				finally {
					state.popActiveContextObject();
				}
			}
			try {
				state.pushActiveContextObject(result);
				nextNode = this.children[cc - 1];
				return nextNode.getValueRef(state);
			}
			finally {
				state.popActiveContextObject();
			}
		}
		catch (SpelEvaluationException ex) {
			// 在重新抛出之前纠正错误的位置
			ex.setPosition(nextNode.getStartPosition());
			throw ex;
		}
	}

	/**
	 * 评估复合表达式.
	 * 这涉及依次评估每个部分, 并且每个部分的返回值是后续部分的活动上下文对象.
	 * 
	 * @param state 正在评估表达式的状态
	 * 
	 * @return 最后一段复合表达式的最终值
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		ValueRef ref = getValueRef(state);
		TypedValue result = ref.getValue();
		this.exitTypeDescriptor = this.children[this.children.length - 1].exitTypeDescriptor;
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws EvaluationException {
		getValueRef(state).setValue(value);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		return getValueRef(state).isWritable();
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(".");
			}
			sb.append(getChild(i).toStringAST());
		}
		return sb.toString();
	}
	
	@Override
	public boolean isCompilable() {
		for (SpelNodeImpl child: this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		for (int i = 0; i < this.children.length;i++) {
			this.children[i].generateCode(mv, cf);
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
